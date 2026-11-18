package com.numberone.app.domain.model

enum class OrderStatus {
    PENDING,
    APPROVED,
    SMS_READY,
    REJECTED
}

data class VirtualService(
    val id: String,
    val appName: String,
    val countryNameAr: String,
    val countryNameEn: String,
    val countryFlag: String,
    val countryCode: String,
    val priceUsd: Double,
    val availableCount: Int,
)

data class PurchaseRequest(
    val id: String,
    val userId: String,
    val serviceName: String,
    val countryName: String,
    val amountUsd: Double,
    val receiptUrl: String? = null,
    val transactionRef: String? = null,
    val status: OrderStatus = OrderStatus.PENDING,
    val assignedNumber: String? = null,
    val otpCode: String? = null,
    val rejectionReason: String? = null,
    val createdAt: String
)

data class SubscriptionItem(
    val id: String,
    val phoneNumber: String,
    val serviceName: String,
    val expiresAt: String,
    val isActive: Boolean
)
