package com.numberone.app.data.repository

import com.numberone.app.data.remote.SupabaseClientProvider
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class WalletSummaryDto(@SerialName("balance") val balance: Double, @SerialName("currency") val currency: String)

@Serializable
data class WalletOrderDto(
    @SerialName("order_id") val orderId: String,
    @SerialName("service_name") val serviceName: String,
    @SerialName("price") val price: Double,
    @SerialName("balance_after") val balanceAfter: Double,
    @SerialName("status") val status: String,
    @SerialName("payment_status") val paymentStatus: String
)

@Serializable
data class WalletPaymentSettingsDto(
    val currency: String,
    val network: String,
    @SerialName("wallet_address") val walletAddress: String? = null,
    @SerialName("is_enabled") val isEnabled: Boolean = false
)

data class DepositVerificationResult(val credited: Boolean, val message: String)

class WalletRepository {
    private val client = SupabaseClientProvider.client

    suspend fun getMyWalletSummary(): Result<WalletSummaryDto> = withContext(Dispatchers.IO) {
        runCatching {
            requireSignedIn()
            client.postgrest.rpc(function = "wallet_get_my_summary").decodeSingle<WalletSummaryDto>()
        }
    }

    suspend fun getDepositPaymentSettings(): Result<WalletPaymentSettingsDto?> = withContext(Dispatchers.IO) {
        runCatching {
            requireSignedIn()
            client.postgrest["payment_settings"].select().decodeList<WalletPaymentSettingsDto>().firstOrNull()
        }
    }

    suspend fun createDepositRequest(amount: Double): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            requireSignedIn()
            require(amount > 0.0 && amount <= 100000.0) { "أدخل مبلغاً صحيحاً." }
            val params = buildJsonObject { put("p_amount", amount) }
            client.postgrest.rpc(function = "wallet_create_deposit_request", parameters = params).decodeSingle<String>()
        }
    }

    suspend fun submitDepositTransaction(requestId: String, transactionHash: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireSignedIn()
            require(transactionHash.matches(Regex("^[0-9a-fA-F]{64}$"))) { "معرّف المعاملة يجب أن يكون 64 خانة hex." }
            val params = buildJsonObject {
                put("p_request_id", requestId)
                put("p_transaction_hash", transactionHash.lowercase())
            }
            client.postgrest.rpc(function = "wallet_submit_deposit_tx", parameters = params).decodeSingle<Boolean>()
            Unit
        }
    }

    suspend fun verifyDeposit(requestId: String, transactionHash: String): Result<DepositVerificationResult> = withContext(Dispatchers.IO) {
        runCatching {
            requireSignedIn()
            require(transactionHash.matches(Regex("^[0-9a-fA-F]{64}$"))) { "معرّف المعاملة غير صحيح." }
            val token = client.auth.currentSessionOrNull()?.accessToken ?: error("انتهت جلسة الدخول. سجّل الدخول مجدداً.")
            val endpoint = URL(SupabaseClientProvider.SUPABASE_URL + "/functions/v1/verify-binance-deposit")
            val connection = endpoint.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer " + token)
            connection.setRequestProperty("apikey", SupabaseClientProvider.SUPABASE_ANON_KEY)
            try {
                val body = "{\"request_id\":\"" + requestId + "\",\"transaction_hash\":\"" + transactionHash.lowercase() + "\"}"
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = Json.parseToJsonElement(response).jsonObject
                if (code !in 200..299) error(json["error"]?.jsonPrimitive?.contentOrNull ?: "تعذر التحقق من الإيداع.")
                val status = json["status"]?.jsonPrimitive?.contentOrNull
                val message = json["message"]?.jsonPrimitive?.contentOrNull
                when (status) {
                    "credited", "already_credited" -> DepositVerificationResult(true, "تم تأكيد الإيداع وإضافة الرصيد إلى محفظتك.")
                    "not_verified" -> DepositVerificationResult(false, message ?: "لم يتم العثور على إيداع مطابق بعد. حاول مرة أخرى لاحقاً.")
                    else -> DepositVerificationResult(false, message ?: "الإيداع قيد التحقق.")
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun purchaseFromWallet(serviceId: String, requestId: String): Result<WalletOrderDto> = withContext(Dispatchers.IO) {
        runCatching {
            requireSignedIn()
            require(serviceId.isNotBlank()) { "اختر الخدمة أولاً." }
            require(requestId.isNotBlank()) { "معرّف عملية الشراء مفقود." }
            val parameters = buildJsonObject {
                put("p_service_id", serviceId)
                put("p_request_id", requestId)
            }
            client.postgrest.rpc(function = "create_wallet_order", parameters = parameters).decodeSingle<WalletOrderDto>()
        }
    }

    private fun requireSignedIn() {
        check(client.auth.currentSessionOrNull()?.user?.id != null) { "يجب تسجيل الدخول قبل استخدام المحفظة." }
    }
}
