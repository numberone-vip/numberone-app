package com.numberone.app.data.remote.dto

import com.numberone.app.domain.model.VirtualService
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors the columns that actually exist in public.services.
 * Optional UI-only values must not be fabricated when the database has no source for them.
 */
@Serializable
data class ServiceDto(
    val id: String,
    val name: String,
    val country: String,
    @SerialName("country_code") val countryCode: String,
    val price: Double,
    @SerialName("available_count") val availableCount: Int? = 0
)

fun ServiceDto.toDomain(): VirtualService {
    val normalizedCountry = country.trim().uppercase()
    val countryNameAr = when (normalizedCountry) {
        "UNITED STATES", "USA", "US" -> "الولايات المتحدة"
        "UNITED KINGDOM", "UK", "GB" -> "المملكة المتحدة"
        "MALAYSIA", "MY" -> "ماليزيا"
        "GERMANY", "DE" -> "ألمانيا"
        else -> country
    }
    val flag = when (normalizedCountry) {
        "UNITED STATES", "USA", "US" -> "🇺🇸"
        "UNITED KINGDOM", "UK", "GB" -> "🇬🇧"
        "MALAYSIA", "MY" -> "🇲🇾"
        "GERMANY", "DE" -> "🇩🇪"
        else -> "🌐"
    }

    return VirtualService(
        id = id,
        appName = name,
        countryNameAr = countryNameAr,
        countryNameEn = country,
        countryFlag = flag,
        countryCode = countryCode,
        priceUsd = price,
        availableCount = availableCount ?: 0
    )
}
