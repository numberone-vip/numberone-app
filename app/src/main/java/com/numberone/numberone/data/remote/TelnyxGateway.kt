package com.numberone.numberone.data.remote
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.numberone.numberone.core.components.*
import com.numberone.numberone.core.error.ErrorMapper
import com.numberone.numberone.core.localization.*
import com.numberone.numberone.presentation.util.normalizeDigits
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.call.body
import kotlinx.coroutines.launch
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.util.UUID

@Serializable data class TelnyxNumberItem(val id: String = "", @SerialName("user_id") val userId: String = "", val email: String? = null, @SerialName("phone_number") val phoneNumber: String = "", val status: String = "", @SerialName("monthly_price") val monthlyPrice: Double = 0.0, @SerialName("expires_at") val expiresAt: String? = null, @SerialName("subscription_id") val subscriptionId: String? = null)
@Serializable data class TelnyxOffer(val enabled: Boolean = false, @SerialName("monthly_price") val monthlyPrice: Double = 0.0, val numbers: List<TelnyxNumberItem> = emptyList())
@Serializable data class TelnyxOrderResult(val ok: Boolean = false, @SerialName("purchase_id") val purchaseId: String? = null, @SerialName("phone_number") val phoneNumber: String? = null, val state: String? = null, val error: String? = null)
@Serializable data class TelnyxAdminPurchase(val id: String = "", @SerialName("user_id") val userId: String = "", val email: String? = null, val amount: Double = 0.0, val state: String = "", @SerialName("phone_number") val phoneNumber: String? = null, @SerialName("failure_detail") val failureDetail: String? = null)
@Serializable data class TelnyxAdminBilling(val id: String = "", @SerialName("user_id") val userId: String = "", val email: String? = null, @SerialName("billing_reference") val billingReference: String = "", val amount: Double = 0.0, val currency: String = "USDT", val status: String = "")
@Serializable data class TelnyxAdminOverview(val numbers: List<TelnyxNumberItem> = emptyList(), val purchases: List<TelnyxAdminPurchase> = emptyList(), @SerialName("billing_records") val billingRecords: List<TelnyxAdminBilling> = emptyList())

object TelnyxClient {
    private val sb = SupabaseClientProvider.client
    suspend fun getOffer(): TelnyxOffer = sb.postgrest.rpcDecode("get_telnyx_offer", buildJsonObject {})
    suspend fun orderNumber(cc: String, key: String): TelnyxOrderResult = sb.functions.invoke("telnyx-order", buildJsonObject { put("p_country_code", cc); put("p_idempotency_key", key) }).body()
    suspend fun adminUpdate(en: Boolean, pr: Double) { sb.postgrest.rpcRaw("admin_update_telnyx_config", buildJsonObject { put("p_enabled", en); put("p_monthly_price", pr) }) }
    suspend fun adminOverview(): TelnyxAdminOverview = sb.postgrest.rpcDecode("admin_list_telnyx_overview", buildJsonObject {})
}

@Composable fun TelnyxCustomerCard(onOpenSms: () -> Unit, onPurchased: () -> Unit) {
    val scope = rememberCoroutineScope()
    var offer by remember { mutableStateOf<TelnyxOffer?>(null) }
    var cc by remember { mutableStateOf("US") }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    var err by remember { mutableStateOf(false) }
    fun reload() { scope.launch { runCatching { TelnyxClient.getOffer() }.onSuccess { offer = it } } }
    LaunchedEffect(Unit) { reload() }
    val cur = offer ?: return
    if (!cur.enabled && cur.numbers.isEmpty()) return
    NOCard {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(tr("أرقام الأعمال السحابية • Telnyx"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(tr("خطوط أعمال فورية التفعيل مع استقبال SMS مباشر"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (cur.enabled && cur.monthlyPrice > 0.0) Text(ltr("${formatAmount(cur.monthlyPrice)} USDT"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        }
        if (cur.enabled && cur.monthlyPrice > 0.0) {
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { listOf("US" to "🇺🇸 أمريكا +1", "GB" to "🇬🇧 بريطانيا +44", "CA" to "🇨🇦 كندا +1", "DE" to "🇩🇪 ألمانيا +49", "FR" to "🇫🇷 فرنسا +33", "NL" to "🇳🇱 هولندا +31", "SE" to "🇸🇪 السويد +46", "AU" to "🇦🇺 أستراليا +61").forEach { (c, l) -> FilterChip(selected = cc == c, onClick = { cc = c }, label = { Text(l) }) } }
            Button(onClick = {
                if (busy) return@Button; busy = true; msg = null
                scope.launch {
                    runCatching { TelnyxClient.orderNumber(cc, "telnyx-${UUID.randomUUID()}") }
                        .onSuccess { r -> if (r.ok && !r.phoneNumber.isNullOrBlank()) { msg = trf("تم تفعيل رقمك بنجاح: {0}", ltr(r.phoneNumber)); err = false; reload(); onPurchased() } else { msg = r.error ?: tr("تعذّر إتمام الطلب حالياً"); err = true } }
                        .onFailure { msg = ErrorMapper.toUserMessage(it); err = true }
                    busy = false
                }
            }, enabled = !busy, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) { Text(if (busy) tr("جارٍ التفعيل…") else trf("شراء فوري ({0} USDT / شهر)", ltr(formatAmount(cur.monthlyPrice))), fontWeight = FontWeight.Bold) }
        }
        msg?.let { Text(it, color = if (err) dangerColor() else successColor(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold) }
        if (cur.numbers.isNotEmpty()) {
            HorizontalDivider()
            cur.numbers.forEach { n ->
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(ltr(n.phoneNumber), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            n.expiresAt?.let { Text(tr("ينتهي: ") + ltr(it.take(10)), style = MaterialTheme.typography.labelSmall) }
                        }
                        OutlinedButton(onClick = onOpenSms) { Text(tr("صندوق SMS")) }
                    }
                }
            }
        }
    }
}

@Composable fun TelnyxAdminCard() {
    val scope = rememberCoroutineScope()
    var en by remember { mutableStateOf(false) }
    var pr by remember { mutableStateOf("") }
    var ov by remember { mutableStateOf<TelnyxAdminOverview?>(null) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    var err by remember { mutableStateOf(false) }
    fun loadAll() { scope.launch { runCatching { TelnyxClient.getOffer() }.onSuccess { en = it.enabled; pr = if (it.monthlyPrice > 0.0) formatAmount(it.monthlyPrice) else "" }; runCatching { TelnyxClient.adminOverview() }.onSuccess { ov = it } } }
    LaunchedEffect(Unit) { loadAll() }
    NOCard {
        Text(tr("إدارة أرقام الأعمال السحابية (Telnyx)"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) { Text(tr("تفعيل البيع للعملاء"), Modifier.weight(1f)); Switch(checked = en, onCheckedChange = { en = it }) }
        OutlinedTextField(value = pr, onValueChange = { pr = normalizeDigits(it).replace('،', '.').replace(',', '.'); msg = null }, label = { Text(tr("السعر الشهري (USDT)")) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            val v = pr.trim().toDoubleOrNull(); if (v == null || !v.isFinite() || v < 0.0) { msg = tr("أدخل سعراً صحيحاً"); err = true; return@Button }
            busy = true; msg = null
            scope.launch { runCatching { TelnyxClient.adminUpdate(en, v) }.onSuccess { msg = tr("تم الحفظ بنجاح"); err = false; loadAll() }.onFailure { msg = ErrorMapper.toUserMessage(it); err = true }; busy = false }
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
