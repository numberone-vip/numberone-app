package com.numberone.numberone.data.remote

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.numberone.numberone.core.components.*
import com.numberone.numberone.core.error.ErrorMapper
import com.numberone.numberone.core.localization.*
import com.numberone.numberone.core.theme.NumberOneColors
import com.telnyx.webrtc.sdk.TelnyxClient as WebRtcClient
import com.telnyx.webrtc.sdk.TokenConfig
import com.telnyx.webrtc.sdk.model.LogLevel
import com.telnyx.webrtc.sdk.model.SocketMethod
import com.telnyx.webrtc.sdk.model.SocketStatus
import com.telnyx.webrtc.sdk.verto.receive.ReceivedMessageBody
import com.telnyx.webrtc.sdk.verto.receive.SocketResponse
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import java.util.UUID

data class PhoneContactItem(val name: String, val phone: String)

suspend fun loadDeviceContacts(ctx: Context): List<PhoneContactItem> = withContext(Dispatchers.IO) {
    val list = mutableListOf<PhoneContactItem>()
    val seen = mutableSetOf<String>()
    runCatching {
        ctx.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
            null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )?.use { c ->
            val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (c.moveToNext() && list.size < 400) {
                val n = if (nameIdx >= 0) c.getString(nameIdx)?.trim().orEmpty() else ""
                val raw = if (numIdx >= 0) c.getString(numIdx)?.replace(Regex("[^+0-9]"), "").orEmpty() else ""
                if (n.isNotBlank() && raw.length >= 6 && seen.add("$n|$raw")) list += PhoneContactItem(n, raw)
            }
        }
    }
    list
}

@Composable
fun CallsTab(
    offer: TelnyxOffer?,
    activity: TelnyxUserActivity?,
    selectedCaller: String,
    onSelectCaller: (String) -> Unit,
    onOpenBuyNumber: () -> Unit,
    onOpenMessageTo: (String) -> Unit,
    onRefreshAll: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val audio = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val rtc = remember { WebRtcClient(ctx.applicationContext) }
    var subTab by remember { mutableIntStateOf(0) }
    var dial by remember { mutableStateOf("") }
    var hasMic by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val micPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasMic = it }
    var hasContacts by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) }
    val contactsPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasContacts = it }
    var contacts by remember { mutableStateOf<List<PhoneContactItem>>(emptyList()) }
    var contactQuery by remember { mutableStateOf("") }
    var calling by remember { mutableStateOf(false) }
    var connected by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(false) }
    var speaker by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var callId by remember { mutableStateOf<UUID?>(null) }
    var dest by remember { mutableStateOf<String?>(null) }
    var elapsedSec by remember { mutableIntStateOf(0) }
    var pickCallerOpen by remember { mutableStateOf(false) }

    val numbers = offer?.numbers.orEmpty()
    val activeCaller = selectedCaller.takeIf { it.isNotBlank() && numbers.any { n -> n.phoneNumber == it } }
        ?: numbers.firstOrNull()?.phoneNumber.orEmpty()
    val rate = offer?.voiceRatePerMin ?: 0.05
    val balance = offer?.walletBalance ?: 0.0

    fun endCall(msg: String? = null) {
        (callId ?: rtc.call?.callId)?.let { id -> runCatching { rtc.call?.endCall(id) } }
        callId = null; dest = null; calling = false; connected = false; muted = false; speaker = false; elapsedSec = 0
        runCatching { audio.isSpeakerphoneOn = false; audio.mode = AudioManager.MODE_NORMAL }
        statusText = msg
        onRefreshAll()
    }

    LaunchedEffect(hasContacts) { if (hasContacts) contacts = loadDeviceContacts(ctx) }
    LaunchedEffect(connected) {
        if (!connected) return@LaunchedEffect
        val maxSec = if (rate > 0.0) ((balance / rate) * 60.0).toInt().coerceAtLeast(55) else 3600
        while (connected) { delay(1000L); elapsedSec += 1; if (elapsedSec >= maxSec) { endCall(tr("انتهى الرصيد المتاح للمكالمة")); break } }
    }

    DisposableEffect(rtc) {
        val obs = Observer<SocketResponse<ReceivedMessageBody>> { res ->
            when (res.status) {
                SocketStatus.ESTABLISHED -> statusText = tr("جارٍ تسجيل الدخول للصوت…")
                SocketStatus.MESSAGERECEIVED -> when (res.data?.method) {
                    SocketMethod.CLIENT_READY.methodName -> dest?.let { d ->
                        dest = null; statusText = trf("جارٍ الاتصال بـ {0}…", ltr(d))
                        audio.mode = AudioManager.MODE_IN_COMMUNICATION
                        rtc.call?.newInvite("Number One", activeCaller, d, "")
                        callId = rtc.call?.callId
                    }
                    SocketMethod.RINGING.methodName -> statusText = trf("يرن الآن: {0}", ltr(dial))
                    SocketMethod.ANSWER.methodName -> { connected = true; elapsedSec = 0; statusText = trf("مكالمة نشطة • {0}", ltr(dial)) }
                    SocketMethod.BYE.methodName -> endCall(tr("انتهت المكالمة"))
                }
                SocketStatus.ERROR -> endCall(res.errorMessage ?: tr("فشل الاتصال الصوتي"))
                SocketStatus.DISCONNECT -> if (calling) endCall(tr("انقطع الاتصال"))
                else -> Unit
            }
        }
        val live = rtc.getSocketResponse()
        live.observeForever(obs)
        onDispose { live.removeObserver(obs); endCall() }
    }

    fun startCallTo(target: String) {
        val clean = target.replace(Regex("[^+0-9]"), "")
        if (activeCaller.isBlank()) { onOpenBuyNumber(); return }
        if (!hasMic) { micPerm.launch(Manifest.permission.RECORD_AUDIO); return }
        if (clean.length < 6) { statusText = tr("أدخل الرقم مع رمز الدولة مثل +1"); return }
        if (rate > 0.0 && balance < rate) { statusText = trf("رصيدك غير كافٍ. تحتاج على الأقل {0} USDT", formatAmount(rate)); return }
        dial = clean; subTab = 0; calling = true; statusText = tr("جارٍ تأمين قناة الاتصال…")
        scope.launch {
            runCatching { SupabaseClientProvider.client.functions.invoke("telnyx-voice-token", buildJsonObject {}).body<TelnyxVoiceTokenRes>() }
                .onSuccess { r ->
                    if (r.token.isNotBlank()) { dest = clean; rtc.tokenLogin(TokenConfig(r.token, "Number One", activeCaller, null, null, null, LogLevel.NONE, false)) }
                    else endCall(r.message ?: r.error ?: tr("تعذّر بدء المكالمة"))
                }
                .onFailure { endCall(ErrorMapper.toUserMessage(it)) }
        }
    }
    if (pickCallerOpen && numbers.isNotEmpty()) {
        AlertDialog(onDismissRequest = { pickCallerOpen = false }, title = { Text(tr("اختر رقم الاتصال الصادر"), fontWeight = FontWeight.Black) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { numbers.forEach { n -> OutlinedButton(onClick = { onSelectCaller(n.phoneNumber); pickCallerOpen = false }, modifier = Modifier.fillMaxWidth()) { Text(ltr(n.phoneNumber), fontWeight = FontWeight.Bold) } } } },
            confirmButton = {}, dismissButton = { TextButton(onClick = { pickCallerOpen = false }) { Text(tr("إغلاق")) } })
    }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(tr("المكالمات"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (activeCaller.isNotBlank()) successColor() else MaterialTheme.colorScheme.outline))
                Text(if (activeCaller.isNotBlank()) tr("متصل") else tr("بدون خط"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (activeCaller.isNotBlank()) successColor() else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), modifier = Modifier.fillMaxWidth()) {
            val subTabs = listOf(tr("لوحة الأرقام"), tr("السجل"), tr("جهات الاتصال"), tr("البريد الصوتي"))
            Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                subTabs.forEachIndexed { idx, label ->
                    val sel = subTab == idx
                    Surface(onClick = { subTab = idx }, shape = RoundedCornerShape(10.dp), color = if (sel) MaterialTheme.colorScheme.surface else Color.Transparent, modifier = Modifier.weight(1f)) {
                        Text(label, modifier = Modifier.padding(vertical = 9.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium, color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        when (subTab) {
            0 -> Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                Spacer(Modifier.height(2.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (activeCaller.isNotBlank()) {
                        Surface(onClick = { if (numbers.size > 1) pickCallerOpen = true }, shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)) {
                            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)) { Text("US", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary) }
                                Text(trf("الاتصال من {0} ›", ltr(activeCaller)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else OutlinedButton(onClick = onOpenBuyNumber, shape = RoundedCornerShape(50)) { Text(tr("+ احصل على رقم أعمال لتفعيل المكالمات"), fontWeight = FontWeight.Bold) }
                    if (dial.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(ltr(dial), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        if (!calling) IconButton(onClick = { dial = dial.dropLast(1) }) { Icon(Icons.AutoMirrored.Filled.Backspace, null) }
                    }
                    val timer = if (connected) " (${elapsedSec / 60}:${(elapsedSec % 60).toString().padStart(2, '0')})" else ""
                    statusText?.let { Text(it + timer, style = MaterialTheme.typography.bodySmall, color = if (connected) successColor() else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }
                    if (calling) Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        FilledTonalIconButton(onClick = { muted = !muted; runCatching { rtc.call?.onMuteUnmutePressed() } }) { Icon(if (muted) Icons.Filled.MicOff else Icons.Filled.Mic, null) }
                        FilledTonalIconButton(onClick = { speaker = !speaker; audio.isSpeakerphoneOn = speaker }) { Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = if (speaker) MaterialTheme.colorScheme.primary else LocalContentColor.current) }
                    }
                }
                val keys = listOf(listOf("1" to "", "2" to "ABC", "3" to "DEF"), listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"), listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"), listOf("*" to "", "0" to "+", "#" to ""))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    keys.forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            row.forEach { (digit, letters) ->
                                Box(modifier = Modifier.size(66.dp).clip(CircleShape).clickable {
                                    val id = callId ?: rtc.call?.callId
                                    if (calling && id != null) runCatching { rtc.call?.dtmf(id, digit) }
                                    else if (dial.length < 18) dial += if (digit == "0" && dial.isEmpty()) "+" else digit
                                }, contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(digit, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                                        if (letters.isNotEmpty()) Text(letters, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.2.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                Box(modifier = Modifier.padding(bottom = 8.dp).size(66.dp).shadow(12.dp, CircleShape).clip(CircleShape).background(if (calling) dangerColor() else NumberOneColors.GoldMid).clickable { if (calling) endCall(tr("تم إنهاء المكالمة")) else startCallTo(dial) }, contentAlignment = Alignment.Center) {
                    Icon(if (calling) Icons.Filled.CallEnd else Icons.Filled.Call, null, tint = if (calling) Color.White else NumberOneColors.Obsidian, modifier = Modifier.size(28.dp))
                }
            }
            1 -> Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val calls = activity?.recentCalls.orEmpty()
                if (calls.isEmpty()) NOCard { Text(tr("لا توجد مكالمات مسجلة بعد."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                else calls.forEach { c ->
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.AutoMirrored.Filled.CallMade, null, tint = successColor(), modifier = Modifier.size(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(c.description.ifBlank { tr("مكالمة صوتية دولية") }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${c.createdAt.take(16).replace("T", " ")} • ${formatAmount(c.amount)} ${c.currency}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            2 -> Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!hasContacts) NOCard { Text(tr("اسمح بالوصول إلى جهات الاتصال للاتصال ومراسلة أصدقائك مباشرة."), style = MaterialTheme.typography.bodySmall); Button(onClick = { contactsPerm.launch(Manifest.permission.READ_CONTACTS) }) { Text(tr("السماح بجهات الاتصال"), fontWeight = FontWeight.Bold) } }
                else {
                    OutlinedTextField(value = contactQuery, onValueChange = { contactQuery = it }, placeholder = { Text(tr("بحث في جهات الاتصال…")) }, leadingIcon = { Icon(Icons.Filled.Search, null) }, singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                    val filtered = remember(contacts, contactQuery) { if (contactQuery.isBlank()) contacts else contacts.filter { it.name.contains(contactQuery, true) || it.phone.contains(contactQuery) } }
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        filtered.forEach { person ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) { Text(person.name.take(2), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary) }
                                Column(modifier = Modifier.weight(1f)) { Text(person.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(ltr(person.phone), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                IconButton(onClick = { onOpenMessageTo(person.phone) }) { Icon(Icons.AutoMirrored.Filled.Message, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                                IconButton(onClick = { startCallTo(person.phone) }) { Icon(Icons.Filled.Call, null, tint = MaterialTheme.colorScheme.primary) }
                            }
                        }
                    }
                }
            }
            else -> Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Voicemail, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(10.dp))
                Text(tr("لا توجد رسائل بريد صوتي"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(tr("تظهر هنا الرسائل الصوتية المرسلة إليك عند عدم الرد وتُحفظ لمدة 30 يوماً."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
            }
        }
    }
}
