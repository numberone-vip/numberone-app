package com.numberone.numberone.presentation.screens.subscription
import com.numberone.numberone.core.localization.tr
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.numberone.numberone.core.components.*
import com.numberone.numberone.domain.model.Subscription
import com.numberone.numberone.presentation.viewmodel.AppViewModel

@Composable
fun SubscriptionsScreen(vm: AppViewModel, onBack: () -> Unit, onSms: () -> Unit, onAccessCode: () -> Unit) {
    var uuidInput by remember { mutableStateOf("") }
    val ltrStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
    NOScaffold(title = tr("اشتراكاتي"), onBack = onBack) {
        FeedbackBanner(loading = vm.loading, error = vm.error, message = vm.message)
        NOCard {
            SectionTitle(tr("جلب اشتراك بواسطة معرّف UUID"), tr("أدخل معرّف الاشتراك (UUID) لعرض تفاصيل رقمك مباشرة."))
            OutlinedTextField(value = uuidInput, onValueChange = { uuidInput = it }, label = { Text(tr("معرّف الاشتراك (UUID)")) }, singleLine = true, textStyle = ltrStyle, modifier = Modifier.fillMaxWidth())
            NOButton(text = tr("عرض تفاصيل الاشتراك"), onClick = { vm.loadSubscriptionById(uuidInput) }, enabled = uuidInput.trim().isNotBlank() && !vm.loading)
        }
        if (vm.subscriptions.isEmpty()) {
            EmptyState(tr("لا توجد اشتراكات نشطة حالياً"))
        } else {
            vm.subscriptions.forEach { sub ->
                NOCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(sub.serviceName ?: tr("الاشتراك"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        sub.status?.takeIf { it.isNotBlank() }?.let { StatusChip(it) }
                    }
                    if (sub.phoneNumber.isNotBlank()) CopyField(label = tr("الرقم"), value = sub.phoneNumber)
                    sub.id?.takeIf { it.isNotBlank() }?.let { id ->
                        CopyField(label = tr("معرّف الاشتراك (UUID)"), value = id)
                        OutlinedButton(onClick = { vm.loadSubscriptionById(id) }, enabled = !vm.loading, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text(tr("تحديث بيانات الاشتراك"), fontWeight = FontWeight.Bold) }
                    }
                    ExpiryBadge(expiry = sub.expiryDate)
                    sub.latestOtp?.takeIf { it.isNotBlank() }?.let { CopyField(label = tr("آخر رمز تحقق"), value = it) }
                }
            }
            NOButton(text = tr("فتح صندوق الرسائل"), onClick = onSms)
        }
    }
}
