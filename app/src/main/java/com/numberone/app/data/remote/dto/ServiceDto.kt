package com.numberone.app.data.remote.dto

import com.numberone.app.domain.model.VirtualService
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServiceDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String = "",
    @SerialName("app_name") val appName: String? = null,
    @SerialName("country") val country: String = "",
    @SerialName("country_name_ar") val countryNameAr: String? = null,
    @SerialName("country_name_en") val countryNameEn: String? = null,
    @SerialName("country_code") val countryCode: String = "",
    @SerialName("country_flag") val countryFlag: String? = null,
    @SerialName("price") val price: Double = 0.0,
    @SerialName("price_usd") val priceUsd: Double? = null,
    @SerialName("success_rate") val successRate: Int = 98,
    @SerialName("is_popular") val isPopular: Boolean = false,
    @SerialName("icon_url") val iconUrl: String? = null,
    @SerialName("available_count") val availableCount: Int = 0
)

fun ServiceDto.toDomain(): VirtualService {
    val resolvedEn = countryNameEn ?: country
    val resolvedAr = countryNameAr ?: when (country.uppercase()) {
        "USA", "US" -> "الولايات المتحدة"
        "UK", "GB" -> "المملكة المتحدة"
        "GERMANY", "DE" -> "ألمانيا"
        else -> country
    }
    val resolvedFlag = countryFlag ?: when (country.uppercase()) {
        "USA", "US" -> "🇺🇸"
        "UK", "GB" -> "🇬🇧"
        "GERMANY", "DE" -> "🇩🇪"
        else -> "🌐"
    }
    return VirtualService(
        id = id,
        appName = appName ?: name,
        countryNameAr = resolvedAr,
        countryNameEn = resolvedEn,
        countryFlag = resolvedFlag,
        priceUsd = priceUsd ?: price,
        successRate = successRate,
        isPopular = isPopular,
        availableCount = availableCount
    )
}
