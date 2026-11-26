package com.numberone.numberone.data.remote

import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.domain.model.CreateWalletTopUp
import com.numberone.numberone.domain.model.Wallet
import com.numberone.numberone.domain.model.WalletTopUp
import com.numberone.numberone.domain.model.WalletTransaction
import com.numberone.numberone.domain.model.Service
import com.numberone.numberone.domain.model.WalletOrderResult
import com.numberone.numberone.domain.model.PaymentOrderCreation
import com.numberone.numberone.domain.model.Order as OrderModel
import com.numberone.numberone.domain.model.ShamCashTopUp
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import io.ktor.client.call.body
import io.ktor.http.ContentType
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseWalletDataSource : WalletDataSource {
    private val supabase = SupabaseClientProvider.client

    @Serializable private data class VerifyResponse(
        @SerialName("status") val status: String? = null,
        @SerialName("message") val message: String? = null,
        @SerialName("error") val error: String? = null
    )

    override suspend fun getWallet(userId: String): Wallet {
        val result = supabase.postgrest.rpcDecode<List<Wallet>>("wallet_get_my_summary")
        return result.firstOrNull() ?: Wallet(userId = userId)
    }

    override suspend fun getTransactions(userId: String): List<WalletTransaction> =
        supabase.postgrest.rpcDecode("wallet_get_my_transactions", buildJsonObject { put("p_limit", 50) })

    override suspend fun createTopUpRequest(userId: String, request: CreateWalletTopUp): WalletTopUp {
        // v2 returns the unique amount the user must send (e.g. 15.0437), used for automatic matching.
        val created = supabase.postgrest.rpcDecode<List<CreateDepositResult>>(
            "wallet_create_deposit_request_v2",
            buildJsonObject { put("p_amount", request.amount) }
        ).first()
        return getMyTopUpRequests(userId).firstOrNull { it.id == created.requestId }
            ?: WalletTopUp(id = created.requestId, requestedAmount = created.amountToSend)
    }

    override suspend fun getMyTopUpRequests(userId: String): List<WalletTopUp> =
        supabase.postgrest.rpcDecode("wallet_get_my_deposit_requests", buildJsonObject { put("p_limit", 30) })

    override suspend fun submitDepositTransaction(requestId: String, transactionHash: String): Boolean =
        supabase.postgrest.rpcDecode("wallet_submit_deposit_tx", buildJsonObject { put("p_request_id", requestId); put("p_transaction_hash", transactionHash) })

    override suspend fun verifyDeposit(requestId: String, transactionHash: String): String {
        val response = supabase.functions.invoke(
            function = "verify-binance-deposit",
            body = buildJsonObject {
                put("request_id", requestId)
                put("transaction_hash", transactionHash)
            }
        )
        val result = response.body<VerifyResponse>()
        return result.error ?: result.message ?: result.status ?: tr("تم إرسال طلب التحقق")
    }

    override suspend fun getServices(): List<Service> {
        val services = rpcJson.decodeFromString<List<Service>>(
            supabase.postgrest.from("services").select().data
        )
        // A catalog product can have several purchasable price rows. Group only identical
        // product/country combinations and retain every original row (especially its UUID).
        return services.groupBy {
            listOf(it.name.trim().lowercase(), it.country.trim().lowercase(), it.countryCode.trim().lowercase())
        }.values.map { variants ->
            val sorted = variants.sortedWith(compareBy<Service> { it.price }.thenBy { it.id })
            sorted.first().copy(options = sorted)
        }
    }

    override suspend fun getMyOrders(userId: String): List<OrderModel> =
        supabase.postgrest.from("orders").select {
            filter { eq("user_id", userId) }
            order(column = "created_at", order = Order.DESCENDING)
        }.data.let { rpcJson.decodeFromString<List<OrderModel>>(it) }

    override suspend fun createOrder(serviceId: String): PaymentOrderCreation =
        supabase.postgrest.rpcDecode<List<PaymentOrderCreation>>("create_order", buildJsonObject { put("p_service_id", serviceId) }).single()

    override suspend fun uploadReceipt(orderId: String, bytes: ByteArray, mimeType: String, extension: String): String {
        require(bytes.isNotEmpty() && bytes.size <= 5 * 1024 * 1024) { tr("إيصال الدفع يجب ألا يتجاوز 5 ميغابايت") }
        require(mimeType in setOf("image/jpeg", "image/png", "image/webp")) { tr("صيغة الإيصال غير مدعومة") }
        require(matchesImageSignature(bytes, mimeType)) { tr("ملف الإيصال ليس صورة صالحة") }
        val userId = supabase.auth.currentUserOrNull()?.id ?: error(tr("يرجى تسجيل الدخول"))
        // Extension is derived from the verified MIME type, never from caller input.
        val ext = when (mimeType) { "image/png" -> "png"; "image/webp" -> "webp"; else -> "jpg" }
        val path = "$userId/$orderId/${java.util.UUID.randomUUID()}.$ext"
        val type = when (mimeType) { "image/png" -> ContentType.Image.PNG; "image/webp" -> ContentType.parse("image/webp"); else -> ContentType.Image.JPEG }
        supabase.storage["payment-receipts"].upload(path, bytes) { contentType = type }
        return path
    }

    private fun matchesImageSignature(b: ByteArray, mime: String): Boolean = when (mime) {
        "image/jpeg" -> b.size > 3 && b[0] == 0xFF.toByte() && b[1] == 0xD8.toByte() && b[2] == 0xFF.toByte()
        "image/png" -> b.size > 8 && b[0] == 0x89.toByte() && b[1] == 0x50.toByte() && b[2] == 0x4E.toByte() && b[3] == 0x47.toByte()
        "image/webp" -> b.size > 12 && String(b, 0, 4, Charsets.ISO_8859_1) == "RIFF" && String(b, 8, 4, Charsets.ISO_8859_1) == "WEBP"
        else -> false
    }

    override suspend fun submitOrderPayment(orderId: String, transactionHash: String, receiptPath: String): Boolean =
        supabase.postgrest.rpcDecode("submit_order_payment", buildJsonObject { put("p_order_id", orderId); put("p_transaction_ref", transactionHash); put("p_receipt_path", receiptPath) })

    override suspend fun purchaseServiceWithWallet(serviceId: String, requestId: String): WalletOrderResult =
        supabase.postgrest.rpcDecode<List<WalletOrderResult>>("create_wallet_order", buildJsonObject { put("p_service_id", serviceId); put("p_request_id", requestId) }).single()


    override suspend fun createShamCashTopUp(paymentMethod: String, paidAmount: Double): String =
        supabase.postgrest.rpcDecode<String>(
            "wallet_create_sham_cash_topup_request",
            buildJsonObject { put("p_payment_method", paymentMethod); put("p_paid_amount", paidAmount) }
        )

    override suspend fun submitShamCashReceipt(requestId: String, bytes: ByteArray, mimeType: String, extension: String): Boolean {
        // Path convention enforced by storage RLS: {user_id}/{request_id}/<file>
        val path = uploadReceipt(requestId, bytes, mimeType, extension)
        return supabase.postgrest.rpcDecode<Boolean>(
            "wallet_submit_sham_cash_topup_receipt",
            buildJsonObject { put("p_request_id", requestId); put("p_receipt_path", path) }
        )
    }

    suspend fun getDepositWalletAddress(): String? = runCatching {
        @Serializable data class Row(@SerialName("wallet_address") val walletAddress: String? = null, @SerialName("is_enabled") val isEnabled: Boolean = true)
        val rows = rpcJson.decodeFromString<List<Row>>(supabase.postgrest.from("payment_settings").select().data)
        rows.firstOrNull { it.isEnabled }?.walletAddress?.trim()?.takeIf { it.isNotBlank() }
    }.getOrNull()

    override suspend fun getMyShamCashTopUps(): List<ShamCashTopUp> =
        supabase.postgrest.rpcDecode("wallet_get_my_sham_cash_topups", buildJsonObject { put("p_limit", 50) })
}


@Serializable
private data class CreateDepositResult(
    @SerialName("request_id") val requestId: String,
    @SerialName("amount_to_send") val amountToSend: Double
)
