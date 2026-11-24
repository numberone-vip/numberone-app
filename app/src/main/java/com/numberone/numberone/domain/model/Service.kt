package com.numberone.numberone.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Service(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("country") val country: String = "",
    @SerialName("country_code") val countryCode: String = "",
    @SerialName("price") val price: Double = 0.0,
    @SerialName("icon_url") val iconUrl: String? = null,
    @SerialName("available_count") val availableCount: Int? = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @kotlinx.serialization.Transient val options: List<Service> = emptyList()
)

@Serializable
data class WalletOrderResult(
    @SerialName("order_id") val orderId: String = "",
    @SerialName("service_name") val serviceName: String = "",
    @SerialName("price") val price: Double = 0.0,
    @SerialName("balance_after") val balanceAfter: Double = 0.0,
    @SerialName("status") val status: String = "",
    @SerialName("payment_status") val paymentStatus: String = ""
)

@Serializable
data class PaymentOrderCreation(
    @SerialName("order_id") val orderId: String = "",
    @SerialName("service_name") val serviceName: String = "",
    @SerialName("price") val price: Double = 0.0,
    @SerialName("wallet_address") val walletAddress: String = "",
    @SerialName("network") val network: String = "",
    @SerialName("currency") val currency: String = "USDT",
    @SerialName("expires_at") val expiresAt: String = ""
)

@Serializable
data class Order(
    @SerialName("id") val id: String = "",
    @SerialName("service_id") val serviceId: String? = null,
    @SerialName("service_name") val serviceName: String = "",
    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("sms_code") val smsCode: String? = null,
    @SerialName("status") val status: String = "",
    @SerialName("price") val price: Double = 0.0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("payment_status") val paymentStatus: String = "",
    @SerialName("payment_method") val paymentMethod: String = "",
    @SerialName("receipt_path") val receiptPath: String? = null,
    @SerialName("transaction_ref") val transactionRef: String? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("customer_email") val customerEmail: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("subscription_id") val subscriptionId: String? = null,
    @SerialName("country_name") val countryName: String? = null,
    @SerialName("payment_wallet_address") val paymentWalletAddress: String? = null,
    @SerialName("payment_network") val paymentNetwork: String? = null,
    @SerialName("payment_currency") val paymentCurrency: String? = null
)
