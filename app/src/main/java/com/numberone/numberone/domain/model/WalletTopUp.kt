package com.numberone.numberone.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Matches wallet_get_my_deposit_requests(). */
@Serializable
data class WalletTopUp(
    @SerialName("id") val id: String = "",
    @SerialName("user_id") val userId: String? = null,
    @SerialName("requested_amount") val requestedAmount: Double = 0.0,
    @SerialName("currency") val currency: String = "USDT",
    @SerialName("network") val network: String = "BEP20",
    @SerialName("transaction_hash") val transactionHash: String? = null,
    @SerialName("status") val status: String = "pending",
    @SerialName("verified_amount") val verifiedAmount: Double? = null,
    @SerialName("binance_deposit_id") val binanceDepositId: String? = null,
    @SerialName("admin_note") val adminNote: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("credited_at") val creditedAt: String? = null,
    @SerialName("reviewed_at") val reviewedAt: String? = null
)
