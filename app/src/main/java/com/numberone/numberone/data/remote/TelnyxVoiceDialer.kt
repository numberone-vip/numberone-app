package com.numberone.numberone.data.remote

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.numberone.numberone.core.components.*
import com.numberone.numberone.core.error.ErrorMapper
import com.numberone.numberone.core.localization.*
import com.telnyx.webrtc.sdk.TelnyxClient
import com.telnyx.webrtc.sdk.TokenConfig
import com.telnyx.webrtc.sdk.model.LogLevel
import com.telnyx.webrtc.sdk.model.SocketMethod
import com.telnyx.webrtc.sdk.model.SocketStatus
import com.telnyx.webrtc.sdk.verto.receive.ReceivedMessageBody
import com.telnyx.webrtc.sdk.verto.receive.SocketResponse
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.*
import kotlinx.serialization.json.buildJsonObject
import java.util.UUID

@Serializable data class TelnyxVoiceTokenRes(val ok: Boolean = false, val token: String = "", @SerialName("phone_number") val phoneNumber: String = "", @SerialName("voice_rate_per_min") val voiceRatePerMin: Double = 0.05, @SerialName("wallet_balance") val walletBalance: Double = 0.0, val error: String? = null, val message: String? = null)

@Composable fun TelnyxVoiceDialerDialog(callerNumber: String, voiceRatePerMin: Double = 0.05, walletBalance: Double = 0.0, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val audio = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val client = remember { TelnyxClient(ctx.applicationContext) }
    var hasMic by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val micPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasMic = it }
    var dial by remember { mutableStateOf("+1") }
    var rate by remember { mutableDoubleStateOf(voiceRatePerMin) }
    var balance by remember { mutableDoubleStateOf(walletBalance) }
    var status by remember { mutableStateOf(tr("جاهز للاتصال الصوتي عبر Telnyx")) }
    var calling by remember { mutableStateOf(false) }
    var connected by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(false) }
    var speaker by remember { mutableStateOf(false) }
    var callId by remember { mutableStateOf<UUID?>(null) }
    var dest by remember { mutableStateOf<String?>(null) }
    var elapsedSec by remember { mutableIntStateOf(0) }

    fun end(msg: String? = null) {
        (callId ?: client.call?.callId)?.let { id -> runCatching { client.call?.endCall(id) } }
        callId = null; dest = null; calling = false; connected = false; muted = false; speaker = false; elapsedSec = 0
        runCatching { audio.isSpeakerphoneOn = false; audio.mode = AudioManager.MODE_NORMAL }
        if (msg != null) status = msg
    }

    LaunchedEffect(connected) {
        if (!connected) return@LaunchedEffect
        val maxSec = if (rate > 0.0) ((balance / rate) * 60.0).toInt().coerceAtLeast(55) else 3600
        while (connected) {
            delay(1000L)
            elapsedSec += 1
            if (elapsedSec >= maxSec) { end(tr("انتهى الرصيد المتاح للمكالمة")); break }
        }
    }

    DisposableEffect(client) {
        val obs = Observer<SocketResponse<ReceivedMessageBody>> { res ->
            when (res.status) {
                SocketStatus.ESTABLISHED -> status = tr("جارٍ تسجيل الدخول للصوت…")
                SocketStatus.MESSAGERECEIVED -> when (res.data?.method) {
                    SocketMethod.CLIENT_READY.methodName -> dest?.let { d ->
                        dest = null; status = trf("جارٍ الاتصال بـ {0}…", ltr(d))
                        audio.mode = AudioManager.MODE_IN_COMMUNICATION
                        client.call?.newInvite("Number One", callerNumber, d, "")
                        callId = client.call?.callId
                    }
                    SocketMethod.RINGING.methodName -> status = trf("يرن الآن: {0}", ltr(dial))
                    SocketMethod.ANSWER.methodName -> { connected = true; elapsedSec = 0; status = trf("مكالمة نشطة مع {0}", ltr(dial)) }
                    SocketMethod.BYE.methodName -> end(tr("انتهت المكالمة"))
                }
                SocketStatus.ERROR -> end(res.errorMessage ?: tr("فشل الاتصال الصوتي"))
                SocketStatus.DISCONNECT -> if (calling) end(tr("انقطع الاتصال"))
                else -> Unit
            }
        }
        val live = client.getSocketResponse()
        live.observeForever(obs)
        onDispose { live.removeObserver(obs); end(); runCatching { client.onDestroy() } }
    }

    AlertDialog(
        onDismissRequest = { end(); onDismiss() },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(tr("لوحة الاتصال الصوتية"), fontWeight = FontWeight.Black)
                Text(ltr(callerNumber), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(trf("الرصيد: {0} USDT • الدقيقة: {1} USDT", formatAmount(balance), formatAmount(rate)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text(ltr(dial.ifBlank { "+" }), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                val timer = if (connected) " (${elapsedSec / 60}:${(elapsedSec % 60).toString().padStart(2, '0')})" else ""
                Text(status + timer, style = MaterialTheme.typography.bodySmall, color = if (connected) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                if (calling) Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilledTonalIconButton(onClick = { muted = !muted; runCatching { client.call?.onMuteUnmutePressed() } }) { Icon(if (muted) Icons.Filled.MicOff else Icons.Filled.Mic, null) }
                    FilledTonalIconButton(onClick = { speaker = !speaker; audio.isSpeakerphoneOn = speaker }) { Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = if (speaker) MaterialTheme.colorScheme.primary else LocalContentColor.current) }
                }
                listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("*","0","#")).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { d ->
                            FilledTonalButton(onClick = { val id = callId ?: client.call?.callId; if (calling && id != null) runCatching { client.call?.dtmf(id, d) } else if (dial.length < 18) dial += d }, shape = CircleShape, modifier = Modifier.size(56.dp)) { Text(d, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!calling) IconButton(onClick = { if (dial.isNotEmpty()) dial = dial.dropLast(1) }) { Icon(Icons.AutoMirrored.Filled.Backspace, null) }
                    Button(
                        onClick = {
                            if (!hasMic) { micPerm.launch(Manifest.permission.RECORD_AUDIO); return@Button }
                            if (calling) { end(tr("تم إنهاء المكالمة")); return@Button }
                            if (dial.length < 6) { status = tr("أدخل الرقم مع رمز الدولة مثل +1"); return@Button }
                            if (rate > 0.0 && balance < rate) { status = trf("رصيدك غير كافٍ. تحتاج على الأقل {0} USDT", formatAmount(rate)); return@Button }
                            calling = true; status = tr("جارٍ تأمين قناة الاتصال…")
                            scope.launch {
                                runCatching { SupabaseClientProvider.client.functions.invoke("telnyx-voice-token", buildJsonObject {}).body<TelnyxVoiceTokenRes>() }
                                    .onSuccess { r ->
                                        rate = r.voiceRatePerMin; balance = r.walletBalance
                                        if (r.token.isNotBlank()) { dest = dial; client.tokenLogin(TokenConfig(r.token, "Number One", callerNumber, null, null, null, LogLevel.NONE, false)) }
                                        else end(r.message ?: r.error ?: tr("تعذّر بدء المكالمة"))
                                    }
                                    .onFailure { e ->
                                        val msg = e.message.orEmpty()
                                        if (msg.contains("insufficient_balance") || msg.contains("402")) end(trf("رصيدك غير كافٍ. تحتاج على الأقل {0} USDT للدقيقة", formatAmount(rate)))
                                        else end(ErrorMapper.toUserMessage(e))
                                    }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (calling) dangerColor() else Color(0xFF16A34A)),
                        shape = RoundedCornerShape(50), modifier = Modifier.height(48.dp)
                    ) { Icon(if (calling) Icons.Filled.CallEnd else Icons.Filled.Call, null); Spacer(Modifier.width(6.dp)); Text(if (calling) tr("إنهاء") else tr("اتصال"), fontWeight = FontWeight.Bold) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { end(); onDismiss() }) { Text(tr("إغلاق")) } }
    )
}
