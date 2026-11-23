package com.numberone.numberone.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Admin DTOs. Every numeric and string field has a safe default so a null or
// missing value from the server never fails the whole list decode.

@Serializable
data class AdminCustomer(
    @SerialName("user_id") val userId: String = "",
    @SerialName("email") val email: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class AdminSubscription(
    @SerialName("id") val id: String = "",
    @SerialName("phone_number") val phoneNumber: String = "",
    @SerialName("status") val status: String? = null,
    @SerialName("expiry_date") val expiryDate: String? = null,
    @SerialName("admin_note") val adminNote: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("service_id") val serviceId: String? = null,
    @SerialName("customer_email") val customerEmail: String? = null,
    @SerialName("service_name") val serviceName: String? = null
)

@Serializable
data class AdminWalletBalance(
    @SerialName("user_id") val userId: String = "",
    @SerialName("email") val email: String? = null,
    @SerialName("balance") val balance: Double = 0.0,
    @SerialName("currency") val currency: String = "USDT",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class AdminWalletDeposit(
    @SerialName("id") val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("requested_amount") val requestedAmount: Double = 0.0,
    @SerialName("currency") val currency: String = "USDT",
    @SerialName("network") val network: String = "BEP20",
    @SerialName("transaction_hash") val transactionHash: String? = null,
    @SerialName("status") val status: String = "pending",
    @SerialName("verified_amount") val verifiedAmount: Double? = null,
    @SerialName("binance_deposit_id") val binanceDepositId: String? = null,
    @SerialName("admin_note") val adminNote: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("credited_at") val creditedAt: String? = null,
    @SerialName("reviewed_at") val reviewedAt: String? = null
)

/** Live payment + app configuration (get_payment_config / admin_update_payment_config). */
@Serializable
data class AppPaymentConfig(
    @SerialName("binance_wallet_address") val binanceWalletAddress: String = "",
    @SerialName("binance_network") val binanceNetwork: String = "BEP20",
    @SerialName("sham_cash_account") val shamCashAccount: String = "",
    @SerialName("sham_cash_name") val shamCashName: String = "",
    @SerialName("syp_usd_rate") val sypUsdRate: Double = 0.0,
    @SerialName("promo_banner_text") val promoBannerText: String = ""
)

/** One catalog row as returned by admin_list_services() (includes inactive packages). */
@Serializable
data class AdminService(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("country") val country: String = "",
    @SerialName("country_code") val countryCode: String = "",
    @SerialName("price") val price: Double = 0.0,
    @SerialName("description") val description: String? = null,
    @SerialName("icon_url") val iconUrl: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("available_count") val availableCount: Int? = null,
    @SerialName("created_at") val createdAt: String? = null
)

/** Input for admin_upsert_service(). A null [id] creates a new service. */
data class ServiceDraft(
    val id: String?,
    val name: String,
    val country: String,
    val countryCode: String,
    val price: Double,
    val description: String,
    val isActive: Boolean
)

/** A registered user with role and wallet balance, as shown in the Users tab. [role] is null when unknown. */
@Serializable
data class AdminUser(
    @SerialName("user_id") val userId: String = "",
    @SerialName("email") val email: String? = null,
    @SerialName("role") val role: String? = null,
    @SerialName("balance") val balance: Double = 0.0,
    @SerialName("currency") val currency: String = "USDT",
    @SerialName("created_at") val createdAt: String = ""
) {
    val isAdmin: Boolean get() = role.orEmpty().trim().equals("admin", ignoreCase = true)
}
