package com.numberone.app.data.remote.dto

import com.numberone.app.domain.model.OrderStatus
import com.numberone.app.domain.model.PurchaseRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("service_name") val serviceName: String,
    @SerialName("country_name") val countryName: String? = null,
    val price: Double = 0.0,
    @SerialName("receipt_path") val receiptPath: String? = null,
    @SerialName("transaction_ref") val transactionRef: String? = null,
    val status: String,
    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("sms_code") val smsCode: String? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("created_at") val createdAt: String
)

fun OrderDto.toDomain(): PurchaseRequest {
    val mappedStatus = when (status.uppercase()) {
        "APPROVED", "ACTIVE", "COMPLETED" -> OrderStatus.APPROVED
        "SMS_READY", "CODE_RECEIVED" -> OrderStatus.SMS_READY
        "REJECTED", "CANCELLED", "FAILED" -> OrderStatus.REJECTED
        else -> OrderStatus.PENDING
    }
    return PurchaseRequest(
        id = id,
        userId = userId,
        serviceName = serviceName,
        countryName = countryName.orEmpty(),
        amountUsd = price,
        receiptUrl = receiptPath,
        transactionRef = transactionRef,
        status = mappedStatus,
        assignedNumber = phoneNumber,
        otpCode = smsCode,
        rejectionReason = rejectionReason,
        createdAt = createdAt
    )
}
