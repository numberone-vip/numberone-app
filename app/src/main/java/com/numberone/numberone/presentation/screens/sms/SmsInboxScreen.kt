package com.numberone.numberone.presentation.screens.sms

import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.core.localization.trf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import com.numberone.numberone.core.components.CopyField
import com.numberone.numberone.core.components.EmptyState
import com.numberone.numberone.core.components.FeedbackBanner
import com.numberone.numberone.core.components.IconBadge
import com.numberone.numberone.core.components.NOCard
import com.numberone.numberone.core.components.NOScaffold
import com.numberone.numberone.core.components.formatDateTime
import com.numberone.numberone.domain.model.SmsMessage
import com.numberone.numberone.presentation.viewmodel.AppViewModel

@Composable
fun SmsInboxScreen(vm: AppViewModel, onBack: () -> Unit) {
    LaunchedEffect(Unit) { vm.refreshAccountSms() }
    NOScaffold(title = tr("الرسائل"), onBack = onBack) {
        FeedbackBanner(loading = vm.loading, error = vm.error, message = null)

        NOCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconBadge(Icons.Filled.Sms)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(tr("صندوق الوارد"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (vm.sms.isEmpty()) tr("ستظهر هنا رموز التحقق فور وصولها")
                        else trf("{0} رسالة • اضغط على الرمز لنسخه", vm.sms.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (vm.sms.isEmpty() && !vm.loading) {
            EmptyState(tr("لا توجد رسائل بعد.\nستظهر هنا رسائل رقمك النشط تلقائياً."))
        }
        vm.sms.forEach { message -> SmsCard(message) }
    }
}

@Composable
private fun SmsCard(message: SmsMessage) {
    // Detects a one-time code (4 to 8 digits) so it can be copied with one tap.
    val code = remember(message.messageText) {
        Regex("\\b\\d{4,8}\\b").find(message.messageText)?.value
    }
    NOCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    message.sender.ifBlank { tr("رسالة") },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    formatDateTime(message.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ) {
            Text(
                message.messageText,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        code?.let { CopyField(label = tr("رمز التحقق"), value = it) }
    }
}
