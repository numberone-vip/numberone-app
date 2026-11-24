package com.numberone.numberone.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VirtualNumberCountry(
    val name: String,
    /** 5SIM path segment, e.g. "canada" or "england". */
    @SerialName("country_code") val countryCode: String,
    val iso: String = "",
    @SerialName("dial_code") val dialCode: String = ""
)

@Serializable
data class VirtualNumberService(
    val code: String,
    val name: String
)

@Serializable
data class VirtualNumberOperator(
    val name: String,
    @SerialName("success_rate") val successRate: Double = 0.0,
    @SerialName("available_qty") val availableQty: Int = 0,
    @SerialName("base_price") val basePrice: Double
)

@Serializable
data class VirtualNumberQuote(
    @SerialName("country_code") val countryCode: String,
    @SerialName("service_code") val serviceCode: String,
    val currency: String = "USDT",
    val operators: List<VirtualNumberOperator> = emptyList(),
    @SerialName("margin_mode") val marginMode: String = "percent",
    @SerialName("margin_percent") val marginPercent: Double = 0.0,
    @SerialName("margin_fixed") val marginFixed: Double = 0.0
) {
    fun finalPrice(operator: VirtualNumberOperator): Double = when (marginMode.lowercase()) {
        "fixed" -> operator.basePrice + marginFixed
        else -> operator.basePrice * (1.0 + marginPercent / 100.0)
    }
}

@Serializable
data class VirtualNumberCatalog(
    val countries: List<VirtualNumberCountry> = emptyList(),
    val services: List<VirtualNumberService> = emptyList()
)

@Serializable
data class VirtualNumberOrder(
    @SerialName("activation_id") val activationId: String,
    val phone: String,
    val status: String = "PENDING",
    @SerialName("sms_code") val smsCode: String? = null,
    @SerialName("sms_text") val smsText: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("charged_price") val chargedPrice: Double = 0.0,
    @SerialName("base_price") val basePrice: Double = 0.0,
    @SerialName("product_code") val productCode: String = "",
    @SerialName("country_code") val countryCode: String = "",
    @SerialName("is_hidden") val isHidden: Boolean = false
)
