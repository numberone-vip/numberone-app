package com.numberone.numberone.presentation.screens.store

import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.core.localization.trf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.numberone.numberone.core.components.CopyRow
import com.numberone.numberone.core.components.EmptyState
import com.numberone.numberone.core.components.ExpiryBadge
import com.numberone.numberone.core.components.FeedbackBanner
import com.numberone.numberone.core.components.IconBadge
import com.numberone.numberone.core.components.NOButton
import com.numberone.numberone.core.components.NOCard
import com.numberone.numberone.core.components.NOScaffold
import com.numberone.numberone.core.components.NoticeBanner
import com.numberone.numberone.core.components.PaymentStepHeader
import com.numberone.numberone.core.components.ReceiptDropZone
import com.numberone.numberone.core.components.ReceiptPreviewCard
import com.numberone.numberone.core.components.WarningStrip
import com.numberone.numberone.core.components.isValidTxId
import com.numberone.numberone.core.components.ltr
import com.numberone.numberone.core.components.rememberReceiptPicker
import com.numberone.numberone.core.components.SectionTitle
import com.numberone.numberone.core.util.ReceiptFile
import com.numberone.numberone.domain.model.PaymentOrderCreation
import com.numberone.numberone.presentation.screens.purchase.CheckoutScreen
import com.numberone.numberone.presentation.viewmodel.AppViewModel

@Composable
fun StoreScreen(vm: AppViewModel, onBack: () -> Unit, onTopUp: (() -> Unit)? = null) {
    NOScaffold(title = tr("المتجر"), onBack = onBack) {
        StoreBody(vm, onTopUp)
    }
}

/** The catalog + pending payment flow, reusable inside a scrolling column (full screen or a tab). */
@Composable
fun ColumnScope.StoreBody(vm: AppViewModel, onTopUp: (() -> Unit)? = null) {
    // Loads once, and waits for any load already running (e.g. the dashboard's) instead of being dropped by it.
    var storeRequested by remember { mutableStateOf(false) }
    LaunchedEffect(vm.loading) {
        if (!vm.loading && !storeRequested) {
            storeRequested = true
            vm.loadStore()
        }
    }
    NOCard {
        SectionTitle(tr("🛡️ تعليمات حماية رقمك من الحظر"), tr("اتبع هذه القواعد للحفاظ على رقمك الخاص بأمان"))
        Text(tr("• في اليوم الأول للتفعيل: استقبل الرسائل الواردة فقط ولا تبدأ محادثات مع أرقام غير مسجّلة على هاتفك."), style = MaterialTheme.typography.bodySmall)
        Text(tr("• فعّل التحقق بخطوتين فوراً (PIN) لحماية حسابك."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(tr("• تجنّب الرسائل الجماعية والروابط والانضمام لمجموعات كثيرة في الأيام الأولى."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    var receipt by remember { mutableStateOf<ReceiptFile?>(null) }

    val pickReceipt = rememberReceiptPicker(
        onPicked = { receipt = it },
        onError = { vm.fail(it) }
    )

        NOCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconBadge(Icons.Filled.AccountBalanceWallet)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(tr("رصيدك"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        ltr("%.2f USDT".format(java.util.Locale.US, vm.wallet.balance)),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            SectionTitle(
                tr("الباقات والأرقام الخاصة"),
                tr("ادفع من رصيدك فوراً، أو حوّل USDT عبر شبكة BEP20 وأرسل الإيصال.")
            )
        }

        FeedbackBanner(loading = vm.loading, error = vm.error, message = vm.message)

        val activeOrder = vm.pendingOrder
        if (activeOrder != null) {
            PendingPaymentCard(
                order = activeOrder,
                txHash = vm.txHash,
                onTxHashChange = vm::setTxHash,
                loading = vm.loading,
                error = vm.error,
                receipt = receipt,
                onPickReceipt = pickReceipt,
                onConfirmReceipt = {
                    val file = receipt
                    if (file != null) {
                        vm.submitManualOrder(file.bytes, file.mimeType, file.extension)
                    }
                },
                onCancelReceipt = { receipt = null }
            )
            androidx.compose.material3.OutlinedButton(onClick = { receipt = null; vm.cancelPendingOrder() }, modifier = Modifier.fillMaxWidth()) { Text(tr("إلغاء هذا الطلب"), color = com.numberone.numberone.core.components.dangerColor(), fontWeight = FontWeight.Bold) }
        } else {
            if (vm.services.isEmpty() && !vm.loading) EmptyState(tr("لا توجد خدمات متاحة حالياً. حاول لاحقاً."))

            vm.services.forEach { service ->
                CheckoutScreen(
                    service = service,
                    enabled = !vm.loading,
                    walletBalance = vm.wallet.balance,
                    onTopUp = onTopUp,
                    onWalletPurchase = { selectedServiceId, months -> vm.buy(selectedServiceId, months) },
                    onManualPayment = { selectedServiceId, months ->
                        receipt = null
                        vm.createManualOrder(selectedServiceId, months)
                    }
                )
            }
        }

        FeedbackBanner(loading = vm.loading, error = vm.error, message = vm.message)
}

@Composable
private fun PendingPaymentCard(
    order: PaymentOrderCreation,
    txHash: String,
    onTxHashChange: (String) -> Unit,
    loading: Boolean,
    error: String?,
    receipt: ReceiptFile?,
    onPickReceipt: () -> Unit,
    onConfirmReceipt: () -> Unit,
    onCancelReceipt: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val txValue = txHash.trim()
    val txInvalid = txValue.isNotEmpty() && !isValidTxId(txValue)
    val amountText = "%.2f".format(java.util.Locale.US, order.price)

    NOCard {
        PaymentStepHeader(1, tr("حوّل المبلغ"), order.serviceName)
        ExpiryBadge(expiry = order.expiresAt)
        WarningStrip(trf("حوّل المبلغ بالضبط وعبر شبكة {0} فقط. أي شبكة أخرى قد تضيع أموالك.", order.network))
        CopyRow(
            label = trf("المبلغ المطلوب ({0})", order.currency),
            value = ltr("$amountText ${order.currency}"),
            copyValue = amountText,
            emphasized = true
        )
        CopyRow(label = tr("الشبكة"), value = order.network)
        var liveCfg by remember { mutableStateOf(com.numberone.numberone.data.remote.SupabaseWalletDataSource.LiveDepositConfig()) }
        LaunchedEffect(Unit) { liveCfg = com.numberone.numberone.data.remote.SupabaseWalletDataSource().getLiveDepositConfig() }
        val destWallet = order.walletAddress.ifBlank { liveCfg.usdtAddress.orEmpty() }
        CopyRow(label = tr("عنوان التحويل"), value = destWallet, monospace = true)
        CopyRow(label = tr("رقم الطلب"), value = order.orderId, monospace = true)
    }

    NOCard {
        PaymentStepHeader(2, tr("أدخل رقم المعاملة"), tr("الصق TXID بعد إتمام التحويل"), done = txValue.isNotEmpty() && !txInvalid)
        OutlinedTextField(
            value = txHash,
            onValueChange = onTxHashChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = txInvalid,
            label = { Text(tr("رقم المعاملة (64 خانة)")) },
            textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
            supportingText = {
                Text(
                    if (txInvalid) tr("يجب أن يتكوّن من 64 خانة (أرقام وحروف a-f)")
                    else "${ltr(txValue.removePrefix("0x").removePrefix("0X").length.toString())} / 64"
                )
            },
            trailingIcon = {
                IconButton(onClick = { clipboard.getText()?.text?.let { onTxHashChange(it.trim()) } }) {
                    Icon(Icons.Filled.ContentPaste, contentDescription = tr("لصق"), tint = MaterialTheme.colorScheme.primary)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            shape = RoundedCornerShape(16.dp)
        )
    }

    NOCard {
        PaymentStepHeader(3, tr("ارفع إيصال التحويل"), tr("لقطة شاشة واضحة تُظهر المبلغ والعنوان"))
        if (receipt == null) {
            ReceiptDropZone(onPick = onPickReceipt, enabled = !loading)
        } else {
            ReceiptPreviewCard(
                file = receipt,
                loading = loading,
                onConfirm = onConfirmReceipt,
                onReplace = onPickReceipt,
                onRemove = onCancelReceipt,
                confirmLabel = tr("إرسال إثبات الدفع"),
                confirmEnabled = isValidTxId(txValue),
                confirmHint = if (isValidTxId(txValue)) null else tr("أدخل رقم معاملة صالحاً (الخطوة 2) لتفعيل الإرسال.")
            )
            error?.let { NoticeBanner(text = it, isError = true) }
        }
    }
}
