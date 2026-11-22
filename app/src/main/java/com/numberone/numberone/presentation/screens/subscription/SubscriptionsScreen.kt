package com.numberone.numberone.presentation.screens.subscription

import com.numberone.numberone.core.localization.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.numberone.numberone.core.components.CopyField
import com.numberone.numberone.core.components.ExpiryBadge
import com.numberone.numberone.core.components.FeedbackBanner
import com.numberone.numberone.core.components.NOButton
import com.numberone.numberone.core.components.NOCard
import com.numberone.numberone.core.components.NOScaffold
import com.numberone.numberone.core.components.SectionTitle
import com.numberone.numberone.core.components.StatusChip
import com.numberone.numberone.domain.model.Subscription
import com.numberone.numberone.presentation.viewmodel.AppViewModel

@Composable
fun SubscriptionsScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onSms: () -> Unit,
    onAccessCode: () -> Unit
) {
    NOScaffold(title = tr("اشتراكاتي"), onBack = onBack) {
        FeedbackBanner(loading = false, error = vm.error, message = vm.message)

        if (vm.subscriptions.isEmpty()) {
            NOCard {
                SectionTitle(
                    tr("لا توجد بيانات اشتراك"),
                    tr("استخدم رمز الوصول للتحقق من اشتراكك ورسائلك.")
                )
                NOButton(text = tr("استخدام رمز الوصول"), onClick = onAccessCode)
            }
        } else {
            vm.subscriptions.forEach { sub -> SubscriptionCard(sub) }
            NOButton(text = tr("فتح صندوق الرسائل"), onClick = onSms)
        }
    }
}

@Composable
private fun SubscriptionCard(sub: Subscription) {
    NOCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    sub.serviceName ?: tr("الاشتراك"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            sub.status?.takeIf { it.isNotBlank() }?.let { StatusChip(it) }
        }
        if (sub.phoneNumber.isNotBlank()) CopyField(label = tr("الرقم"), value = sub.phoneNumber)
        ExpiryBadge(expiry = sub.expiryDate)
        sub.latestOtp?.takeIf { it.isNotBlank() }?.let { CopyField(label = tr("آخر رمز تحقق"), value = it) }
    }
}
