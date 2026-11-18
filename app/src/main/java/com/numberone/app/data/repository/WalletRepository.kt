package com.numberone.app.data.repository

import com.numberone.app.data.remote.SupabaseClientProvider
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class WalletSummaryDto(
    @SerialName("balance") val balance: Double,
    @SerialName("currency") val currency: String
)

@Serializable
data class WalletOrderDto(
    @SerialName("order_id") val orderId: String,
    @SerialName("service_name") val serviceName: String,
    @SerialName("price") val price: Double,
    @SerialName("balance_after") val balanceAfter: Double,
    @SerialName("status") val status: String,
    @SerialName("payment_status") val paymentStatus: String
)

class WalletRepository {
    private val client = SupabaseClientProvider.client

    suspend fun getMyWalletSummary(): Result<WalletSummaryDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireSignedIn()
                client.postgrest.rpc("wallet_get_my_summary")
                    .decodeSingle<WalletSummaryDto>()
            }
        }

    /**
     * requestId must be generated once per user tap and reused for network retries
     * of that same purchase attempt. Generate a new UUID for a genuinely new purchase.
     */
    suspend fun purchaseFromWallet(
        serviceId: String,
        requestId: String
    ): Result<WalletOrderDto> = withContext(Dispatchers.IO) {
        runCatching {
            requireSignedIn()
            require(serviceId.isNotBlank()) { "اختر الخدمة أولاً." }
            require(requestId.isNotBlank()) { "معرّف عملية الشراء مفقود." }

            val parameters = buildJsonObject {
                put("p_service_id", serviceId)
                put("p_request_id", requestId)
            }

            client.postgrest.rpc(
                function = "create_wallet_order",
                parameters = parameters
            ).decodeSingle<WalletOrderDto>()
        }
    }

    private fun requireSignedIn() {
        check(client.auth.currentSessionOrNull()?.user?.id != null) {
            "يجب تسجيل الدخول قبل استخدام المحفظة."
        }
    }
}
