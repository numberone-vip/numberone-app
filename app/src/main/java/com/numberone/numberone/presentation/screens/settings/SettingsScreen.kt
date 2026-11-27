package com.numberone.numberone.presentation.screens.settings

import com.numberone.numberone.core.localization.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.numberone.numberone.core.components.InitialAvatar
import com.numberone.numberone.core.components.NOCard
import com.numberone.numberone.core.components.NOScaffold
import com.numberone.numberone.core.components.SectionTitle
import com.numberone.numberone.core.localization.AppLanguage
import com.numberone.numberone.di.AppContainer
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit, onLogout: () -> Unit, userEmail: String? = null) {
    val scope = rememberCoroutineScope()
    val prefs = AppContainer.appPreferences
    val language by prefs.appLanguage.collectAsState(initial = AppLanguage.ENGLISH)
    val theme by prefs.appTheme.collectAsState(initial = "dark")

    val myUserId = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        myUserId.value = AppContainer.userRepository.getCurrentUser().getOrNull()?.id.orEmpty()
    }
    NOScaffold(title = tr("حسابي"), onBack = onBack) {
        // Profile
        NOCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                InitialAvatar(text = userEmail, size = 56.dp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(tr("حسابك على Number One"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        userEmail ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (myUserId.value.isNotBlank()) {
                com.numberone.numberone.core.components.CopyField(
                    label = tr("معرّف حسابك (ID)"),
                    value = myUserId.value
                )
            }
        }

        NOCard {
            SectionTitle(tr("اللغة"), tr("تنطبق على القوائم والرسائل"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = language.isArabic,
                    onClick = { scope.launch { prefs.setLanguage(AppLanguage.ARABIC) } },
                    label = { Text(tr("العربية")) }
                )
                FilterChip(
                    selected = !language.isArabic,
                    onClick = { scope.launch { prefs.setLanguage(AppLanguage.ENGLISH) } },
                    label = { Text("English") }
                )
            }
        }

        NOCard {
            SectionTitle(tr("المظهر"), tr("اختر الوضع الأنسب لعينيك"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("system" to tr("حسب الجهاز"), "light" to tr("فاتح"), "dark" to tr("داكن")).forEach { (value, label) ->
                    FilterChip(
                        selected = theme == value,
                        onClick = { scope.launch { prefs.setTheme(value) } },
                        label = { Text(label) }
                    )
                }
            }
        }

        NOCard {
            SectionTitle(tr("حول التطبيق"), "Number One")
            Text(
                tr("منصة متكاملة لإدارة الأرقام الافتراضية وخدمات التفعيل والاشتراكات الرقمية."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                tr("• التفعيل الفوري: أرقام مخصصة لاستقبال رمز التحقق لمرة واحدة، مع استرداد تلقائي للمبلغ في حال عدم وصول الرمز وفق شروط الخدمة."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                tr("• الأرقام الشهرية والسنوية: احتفظ برقمك واستخدمه باستمرار من خلال اشتراك شهري يتجدد تلقائياً أو باقة سنوية طويلة الأمد."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Number One | Your Number. Your Control.",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text(tr("تسجيل الخروج"), fontWeight = FontWeight.SemiBold)
        }
    }
}
