package com.numberone.numberone.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response for wallet_get_my_summary(). user_id is absent from that RPC response. */
@Serializable
data class Wallet(
    @SerialName("user_id") val userId: String? = null,
    @SerialName("balance") val balance: Double = 0.0,
    @SerialName("currency") val currency: String = "USDT",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)
