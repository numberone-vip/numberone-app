package com.numberone.numberone.presentation.screens.support

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.numberone.numberone.core.components.NOCard
import com.numberone.numberone.core.components.NOScaffold
import com.numberone.numberone.core.components.SectionTitle
import com.numberone.numberone.core.components.formatDateTime
import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.data.remote.SupportChatSource
import com.numberone.numberone.domain.model.ChatMessage
import kotlinx.coroutines.launch

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
                tr("للمساعدة في تسجيل الدخول أو الطلبات أو الإيداعات، راسل فريق الدعم من المحادثة أدناه."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SupportChatCard()

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

/** Two-way chat with the support team. The customer only ever sees their own conversation (enforced by the server). */
@Composable
private fun SupportChatCard() {
    val scope = rememberCoroutineScope()
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            runCatching { SupportChatSource.customerList() }
                .onSuccess { messages = it; error = null }
                .onFailure { error = it.message?.lineSequence()?.firstOrNull() }
            loaded = true
        }
    }

    LaunchedEffect(Unit) { reload() }

    NOCard {
        SectionTitle(tr("راسل فريق الدعم"), tr("اكتب رسالتك هنا وسيرد عليك فريق الدعم داخل التطبيق."))
        if (loaded && messages.isEmpty()) {
            Text(tr("لا توجد رسائل بعد"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        messages.forEach { m ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (m.fromAdmin) Arrangement.Start else Arrangement.End
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (m.fromAdmin) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (m.fromAdmin) tr("فريق الدعم") else tr("أنت"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(m.body, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            formatDateTime(m.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(tr("اكتب رسالتك…")) },
            maxLines = 4,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                val body = text.trim()
                scope.launch {
                    sending = true
                    runCatching { SupportChatSource.customerSend(body) }
                        .onSuccess { text = ""; error = null; reload() }
                        .onFailure { error = it.message?.lineSequence()?.firstOrNull() }
                    sending = false
                }
            },
            enabled = text.isNotBlank() && !sending,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) { Text(tr("إرسال"), fontWeight = FontWeight.Bold) }
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    }
}
