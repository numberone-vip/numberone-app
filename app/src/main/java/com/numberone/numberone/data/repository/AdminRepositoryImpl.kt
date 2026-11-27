package com.numberone.numberone.data.repository

import com.numberone.numberone.core.error.AppError
import com.numberone.numberone.core.error.AppException
import com.numberone.numberone.core.log.AppLog
import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.data.remote.AdminRpc
import com.numberone.numberone.data.remote.SupabaseAdminRpc
import com.numberone.numberone.data.remote.SupabaseClientProvider
import com.numberone.numberone.data.remote.rpcJson
import com.numberone.numberone.data.remote.rpcRaw
import com.numberone.numberone.domain.model.*
import com.numberone.numberone.domain.repository.AdminRepository
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.serializer
import kotlin.time.Duration.Companion.minutes

@Serializable
private data class UserIdRow(@SerialName("user_id") val userId: String = "")

/**
 * Admin RPC access.
 *
 * Every list RPC is decoded through [rows], and every scalar/boolean result through
 * [numberResult] / [booleanResult]. These throw an exception whose message names the RPC and
 * the decoding problem, so the admin panel can show the real cause instead of a generic error.
 */
class AdminRepositoryImpl(
    private val rpc: AdminRpc = SupabaseAdminRpc()
) : AdminRepository {
    private companion object { const val TAG = "AdminRepo" }
    private val dateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    private val bscRegex = Regex("^0x[0-9a-fA-F]{40}$")
    private val phoneRegex = Regex("^\\+[1-9][0-9]{7,14}$")
    private val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    // ---- Decoding helpers ---------------------------------------------------

    /** Decodes a list-returning RPC. A null or blank response is an empty list. */
    private inline fun <reified T> rows(rpc: String, raw: String): List<T> {
        val text = raw.trim()
        if (text.isEmpty() || text == "null") return emptyList()
        return try {
            rpcJson.decodeFromString(ListSerializer(serializer<T>()), text)
        } catch (e: Exception) {
            AppLog.error(TAG, "admin RPC $rpc: undecodable response", e)
            throw AppException(AppError.ServerResponse, e)
        }
    }

    /**
     * Reads a numeric result (new balance, credited amount). Accepts a bare number, a one-row
     * array, or an object; for objects it prefers a key that mentions "balance".
     */
    private fun numberResult(rpc: String, raw: String): Double {
        fun pick(el: JsonElement): Double? = when (el) {
            is JsonPrimitive -> el.doubleOrNull
            is JsonArray -> el.firstNotNullOfOrNull { pick(it) }
            is JsonObject -> (el.entries.filter { "balance" in it.key }.firstNotNullOfOrNull { pick(it.value) })
                ?: el.values.firstNotNullOfOrNull { pick(it) }
            else -> null
        }
        val parsed = runCatching { rpcJson.parseToJsonElement(raw) }.getOrNull()
        return parsed?.let { pick(it) }
            ?: run {
                AppLog.error(TAG, "admin RPC $rpc: unexpected response shape")
                throw AppException(AppError.ServerResponse)
            }
    }

    /** Reads a boolean result. A successful call with no boolean body counts as true. */
    private fun booleanResult(raw: String): Boolean {
        val parsed = runCatching { rpcJson.parseToJsonElement(raw) }.getOrNull()
        return (parsed as? JsonPrimitive)?.booleanOrNull ?: true
    }

    // ---- Summary & shams ----------------------------------------------------

    override suspend fun getSummary() = runCatching {
        val deposits = rows<AdminWalletDeposit>("admin_list_wallet_deposits", rpc.rpcRaw("admin_list_wallet_deposits"))
        AdminSummary(
            customers = rows<AdminCustomer>("admin_list_customers", rpc.rpcRaw("admin_list_customers")).size,
            wallets = rows<AdminWalletBalance>("admin_list_wallet_balances", rpc.rpcRaw("admin_list_wallet_balances")).size,
            deposits = deposits.size,
            subscriptions = rows<AdminSubscription>("admin_list_subscriptions", rpc.rpcRaw("admin_list_subscriptions")).size,
            pendingDeposits = deposits.count { isOpenStatus(it.status) }
        )
    }

    override suspend fun listShamCashTopUps() = runCatching {
        rows<AdminShamCashTopUp>("admin_list_sham_cash_topups", rpc.rpcRaw("admin_list_sham_cash_topups"))
    }

    override suspend fun approveShamCashTopUp(requestId: String, creditedUsdt: Double) = runCatching {
        require(uuidRegex.matches(requestId)) { tr("معرّف غير صالح") }
        require(creditedUsdt.isFinite() && creditedUsdt > 0.0 && creditedUsdt <= 1_000_000.0) { tr("مبلغ USDT غير صالح") }
        val raw = rpc.rpcRaw(
            "admin_approve_sham_cash_topup",
            buildJsonObject { put("p_request_id", requestId); put("p_credited_usdt_amount", creditedUsdt) }
        )
        numberResult("admin_approve_sham_cash_topup", raw)
    }

    override suspend fun rejectShamCashTopUp(requestId: String, reason: String) = runCatching {
        require(uuidRegex.matches(requestId)) { tr("معرّف غير صالح") }
        require(reason.isNotBlank()) { tr("سبب الرفض مطلوب") }
        val raw = rpc.rpcRaw(
            "admin_reject_sham_cash_topup",
            buildJsonObject { put("p_request_id", requestId); put("p_reason", reason.trim()) }
        )
        booleanResult(raw)
    }

    // ---- Orders -------------------------------------------------------------

    override suspend fun listOrders() = runCatching {
        rows<Order>("admin_list_orders", rpc.rpcRaw("admin_list_orders"))
    }

    override suspend fun approveOrder(orderId: String, phoneNumber: String, expiryDate: String) = runCatching {
        require(orderId.isNotBlank() && phoneNumber.isNotBlank()) { tr("رقم الهاتف وتاريخ الانتهاء مطلوبان") }
        require(dateRegex.matches(expiryDate.trim())) { tr("صيغة التاريخ yyyy-MM-dd") }
        java.time.LocalDate.parse(expiryDate.trim())
        val raw = rpc.rpcRaw(
            "admin_approve_order",
            buildJsonObject {
                put("p_order_id", orderId.trim())
                put("p_phone_number", phoneNumber.replace(Regex("[\\s\\-()]+"), "").trim())
                put("p_expiry_date", expiryDate.trim())
            }
        )
        raw.trim().removePrefix("\"").removeSuffix("\"")
    }

    override suspend fun rejectOrder(orderId: String, reason: String) = runCatching {
        require(orderId.isNotBlank() && reason.isNotBlank()) { tr("سبب الرفض مطلوب") }
        rpc.rpcRaw("admin_reject_order", buildJsonObject { put("p_order_id", orderId); put("p_reason", reason.trim()) })
        Unit
    }

    override suspend fun getReceiptUrl(path: String) = runCatching {
        require(path.isNotBlank() && !path.contains("..")) { tr("مسار غير صالح") }
        SupabaseClientProvider.client.storage["payment-receipts"].createSignedUrl(path, 5.minutes)
    }

    // ---- Command-center additions ------------------------------------------

    override suspend fun listDeposits() = runCatching {
        rows<AdminWalletDeposit>("admin_list_wallet_deposits", rpc.rpcRaw("admin_list_wallet_deposits"))
    }

    override suspend fun listSubscriptions() = runCatching {
        rows<AdminSubscription>("admin_list_subscriptions", rpc.rpcRaw("admin_list_subscriptions"))
    }

    override suspend fun getPaymentConfig() = runCatching {
        // The RPC may return a single object or a one-row table; accept both.
        val element = rpcJson.parseToJsonElement(rpc.rpcRaw("get_payment_config"))
        val obj = when (element) {
            is JsonObject -> element
            is JsonArray -> element.firstOrNull() as? JsonObject
            else -> null
        }
        if (obj == null) AppPaymentConfig() else rpcJson.decodeFromJsonElement(AppPaymentConfig.serializer(), obj)
    }

    override suspend fun updatePaymentConfig(config: AppPaymentConfig) = runCatching {
        val wallet = config.binanceWalletAddress.trim()
        require(wallet.isEmpty() || bscRegex.matches(wallet)) { tr("عنوان محفظة BEP20 غير صالح (يبدأ بـ 0x ويتكون من 42 خانة)") }
        require(config.binanceNetwork.equals("BEP20", ignoreCase = true)) { tr("شبكة الإيداع المدعومة هي BEP20 فقط") }
        require(config.shamCashName.length <= 120) { tr("اسم صاحب حساب شام كاش طويل جداً") }
        require(config.sypUsdRate.isFinite() && config.sypUsdRate >= 0.0 && config.sypUsdRate <= 10_000_000.0) { tr("سعر صرف الليرة غير صالح") }
        require(config.promoBannerText.length <= 300) { tr("نص البانر طويل (الحد 300 حرف)") }
        // Match the live five-argument public RPC signature exactly.
        rpc.rpcRaw(
            "admin_update_payment_config",
            buildJsonObject {
                put("p_binance_wallet_address", wallet)
                put("p_sham_cash_account", config.shamCashAccount.trim())
                put("p_sham_cash_name", config.shamCashName.trim())
                put("p_syp_usd_rate", config.sypUsdRate)
                put("p_promo_banner_text", config.promoBannerText.trim())
            }
        )
        Unit
    }

    override suspend fun listServices() = runCatching {
        rows<AdminService>("admin_list_services", rpc.rpcRaw("admin_list_services"))
    }

    override suspend fun upsertService(draft: ServiceDraft) = runCatching {
        require(draft.name.isNotBlank()) { tr("اسم الخدمة مطلوب") }
        require(draft.price.isFinite() && draft.price >= 0.0 && draft.price <= 1_000_000.0) { tr("السعر غير صالح") }
        draft.id?.let { require(it.isNotBlank()) { tr("معرّف غير صالح") } }
        rpc.rpcRaw(
            "admin_upsert_service",
            buildJsonObject {
                draft.id?.let { put("p_id", it.trim()) }
                put("p_name", draft.name.trim())
                // Live RPC derives the services.country column from p_country_code.
                // It has no p_country parameter.
                put("p_country_code", draft.countryCode.trim())
                put("p_price", draft.price)
                put("p_currency", "USDT")
                put("p_description", draft.description.trim())
                put("p_is_active", draft.isActive)
            }
        )
        Unit
    }

    override suspend fun deleteService(serviceId: String) = runCatching {
        require(serviceId.isNotBlank()) { tr("معرّف غير صالح") }
        try {
            rpc.rpcRaw("admin_delete_service", buildJsonObject { put("p_id", serviceId) })
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            val m = e.message.orEmpty().lowercase()
            if ("foreign key" in m || "violates" in m || "in_use" in m) {
                throw AppException(AppError.ServiceInUse, e)
            }
            throw e
        }
        Unit
    }

    // ---- Users & wallets ----------------------------------------------------

    override suspend fun listUsers() = runCatching {
        // Uses the EXISTING admin_list_customers / admin_list_wallet_balances functions.
        // Role comes from admin_list_admin_ids(); if that is not deployed the role stays unknown (null).
        val balances = rows<AdminWalletBalance>("admin_list_wallet_balances", rpc.rpcRaw("admin_list_wallet_balances"))
            .associateBy { it.userId }
        val adminIds = runCatching {
            rows<UserIdRow>("admin_list_admin_ids", rpc.rpcRaw("admin_list_admin_ids")).map { it.userId }.toSet()
        }.getOrNull()
        rows<AdminCustomer>("admin_list_customers", rpc.rpcRaw("admin_list_customers")).map {
            val w = balances[it.userId]
            AdminUser(
                userId = it.userId,
                email = it.email,
                role = adminIds?.let { ids -> if (it.userId in ids) "Admin" else "User" },
                balance = w?.balance ?: 0.0,
                currency = w?.currency ?: "USDT",
                createdAt = it.createdAt
            )
        }
    }

    override suspend fun listWalletTransactions(limit: Int) = runCatching {
        require(limit in 1..500) { tr("عدد الصفوف غير صالح") }
        rows<WalletTransaction>(
            "admin_list_wallet_transactions",
            rpc.rpcRaw("admin_list_wallet_transactions", buildJsonObject { put("p_limit", limit) })
        )
    }

    override suspend fun adjustWalletBalance(userId: String, amount: Double, note: String, requestId: String) = runCatching {
        require(uuidRegex.matches(requestId)) { tr("معرّف العملية غير صالح") }
        require(uuidRegex.matches(userId)) { tr("معرّف غير صالح") }
        require(amount.isFinite() && amount != 0.0 && kotlin.math.abs(amount) <= 1_000_000.0) { tr("مبلغ USDT غير صالح") }
        require(note.isNotBlank()) { tr("ملاحظة التدقيق مطلوبة") }
        val raw = rpc.rpcRaw(
            "admin_adjust_wallet_balance",
            buildJsonObject { put("p_user_id", userId); put("p_amount", amount); put("p_note", note.trim()); put("p_request_id", requestId) }
        )
        numberResult("admin_adjust_wallet_balance", raw)
    }

    override suspend fun assignSubscription(
        userId: String, serviceId: String, phoneNumber: String, accessCode: String, expiryDate: String
    ) = runCatching {
        require(uuidRegex.matches(userId) && serviceId.isNotBlank()) { tr("اختر المستخدم والخدمة") }
        require(phoneNumber.isNotBlank() && accessCode.isNotBlank()) { tr("الرقم ورمز الوصول مطلوبان") }
        require(dateRegex.matches(expiryDate.trim())) { tr("صيغة التاريخ yyyy-MM-dd") }
        java.time.LocalDate.parse(expiryDate.trim())
        require(accessCode.trim().length in 8..256) { tr("رمز الوصول يجب أن يكون بين 8 و256 خانة") }
        val cleanP = phoneNumber.replace(Regex("[\\s\\-()]+"), "").trim(); require(phoneRegex.matches(cleanP)) { tr("رقم الهاتف غير صالح") }
        rpc.rpcRaw(
            "admin_add_subscription_for_user",
            buildJsonObject {
                put("p_user_id", userId); put("p_service_id", serviceId.trim())
                put("p_access_code", accessCode.trim()); put("p_phone_number", phoneNumber.replace(Regex("[\\s\\-()]+"), "").trim())
                put("p_expiry_date", expiryDate.trim())
                put("p_admin_note", "Assigned manually from admin panel")
            }
        )
        Unit
    }

    override suspend fun sendSubscriptionSms(subscriptionId: String, sender: String, body: String) = runCatching {
        require(uuidRegex.matches(subscriptionId)) { tr("معرّف غير صالح") }
        require(sender.isNotBlank() && body.isNotBlank()) { tr("المرسل ونص الرسالة مطلوبان") }
        rpc.rpcRaw("admin_send_subscription_sms", buildJsonObject { put("p_subscription_id", subscriptionId); put("p_sender", sender.trim()); put("p_body", body.trim()) })
        Unit
    }
    override suspend fun sendAdminNotification(userIds: List<String>, title: String, body: String) = runCatching {
        val t = title.trim(); val b = body.trim()
        require(t.isNotEmpty() && b.isNotEmpty()) { tr("عنوان الإشعار ونص الرسالة مطلوبان") }
        val targets = userIds.map { it.trim() }.filter { uuidRegex.matches(it) }.distinct()
        require(targets.isNotEmpty()) { tr("لا يوجد مستلمون صالحون") }
        val rows = targets.map { uid -> buildJsonObject { put("user_id", uid); put("event_type", "admin_announcement"); put("title", t); put("body", b); put("status", "sent") } }
        com.numberone.numberone.data.remote.SupabaseClientProvider.client.postgrest["notifications"].insert(rows)
        targets.size
    }
    override suspend fun resetSubscriptionAccess(subscriptionId: String, accessCode: String, expiryDate: String) = runCatching {
        require(subscriptionId.isNotBlank()) { tr("معرّف غير صالح") }
        val date = expiryDate.trim()
        require(accessCode.isNotBlank() || date.isNotEmpty()) { tr("أدخل رمزاً جديداً أو تاريخ انتهاء") }
        if (date.isNotEmpty()) {
            require(dateRegex.matches(date)) { tr("صيغة التاريخ yyyy-MM-dd") }
            java.time.LocalDate.parse(date)
        }
        require(uuidRegex.matches(subscriptionId)) { tr("معرّف غير صالح") }
        if (accessCode.isNotBlank()) require(accessCode.trim().length in 8..256) { tr("رمز الوصول يجب أن يكون بين 8 و256 خانة") }
        rpc.rpcRaw(
            "admin_reset_subscription_access",
            buildJsonObject {
                put("p_subscription_id", subscriptionId)
                if (accessCode.isNotBlank()) put("p_access_code", accessCode.trim())
                if (date.isNotEmpty()) put("p_expiry_date", date)
            }
        )
        Unit
    }
}
