package com.numberone.numberone.presentation.screens.support

import com.numberone.numberone.core.localization.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.numberone.numberone.core.components.NOCard
import com.numberone.numberone.core.components.NOScaffold
import com.numberone.numberone.core.components.SectionTitle

@Composable
fun SupportScreen(onBack: () -> Unit) {
    NOScaffold(title = tr("الدعم والمساعدة"), onBack = onBack) {
        NOCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    Icons.Filled.SupportAgent,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Text(tr("نحن هنا لمساعدتك"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Text(
                tr("للمساعدة في تسجيل الدخول أو الطلبات أو الإيداعات، يرجى التواصل مع فريق الدعم عبر قناة التواصل الرسمية التي زودك بها مسؤول الخدمة."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        NOCard {
            SectionTitle(tr("تسجيل الدخول"), tr("تأكد من البريد الإلكتروني وكلمة المرور. إذا نسيت كلمة المرور تواصل مع الدعم."))
        }
        NOCard {
            SectionTitle(tr("الطلبات والدفع"), tr("بعد الدفع أرفق صورة الإيصال ورقم المعاملة. تتم مراجعة الطلب ثم يظهر الرقم في الاشتراكات."))
        }
        NOCard {
            SectionTitle(tr("الإيداعات"), tr("تتم مراجعة إيداعات شام كاش يدوياً، ويُضاف الرصيد بعد الاعتماد. إيداعات Binance تُتحقق من رقم المعاملة تلقائياً."))
        }
    }
}
