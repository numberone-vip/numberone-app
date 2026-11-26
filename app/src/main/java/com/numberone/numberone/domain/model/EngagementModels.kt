package com.numberone.numberone.domain.model
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppNotification(
    val id: String,
    @SerialName("event_type") val eventType: String = "",
    val title: String = "",
    val body: String = "",
    val status: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("read_at") val readAt: String? = null
) { val isUnread: Boolean get() = readAt.isNullOrBlank() }

@Serializable
data class PromoValidationResult(
    @SerialName("promo_id") val promoId: String = "",
    val code: String = "",
    @SerialName("discount_type") val discountType: String = "",
    @SerialName("discount_value") val discountValue: Double = 0.0,
    @SerialName("discount_amount") val discountAmount: Double = 0.0,
    @SerialName("final_amount") val finalAmount: Double = 0.0,
    val currency: String = "USDT"
)
