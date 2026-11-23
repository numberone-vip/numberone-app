
package com.numberone.numberone.core.localization

/**
 * Number One - Supported App Languages
 * Default language: Arabic
 */
enum class AppLanguage(
    val languageCode: String,
    val displayName: String,
    val isArabic: Boolean
) {
    ARABIC(
        languageCode = "ar",
        displayName = "العربية",
        isArabic = true
    ),

    ENGLISH(
        languageCode = "en",
        displayName = "English",
        isArabic = false
    );

    companion object {
        fun fromLanguageCode(code: String?): AppLanguage {
            return when (code?.lowercase()) {
                "en" -> ENGLISH
                "ar" -> ARABIC
                else -> ARABIC
            }
        }
    }
}
