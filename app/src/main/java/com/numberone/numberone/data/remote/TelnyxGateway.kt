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
@Serializable data class TelnyxSearchRes(val ok: Boolean = false, @SerialName("markup_percent") val markupPercent: Double = 100.0, val numbers: List<TelnyxAvailableNumber> = emptyList(), val errors: List<String> = emptyList())
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
    suspend fun searchNumbers(cc: String, limit: Int = 5): TelnyxSearchRes =
        sb.functions.invoke("telnyx-search-numbers", buildJsonObject { put("country_code", cc); put("limit", limit) }).body()
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
    "US" -> "الولايات المتحدة"; "CA" -> "كندا"; "GB", "UK" -> "المملكة المتحدة"; "AU" -> "أستراليا"; else -> cc
}
