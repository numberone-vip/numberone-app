package com.numberone.app.presentation.wallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.numberone.app.core.util.ClipboardHelper
import com.numberone.app.data.repository.WalletRepository
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun WalletTopUpDialog(
    repository: WalletRepository,
    onDismiss: () -> Unit,
    onCredited: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var config by remember { mutableStateOf<com.numberone.app.data.repository.WalletPaymentSettingsDto?>(null) }
    var configLoading by remember { mutableStateOf(true) }
    var amount by remember { mutableStateOf("") }
    var requestId by remember { mutableStateOf<String?>(null) }
    var txHash by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        repository.getDepositPaymentSettings()
            .onSuccess { config = it }
            .onFailure { error = it.localizedMessage ?: "تعذر تحميل إعدادات الدفع." }
        configLoading = false
    }

    val enabled = config?.isEnabled == true &&
        config?.network?.equals("BEP20", ignoreCase = true) == true &&
        !config?.walletAddress.isNullOrBlank()

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("شحن المحفظة", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (configLoading) {
                    CircularProgressIndicator()
                } else if (!enabled) {
                    Text(
                        "الشحن غير متاح حالياً. لم يتم تفعيل إعداد USDT BEP20 أو لم يتم تحديد عنوان الاستلام بعد.",
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "يمكنك استخدام المتجر ورصيدك الحالي. عند تجهيز وسيلة الدفع ستظهر هنا تلقائياً.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (requestId == null) {
                    Text("الشحن عبر USDT على شبكة BEP20.", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("المبلغ بالدولار USDT") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text("حوّل المبلغ إلى عنوان الاستلام التالي:", style = MaterialTheme.typography.bodyMedium)
                    Text(config?.walletAddress.orEmpty(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    TextButton(onClick = {
                        ClipboardHelper.copyToClipboard(context, "USDT BEP20 address", config?.walletAddress.orEmpty(), "تم نسخ العنوان")
                    }) { Text("نسخ عنوان المحفظة") }
                    Text("رقم طلب الشحن: " + requestId, style = MaterialTheme.typography.labelSmall)
                    OutlinedTextField(
                        value = txHash,
                        onValueChange = { txHash = it.trim() },
                        label = { Text("Transaction Hash / TXID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("أدخل TXID الخاص بتحويلك. التحقق يتم تلقائياً عبر سجل الإيداعات في Binance.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (error != null) Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            if (enabled) {
                Button(
                    enabled = !busy && (if (requestId == null) amount.toDoubleOrNull()?.let { it > 0 } == true else txHash.matches(Regex("^[0-9a-fA-F]{64}$"))),
                    onClick = {
                        scope.launch {
                            busy = true
                            error = null
                            message = ""
                            if (requestId == null) {
                                repository.createDepositRequest(amount.toDoubleOrNull() ?: 0.0)
                                    .onSuccess { requestId = it; message = "تم إنشاء طلب الشحن. حوّل المبلغ ثم أدخل TXID." }
                                    .onFailure { error = it.localizedMessage ?: "تعذر إنشاء طلب الشحن." }
                            } else {
                                if (!submitted) {
                                    repository.submitDepositTransaction(requestId!!, txHash)
                                        .onSuccess { submitted = true }
                                        .onFailure { error = it.localizedMessage ?: "تعذر إرسال TXID." }
                                }
                                if (submitted || error == null) {
                                    repository.verifyDeposit(requestId!!, txHash)
                                        .onSuccess { result ->
                                            message = result.message
                                            if (result.credited) {
                                                onCredited()
                                                onDismiss()
                                            }
                                        }
                                        .onFailure { error = it.localizedMessage ?: "تعذر التحقق من التحويل." }
                                }
                            }
                            busy = false
                        }
                    }
                ) {
                    if (busy) CircularProgressIndicator() else Text(if (requestId == null) "إنشاء طلب الشحن" else if (submitted) "تحقق من الإيداع" else "إرسال TXID والتحقق")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("إغلاق") }
        }
    )
}
