package com.numberone.numberone.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Subscription(
    @SerialName("id") val id: String? = null,
    @SerialName("phone_number") val phoneNumber: String = "",
    @SerialName("status") val status: String? = null,
    @SerialName("expiry_date") val expiryDate: String? = null,
    @SerialName("admin_note") val adminNote: String? = null,
    @SerialName("service_name") val serviceName: String? = null,
    @SerialName("latest_otp") val latestOtp: String? = null,
    @SerialName("is_valid") val isValid: Boolean = false
)
