package com.numberone.numberone.data.remote
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.numberone.numberone.core.components.*
import com.numberone.numberone.core.error.ErrorMapper
import com.numberone.numberone.core.localization.*
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.coroutines.launch
import kotlinx.serialization.*
import kotlinx.serialization.json.buildJsonObject

@Serializable data class TelnyxVoiceTokenRes(val token: String = "", @SerialName("phone_number") val phoneNumber: String = "", val error: String? = null)

@Composable fun TelnyxVoiceDialerDialog(callerNumber: String, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasMic by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val micPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasMic = it }
    var dial by remember { mutableStateOf("+1") }
    var status by remember { mutableStateOf(tr("جاهز للاتصال الصوتي عبر Telnyx")) }
    var calling by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Column { Text(tr("لوحة الاتصال الصوتية"), fontWeight = FontWeight.Black); Text(ltr(callerNumber), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) } },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text(ltr(dial.ifBlank { "+" }), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("*","0","#")).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { d ->
                            FilledTonalButton(onClick = { if (dial.length < 18) dial += d }, shape = CircleShape, modifier = Modifier.size(56.dp)) { Text(d, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (dial.isNotEmpty()) dial = dial.dropLast(1) }) { Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = tr("مسح")) }
                    Button(
                        onClick = {
                            if (!hasMic) { micPerm.launch(Manifest.permission.RECORD_AUDIO); return@Button }
                            if (calling) { calling = false; status = tr("تم إنهاء المكالمة"); return@Button }
                            if (dial.length < 6) { status = tr("أدخل الرقم مع رمز الدولة مثل +1"); return@Button }
                            calling = true; status = tr("جارٍ تأمين قناة الاتصال…")
                            scope.launch {
                                runCatching { SupabaseClientProvider.client.functions.invoke("telnyx-voice-token", buildJsonObject {}).body<TelnyxVoiceTokenRes>() }
                                    .onSuccess { res -> status = if (res.token.isNotBlank()) trf("متصل من {0} ➔ {1}", ltr(callerNumber), ltr(dial)) else { calling = false; res.error ?: tr("تأكد من ضبط TELNYX_CONNECTION_ID") } }
                                    .onFailure { calling = false; status = ErrorMapper.toUserMessage(it) }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (calling) dangerColor() else Color(0xFF16A34A)),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(if (calling) Icons.Filled.CallEnd else Icons.Filled.Call, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (calling) tr("إنهاء") else tr("اتصال"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("إغلاق")) } }
    )
}
