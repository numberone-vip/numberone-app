
@Composable fun TelnyxVoiceDialerDialog(callerNumber: String, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val audio = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val client = remember { TelnyxClient(ctx.applicationContext) }
    var hasMic by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val micPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasMic = it }
    var dial by remember { mutableStateOf("+1") }
    var status by remember { mutableStateOf(tr("جاهز للاتصال الصوتي عبر Telnyx")) }
    var calling by remember { mutableStateOf(false) }
    var connected by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(false) }
    var speaker by remember { mutableStateOf(false) }
    var callId by remember { mutableStateOf<UUID?>(null) }
    var dest by remember { mutableStateOf<String?>(null) }

    fun end(msg: String? = null) {
        callId?.let { runCatching { client.endCall(it) } }
        callId = null; dest = null; calling = false; connected = false; muted = false; speaker = false
        runCatching { audio.isSpeakerphoneOn = false; audio.mode = AudioManager.MODE_NORMAL }
        if (msg != null) status = msg
    }

    LaunchedEffect(client) {
        client.socketResponseFlow.collect { res ->
            when (res.status) {
                SocketStatus.ESTABLISHED -> status = tr("جارٍ تسجيل الدخول للصوت…")
                SocketStatus.MESSAGERECEIVED -> when (res.data?.method) {
                    SocketMethod.CLIENT_READY.methodName -> dest?.let { d ->
                        dest = null; status = trf("جارٍ الاتصال بـ {0}…", ltr(d))
                        audio.mode = AudioManager.MODE_IN_COMMUNICATION
                        callId = client.call.newInvite("Number One", callerNumber, d, "").callId
                    }
                    SocketMethod.RINGING.methodName -> status = trf("يرن الآن: {0}", ltr(dial))
                    SocketMethod.ANSWER.methodName -> { connected = true; status = trf("مكالمة نشطة مع {0}", ltr(dial)) }
                    SocketMethod.BYE.methodName -> end((res.data?.result as? ByeResponse)?.cause?.let { trf("انتهت ({0})", it) } ?: tr("انتهت المكالمة"))
                }
                SocketStatus.ERROR -> end(res.errorMessage ?: tr("فشل الاتصال الصوتي"))
                SocketStatus.DISCONNECT -> if (calling) end(tr("انقطع الاتصال"))
                else -> Unit
            }
        }
    }
    DisposableEffect(Unit) { onDispose { end(); runCatching { client.disconnect() } } }

    AlertDialog(
        onDismissRequest = { end(); onDismiss() },
        title = { Column { Text(tr("لوحة الاتصال الصوتية"), fontWeight = FontWeight.Black); Text(ltr(callerNumber), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) } },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text(ltr(dial.ifBlank { "+" }), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text(status, style = MaterialTheme.typography.bodySmall, color = if (connected) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                if (calling) Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilledTonalIconButton(onClick = { muted = !muted; runCatching { client.call.onMuteUnmutePressed() } }) { Icon(if (muted) Icons.Filled.MicOff else Icons.Filled.Mic, null) }
                    FilledTonalIconButton(onClick = { speaker = !speaker; audio.isSpeakerphoneOn = speaker }) { Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = if (speaker) MaterialTheme.colorScheme.primary else LocalContentColor.current) }
                }
                listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("*","0","#")).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { d ->
                            FilledTonalButton(onClick = { if (calling && callId != null) runCatching { client.call.dtmf(callId!!, d) } else if (dial.length < 18) dial += d }, shape = CircleShape, modifier = Modifier.size(56.dp)) { Text(d, fontWeight = FontWeight.Bold) }
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
                            calling = true; status = tr("جارٍ تأمين قناة الاتصال…")
                            scope.launch {
                                runCatching { SupabaseClientProvider.client.functions.invoke("telnyx-voice-token", buildJsonObject {}).body<TelnyxVoiceTokenRes>() }
                                    .onSuccess { r -> if (r.token.isNotBlank()) { dest = dial; client.tokenLogin(TokenConfig(r.token, "Number One", callerNumber, null, null, null, false)) } else end(r.error ?: tr("تأكد من ضبط TELNYX_CONNECTION_ID")) }
                                    .onFailure { end(ErrorMapper.toUserMessage(it)) }
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
