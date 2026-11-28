package com.numberone.numberone.data.remote

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.numberone.numberone.core.components.*
import com.numberone.numberone.core.error.ErrorMapper
import com.numberone.numberone.core.localization.*
import com.numberone.numberone.core.theme.NumberOneColors
import com.numberone.numberone.presentation.util.normalizeDigits
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.call.body
import kotlinx.coroutines.launch
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.util.UUID

@Serializable data class TelnyxNumberItem(val id: String = "", @SerialName("user_id") val userId: String = "", val email: String? = null, @SerialName("phone_number") val phoneNumber: String = "", val status: String = "", @SerialName("monthly_price") val monthlyPrice: Double = 0.0, @SerialName("expires_at") val expiresAt: String? = null, @SerialName("subscription_id") val subscriptionId: String? = null)
@Serializable data class TelnyxOffer(val enabled: Boolean = false, @SerialName("monthly_price") val monthlyPrice: Double = 0.0, @SerialName("markup_percent") val markupPercent: Double = 100.0, @SerialName("voice_rate_per_min") val voiceRatePerMin: Double = 0.05, @SerialName("wallet_balance") val walletBalance: Double = 0.0, val numbers: List<TelnyxNumberItem> = emptyList())
@Serializable data class TelnyxAvailableNumber(@SerialName("phone_number") val phoneNumber: String = "", @SerialName("country_code") val countryCode: String = "US", val region: String = "", @SerialName("provider_cost") val providerCost: Double = 0.0, @SerialName("customer_price") val customerPrice: Double = 0.0, val features: List<String> = emptyList())
@Serializable data class TelnyxSearchRes(val ok: Boolean = false, @SerialName("markup_percent") val markupPercent: Double = 100.0, val numbers: List<TelnyxAvailableNumber> = emptyList(), val errors: List<String> = emptyList(), @SerialName("unavailable_countries") val unavailableCountries: Map<String, String> = emptyMap())
@Serializable data class TelnyxOrderResult(val ok: Boolean = false, @SerialName("purchase_id") val purchaseId: String? = null, @SerialName("phone_number") val phoneNumber: String? = null, @SerialName("amount_charged") val amountCharged: Double? = null, val state: String? = null, val error: String? = null)
@Serializable data class TelnyxSendSmsRes(val ok: Boolean = false, @SerialName("message_id") val messageId: String? = null, val charged: Double? = null, val error: String? = null)
@Serializable data class TelnyxCallLogItem(val id: String = "", val amount: Double = 0.0, val currency: String = "USDT", val status: String = "", val description: String = "", @SerialName("created_at") val createdAt: String = "")
@Serializable data class TelnyxMessageItem(val id: String = "", val direction: String = "in", val counterparty: String = "", val text: String = "", val status: String = "", val amount: Double = 0.0, @SerialName("created_at") val createdAt: String = "")
@Serializable data class TelnyxActivityTx(val id: String = "", @SerialName("transaction_type") val transactionType: String = "", val direction: String = "", val amount: Double = 0.0, @SerialName("balance_after") val balanceAfter: Double? = null, @SerialName("reference_type") val referenceType: String? = null, val description: String = "", @SerialName("created_at") val createdAt: String = "")
@Serializable data class TelnyxUserActivity(@SerialName("recent_calls") val recentCalls: List<TelnyxCallLogItem> = emptyList(), val messages: List<TelnyxMessageItem> = emptyList(), val transactions: List<TelnyxActivityTx> = emptyList())
@Serializable data class TelnyxAdminPurchase(val id: String = "", @SerialName("user_id") val userId: String = "", val email: String? = null, val amount: Double = 0.0, val state: String = "", @SerialName("phone_number") val phoneNumber: String? = null, @SerialName("failure_detail") val failureDetail: String? = null)
@Serializable data class TelnyxAdminBilling(val id: String = "", @SerialName("user_id") val userId: String = "", val email: String? = null, @SerialName("billing_reference") val billingReference: String = "", val amount: Double = 0.0, val currency: String = "USDT", val status: String = "")
@Serializable data class TelnyxAdminOverview(val numbers: List<TelnyxNumberItem> = emptyList(), val purchases: List<TelnyxAdminPurchase> = emptyList(), @SerialName("billing_records") val billingRecords: List<TelnyxAdminBilling> = emptyList())

object TelnyxClient {
    private val sb = SupabaseClientProvider.client
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }
    suspend fun getOffer(): TelnyxOffer {
        val raw = sb.postgrest.rpc("get_telnyx_offer").data.trim()
        return if (raw.startsWith("[")) json.decodeFromString<List<TelnyxOffer>>(raw).firstOrNull() ?: TelnyxOffer()
        else json.decodeFromString<TelnyxOffer>(raw)
    }
    suspend fun searchNumbers(cc: String, limit: Int = 20, areaCode: String = "", contains: String = "", numberType: String = ""): TelnyxSearchRes =
        sb.functions.invoke("telnyx-search-numbers", buildJsonObject {
            put("country_code", cc)
            put("limit", limit)
            if (areaCode.isNotBlank()) put("area_code", areaCode)
            if (contains.isNotBlank()) put("contains", contains)
            if (numberType.isNotBlank()) put("number_type", numberType)
        }).body()
    suspend fun orderNumber(cc: String, phone: String?, key: String): TelnyxOrderResult =
        sb.functions.invoke("telnyx-order", buildJsonObject {
            put("p_country_code", cc)
            if (!phone.isNullOrBlank()) put("p_phone_number", phone)
            put("p_idempotency_key", key)
        }).body()
    suspend fun sendSms(from: String, to: String, text: String, key: String): TelnyxSendSmsRes =
        sb.functions.invoke("telnyx-send-sms", buildJsonObject {
            put("from_number", from); put("to_number", to); put("text", text); put("idempotency_key", key)
        }).body()
    suspend fun getUserActivity(): TelnyxUserActivity {
        val raw = sb.postgrest.rpc("get_user_telnyx_activity").data.trim()
        return if (raw.startsWith("[")) json.decodeFromString<List<TelnyxUserActivity>>(raw).firstOrNull() ?: TelnyxUserActivity()
        else json.decodeFromString<TelnyxUserActivity>(raw)
    }
    suspend fun adminUpdate(en: Boolean, pr: Double, rate: Double, markup: Double) {
        sb.postgrest.rpcRaw("admin_update_telnyx_config", buildJsonObject {
            put("p_enabled", en); put("p_monthly_price", pr); put("p_voice_rate_per_min", rate); put("p_markup_percent", markup)
        })
    }
    suspend fun adminOverview(): TelnyxAdminOverview = json.decodeFromString(sb.postgrest.rpc("admin_list_telnyx_overview").data)
}

fun countryBadgeCode(cc: String): String = when (cc.uppercase()) {
    "US" -> "US"; "CA" -> "CA"; "GB", "UK" -> "UK"; "AU" -> "AU"; else -> cc.take(2).uppercase()
}

fun countryArabicTitle(cc: String): String = when (cc.uppercase()) {
    "US" -> "الولايات المتحدة"; "CA" -> "كندا"; "GB", "UK" -> "المملكة المتحدة"; "AU" -> "أستراليا"
    "NL" -> "هولندا"; "SE" -> "السويد"; "FI" -> "فنلندا"; "PL" -> "بولندا"; "BE" -> "بلجيكا"
    "ES" -> "إسبانيا"; "MX" -> "المكسيك"; "BR" -> "البرازيل"; "ZA" -> "جنوب أفريقيا"; else -> cc
}

@Composable
fun TelnyxAddNumberDialog(onDismiss: () -> Unit, onPurchased: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var filter by remember { mutableStateOf("US") }
    var areaCode by remember { mutableStateOf("") }
    var contains by remember { mutableStateOf("") }
    var numType by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var items by remember { mutableStateOf<List<TelnyxAvailableNumber>>(emptyList()) }
    var errMsg by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<TelnyxAvailableNumber?>(null) }
    var buying by remember { mutableStateOf(false) }
    var buyErr by remember { mutableStateOf<String?>(null) }

    fun load(code: String) {
        scope.launch {
            loading = true; errMsg = null
            runCatching { TelnyxClient.searchNumbers(code, 20, areaCode.trim(), contains.trim(), numType) }
                .onSuccess { res ->
                    items = res.numbers
                    if (res.numbers.isEmpty()) errMsg = res.unavailableCountries[code] ?: res.errors.firstOrNull()
                }
                .onFailure { errMsg = ErrorMapper.toUserMessage(it) }
            loading = false
        }
    }
    LaunchedEffect(filter, numType) { load(filter) }
    selected?.let { item ->
        AlertDialog(
            onDismissRequest = { if (!buying) { selected = null; buyErr = null } },
            title = { Text(tr("تأكيد شراء الرقم السحابي"), fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(ltr(item.phoneNumber), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Text("${tr(countryArabicTitle(item.countryCode))} ${if (item.region.isNotBlank()) "• ${item.region}" else ""}", style = MaterialTheme.typography.bodySmall)
                    Text(trf("الاشتراك الشهري: {0} USDT", formatAmount(item.customerPrice)), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(tr("يشمل استقبال وإرسال SMS ومكالمات صوتية دولية فورية."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    buyErr?.let { Text(it, color = dangerColor(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold) }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (buying) return@Button
                    buying = true; buyErr = null
                    scope.launch {
                        runCatching { TelnyxClient.orderNumber(item.countryCode, item.phoneNumber, "telnyx-${UUID.randomUUID()}") }
                            .onSuccess { r -> if (r.ok && !r.phoneNumber.isNullOrBlank()) { selected = null; buying = false; onPurchased(r.phoneNumber) } else { buyErr = r.error ?: tr("تعذّر إتمام الشراء حالياً"); buying = false } }
                            .onFailure { buyErr = ErrorMapper.toUserMessage(it); buying = false }
                    }
                }, enabled = !buying) { Text(if (buying) tr("جارٍ التفعيل…") else tr("شراء وتفعيل الآن"), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { if (!buying) { selected = null; buyErr = null } }) { Text(tr("إلغاء")) } }
        )
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                    IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("رجوع")) }
                    Text(tr("إضافة رقم جديد"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    TextButton(onClick = { load(filter) }) { Text(tr("تحديث"), fontWeight = FontWeight.Bold) }
                }
                val states = listOf("CH" to "🇨🇭 سويسرا (مجاني)", "" to "🇺🇸 كل الولايات", "212" to "🗽 نيويورك 212", "305" to "🌴 ميامي 305", "310" to "🎬 لوس أنجلوس 310", "713" to "🤠 تكساس 713", "312" to "🏙️ شيكاغو 312", "202" to "🏛️ واشنطن 202", "702" to "🎰 لاس فيغاس 702", "404" to "🍑 أتلانتا 404")
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    states.forEach { (stCode, label) ->
                        val active = if (stCode == "CH") filter == "CH" else (areaCode == stCode && filter == "US")
                        Surface(onClick = { if (stCode == "CH") { filter = "CH"; areaCode = ""; load("CH") } else { filter = "US"; areaCode = stCode; load("US") } }, shape = RoundedCornerShape(12.dp), color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)) {
                            Text(label, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (active) NumberOneColors.Obsidian else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = areaCode, onValueChange = { areaCode = normalizeDigits(it).filter { c -> c.isDigit() }.take(4) }, placeholder = { Text("212 / 305") }, label = { Text(tr("رمز المنطقة")) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = contains, onValueChange = { contains = normalizeDigits(it).filter { c -> c.isDigit() }.take(8) }, placeholder = { Text("777 / 2026") }, label = { Text(tr("رقم مميز")) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    Button(onClick = { load(filter) }, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)) { Text(tr("بحث"), fontWeight = FontWeight.Bold) }
                }
                if (filter == "US" || filter == "CA") {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("" to tr("الكل"), "local" to tr("محلي Local"), "toll_free" to tr("رقم موحّد (800/888)")).forEach { (tCode, tLabel) ->
                            FilterChip(selected = numType == tCode, onClick = { numType = tCode }, label = { Text(tLabel) })
                        }
                    }
                }
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (loading) { repeat(4) { SkeletonBlock(height = 84.dp) } }
                    else if (errMsg != null && items.isEmpty()) { NOCard { Text(errMsg!!, color = dangerColor(), style = MaterialTheme.typography.bodySmall); TextButton(onClick = { load(filter) }) { Text(tr("إعادة المحاولة")) } } }
                    else if (items.isEmpty()) { NOCard { Text(tr("لا توجد أرقام مطابقة لبحثك حالياً، جرّب رمز منطقة آخر."), style = MaterialTheme.typography.bodySmall) } }
                    else {
                        items.groupBy { it.countryCode.uppercase() }.forEach { (cc, list) ->
                            Text(tr(countryArabicTitle(cc)).uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    list.forEachIndexed { idx, num ->
                                        Surface(onClick = { selected = num; buyErr = null }, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
                                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), modifier = Modifier.size(40.dp)) {
                                                    Box(contentAlignment = Alignment.Center) { Text(countryBadgeCode(num.countryCode), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary) }
                                                }
                                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                    Text(ltr(num.phoneNumber), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                                                    val loc = listOfNotNull(num.region.takeIf { it.isNotBlank() }, tr(countryArabicTitle(num.countryCode))).joinToString(" • ")
                                                    Text(loc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) { Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp)); Text(tr("مكالمات"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) { Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp)); Text(tr("SMS"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                                                    }
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(ltr("${formatAmount(num.customerPrice)} USDT"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                                    Text(tr("/شهرياً"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                        if (idx < list.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TelnyxAdminCard() {
    val scope = rememberCoroutineScope()
    var en by remember { mutableStateOf(false) }
    var pr by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var markup by remember { mutableStateOf("100") }
    var ov by remember { mutableStateOf<TelnyxAdminOverview?>(null) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    var err by remember { mutableStateOf(false) }
    fun loadAll() { scope.launch { runCatching { TelnyxClient.getOffer() }.onSuccess { en = it.enabled; pr = if (it.monthlyPrice > 0.0) formatAmount(it.monthlyPrice) else "10"; rate = formatAmount(it.voiceRatePerMin); markup = formatAmount(it.markupPercent) }.onFailure { msg = ErrorMapper.toUserMessage(it); err = true }; runCatching { TelnyxClient.adminOverview() }.onSuccess { ov = it } } }
    LaunchedEffect(Unit) { loadAll() }
    NOCard {
        Text(tr("إدارة أرقام الأعمال السحابية (Telnyx)"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) { Text(tr("تفعيل البيع للعملاء"), Modifier.weight(1f)); Switch(checked = en, onCheckedChange = { en = it }) }
        OutlinedTextField(value = markup, onValueChange = { markup = normalizeDigits(it).replace('،', '.').replace(',', '.'); msg = null }, label = { Text(tr("نسبة الربح المئوية على أسعار المزود (%)")) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = rate, onValueChange = { rate = normalizeDigits(it).replace('،', '.').replace(',', '.'); msg = null }, label = { Text(tr("سعر الدقيقة الصوتية (USDT)")) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = pr, onValueChange = { pr = normalizeDigits(it).replace('،', '.').replace(',', '.'); msg = null }, label = { Text(tr("السعر الاحتياطي للرقم (USDT)")) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            val v = pr.trim().toDoubleOrNull() ?: 10.0; val r = rate.trim().toDoubleOrNull(); val m = markup.trim().toDoubleOrNull()
            if (!v.isFinite() || v < 0.0 || r == null || !r.isFinite() || r < 0.0 || m == null || !m.isFinite() || m < 0.0) { msg = tr("أدخل قيماً صحيحة"); err = true; return@Button }
            busy = true; msg = null
            scope.launch { runCatching { TelnyxClient.adminUpdate(en, v, r, m) }.onSuccess { msg = tr("تم الحفظ بنجاح"); err = false; loadAll() }.onFailure { msg = ErrorMapper.toUserMessage(it); err = true }; busy = false }
        }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(tr("حفظ إعدادات Telnyx"), fontWeight = FontWeight.Bold) }
        msg?.let { Text(it, color = if (err) dangerColor() else successColor(), style = MaterialTheme.typography.bodySmall) }
        ov?.let { o ->
            HorizontalDivider()
            val bad = o.purchases.count { it.state == "uncertain" || it.state == "failed" }
            Text(trf("الأرقام: {0} • المشتريات: {1} • الفواتير: {2}", o.numbers.size, o.purchases.size, o.billingRecords.size), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            if (bad > 0) Text(trf("تنبيه: {0} عملية بحالة uncertain/failed!", bad), color = dangerColor(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            o.billingRecords.take(5).forEach { b -> Text(ltr("${b.billingReference} • ${b.email ?: b.userId.take(8)} • ${formatAmount(b.amount)} ${b.currency} (${b.status})"), style = MaterialTheme.typography.labelSmall) }
        }
    }
}
