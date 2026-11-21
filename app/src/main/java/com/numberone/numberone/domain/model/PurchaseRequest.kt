package com.numberone.numberone.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PurchaseRequest(
    @SerialName("id") val id: String = "",
    @SerialName("access_code") val accessCode: String? = null,
    @SerialName("country_name") val countryName: String? = null,
    @SerialName("duration") val duration: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    @SerialName("receipt_image_base64") val receiptImageBase64: String? = null,
    @SerialName("status") val status: String? = "pending",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("country") val country: String? = null,
    @SerialName("amount") val amount: Int? = null,
    @SerialName("receipt_image") val receiptImage: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null
)
