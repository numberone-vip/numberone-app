package com.numberone.numberone.presentation.screens.sms

import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.core.localization.trf
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.numberone.numberone.core.components.CopyField
import com.numberone.numberone.core.components.EmptyState
import com.numberone.numberone.core.components.FeedbackBanner
import com.numberone.numberone.core.components.IconBadge
import com.numberone.numberone.core.components.NOCard
import com.numberone.numberone.core.components.NOScaffold
import com.numberone.numberone.core.components.formatDateTime
import com.numberone.numberone.core.theme.NumberOneColors
import com.numberone.numberone.domain.model.SmsMessage
import com.numberone.numberone.presentation.viewmodel.AppViewModel

private val GOLD_BRUSH = Brush.linearGradient(
    listOf(NumberOneColors.GoldLight, NumberOneColors.GoldMid, NumberOneColors.GoldDeep)
)

@Composable
fun SmsInboxScreen(vm: AppViewModel, onBack: () -> Unit) {
    LaunchedEffect(vm.sms.size) { vm.markSmsSeen() }
    NOScaffold(title = tr("الرسائل"), onBack = onBack) {
        FeedbackBanner(loading = vm.loading, error = vm.error, message = null)

        // Summary header: count of codes received and a short hint.
        NOCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(Icons.Filled.Sms, tint = MaterialTheme.colorScheme.primary, size = 48.dp)
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
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconBadge(Icons.Filled.Sms, tint = MaterialTheme.colorScheme.primary, size = 72.dp)
                EmptyState(tr("لا توجد رسائل بعد.\nأدخل رمز الوصول من صفحة «اشتراكاتي» لعرض رسائلك."))
            }
        }

        vm.sms.forEach { message -> SmsCard(message) }
    }
}

@Composable
private fun SmsCard(message: SmsMessage) {
    // Detects a one-time code (4 to 8 digits) so it can be highlighted and copied with one tap.
    val code = remember(message.messageText) {
        Regex("\\b\\d{4,8}\\b").find(message.messageText)?.value
    }
    val initial = message.sender.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "#"

    NOCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Gold initial badge: keeps the sender recognisable at a glance.
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(GOLD_BRUSH),
                contentAlignment = Alignment.Center
            ) {
                Text(initial, fontWeight = FontWeight.Black, color = NumberOneColors.Obsidian)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    message.sender.ifBlank { tr("رسالة") },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    formatDateTime(message.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // The code itself, large and in the app's gold accent.
        code?.let { CodeHighlight(it) }

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

@Composable
private fun CodeHighlight(code: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                tr("رمز التحقق"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                code,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
