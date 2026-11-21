package com.numberone.numberone.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Matches wallet_get_my_transactions() and remains compatible with the full table row. */
@Serializable
data class WalletTransaction(
    @SerialName("id") val id: String = "",
    @SerialName("user_id") val userId: String? = null,
    @SerialName("transaction_type") val transactionType: String = "",
    @SerialName("direction") val direction: String = "",
    @SerialName("amount") val amount: Double = 0.0,
    @SerialName("balance_after") val balanceAfter: Double = 0.0,
    @SerialName("currency") val currency: String = "USDT",
    @SerialName("reference_type") val referenceType: String? = null,
    @SerialName("reference_id") val referenceId: String? = null,
    @SerialName("transaction_hash") val transactionHash: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)
