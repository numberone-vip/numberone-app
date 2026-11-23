package com.numberone.numberone.presentation.screens.support

import android.content.Intent
import android.net.Uri
import com.numberone.numberone.core.localization.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.numberone.numberone.core.components.NOCard
import com.numberone.numberone.core.components.NOScaffold
import com.numberone.numberone.core.components.SectionTitle

@Composable
fun SupportScreen(onBack: () -> Unit) {
    val context = LocalContext.current

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
                tr("للمساعدة في تسجيل الدخول أو الطلبات أو الإيداعات، يُرجى التواصل مباشرة مع فريق الدعم الفني عبر قنواتنا الرسمية:"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me"))
                    runCatching { context.startActivity(intent) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Telegram: @NumbersStore_Sy",
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me"))
                    runCatching { context.startActivity(intent) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "WhatsApp: +13343908826",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        NOCard {
            SectionTitle(tr("تسجيل الدخول"), tr("تأكد من البريد الإلكتروني وكلمة المرور. في حال نسيان كلمة المرور يُرجى التواصل مع الدعم الفني."))
        }
        NOCard {
            SectionTitle(tr("الطلبات والدفع"), tr("بعد إتمام الدفع أرفق صورة الإيصال ورقم المعاملة. تتم مراجعة الطلب ليظهر الرقم مباشرة في قسم الاشتراكات."))
        }
        NOCard {
            SectionTitle(tr("الإيداعات"), tr("تتم مراجعة إيداعات شام كاش يدوياً ويُضاف الرصيد فور الاعتماد، بينما تُعالج إيداعات Binance تلقائياً عبر رقم المعاملة."))
        }
    }
}
