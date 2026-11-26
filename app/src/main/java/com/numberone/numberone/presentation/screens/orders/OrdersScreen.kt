package com.numberone.numberone.presentation.screens.orders

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.numberone.numberone.core.components.CopyField
import com.numberone.numberone.core.components.EmptyState
import com.numberone.numberone.core.components.ExpiryBadge
import com.numberone.numberone.core.components.FeedbackBanner
import com.numberone.numberone.core.components.NOCard
import com.numberone.numberone.core.components.NOScaffold
import com.numberone.numberone.core.components.NoticeBanner
import com.numberone.numberone.core.components.StatusChip
import com.numberone.numberone.core.components.dangerColor
import com.numberone.numberone.core.components.formatDateTime
import com.numberone.numberone.core.components.formatAmount
import com.numberone.numberone.core.components.ltr
import com.numberone.numberone.core.components.successColor
import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.core.localization.trf
import com.numberone.numberone.core.util.ReceiptFile
import com.numberone.numberone.core.util.formatFileSize
import com.numberone.numberone.core.util.readReceiptFile
import com.numberone.numberone.core.error.ErrorMapper
import kotlinx.coroutines.CancellationException
import com.numberone.numberone.domain.model.Order
import com.numberone.numberone.presentation.components.OrderProgress
import com.numberone.numberone.presentation.util.isWalletOrder
import com.numberone.numberone.presentation.util.stage
import com.numberone.numberone.presentation.util.waitingForCustomer
import com.numberone.numberone.presentation.viewmodel.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun OrdersScreen(vm: AppViewModel, onBack: () -> Unit) {
    LaunchedEffect(Unit) { vm.loadHistory() }
    var uploadFor by remember { mutableStateOf<Order?>(null) }

    NOScaffold(title = tr("سجل المشتريات"), onBack = onBack) {
        FeedbackBanner(loading = vm.loading, error = vm.error, message = vm.message)
        if (vm.history.isEmpty() && !vm.loading) EmptyState(tr("لا توجد مشتريات بعد"))
        vm.history.forEach { order ->
            val upload: (() -> Unit)? = if (order.waitingForCustomer()) ({ uploadFor = order }) else null
            OrderCard(order = order, onUploadReceipt = upload)
        }
    }

    uploadFor?.let { order ->
        UploadReceiptDialog(
            vm = vm,
            order = order,
            onDismiss = { uploadFor = null },
            onSubmitted = { uploadFor = null }
        )
    }
}

@Composable
private fun OrderCard(order: Order, onUploadReceipt: (() -> Unit)?) {
    NOCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    order.serviceName.ifBlank { order.countryName ?: tr("طلب شراء") },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    formatDateTime(order.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusChip(order.status)
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ltr("${formatAmount(order.price.toDouble())} USDT"), fontWeight = FontWeight.Bold)
            if (order.paymentStatus.isNotBlank()) StatusChip(order.paymentStatus)
        }

        if (order.isWalletOrder()) {
            Text(
                tr("دُفع من رصيد المحفظة"),
                style = MaterialTheme.typography.bodySmall,
                color = successColor(),
                fontWeight = FontWeight.SemiBold
            )
        } else {
            OrderProgress(stage = order.stage())
        }

        order.phoneNumber?.takeIf { it.isNotBlank() }?.let { CopyField(label = tr("رقم الهاتف"), value = it) }
        order.smsCode?.takeIf { it.isNotBlank() }?.let { CopyField(label = tr("رمز التحقق"), value = it) }
        order.expiresAt?.let { ExpiryBadge(expiry = it) }
        order.rejectionReason?.takeIf { it.isNotBlank() }?.let {
            NoticeBanner(text = trf("سبب الرفض: {0}", it), isError = true)
        }

        if (onUploadReceipt != null) {
            Button(
                onClick = onUploadReceipt,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(tr("رفع إثبات الدفع"), fontWeight = FontWeight.Bold)
            }
            Text(
                tr("أرسل صورة الإيصال ورقم المعاملة لبدء مراجعة الطلب."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Upload the receipt for an order that is waiting for one. Uses the existing receipt workflow via
 * [AppViewModel.submitReceiptForOrder]. Shows the order amount, requires a valid TXID, and accepts
 * only image files up to [MAX_RECEIPT_BYTES].
 */
@Composable
private fun UploadReceiptDialog(
    vm: AppViewModel,
    order: Order,
    onDismiss: () -> Unit,
    onSubmitted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ltrStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
    var txHash by remember(order.id) { mutableStateOf("") }
    var picked by remember(order.id) { mutableStateOf<ReceiptFile?>(null) }
    var reading by remember(order.id) { mutableStateOf(false) }
    var pickError by remember(order.id) { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            reading = true
            pickError = null
            scope.launch {
                try {
                    // Same reader as the store's receipt picker: validates the real file type and
                    // compresses oversized images to the 5 MB server limit.
                    picked = withContext(Dispatchers.IO) { readReceiptFile(context, uri) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    picked = null
                    pickError = ErrorMapper.toUserMessage(e)
                } finally {
                    reading = false
                }
            }
        }
    }

    val valid = vm.isValidTxHash(txHash)
    val current = picked
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("رفع إثبات الدفع"), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(order.serviceName.ifBlank { tr("طلب شراء") }, fontWeight = FontWeight.SemiBold)
                Text(
                    ltr("${formatAmount(order.price.toDouble())} USDT"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    tr("أرسل صورة إثبات الدفع ورقم المعاملة (TXID) لبدء مراجعة الطلب."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = txHash,
                    onValueChange = { txHash = it },
                    label = { Text(tr("رقم المعاملة (TXID)")) },
                    singleLine = true,
                    isError = txHash.isNotBlank() && !valid,
                    supportingText = { Text(tr("يتكوّن من 64 حرفاً ورقماً (مع أو بدون 0x)")) },
                    textStyle = ltrStyle,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    enabled = !reading && !vm.loading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(tr("اختيار صورة الإيصال"), style = MaterialTheme.typography.labelLarge)
                }
                when {
                    reading -> Text(
                        tr("جارٍ قراءة الصورة…"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    current != null -> Text(
                        trf("تم اختيار الصورة ({0})", formatFileSize(current.bytes.size)),
                        style = MaterialTheme.typography.bodySmall,
                        color = successColor(),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                pickError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = dangerColor()) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid && current != null && !reading && !vm.loading,
                onClick = {
                    val p = picked
                    if (p != null && valid) {
                        vm.submitReceiptForOrder(order.id, txHash.trim(), p.bytes, p.mimeType, p.extension)
                        onSubmitted()
                    }
                }
            ) { Text(tr("إرسال الإيصال"), color = successColor(), fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("إلغاء")) } }
    )
}
