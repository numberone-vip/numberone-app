package com.numberone.numberone.core.localization

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Current UI language, shared by the whole app.
 * Reads made during composition recompose automatically when the language changes.
 */
object AppStrings {
    var language: AppLanguage by mutableStateOf(AppLanguage.ARABIC)
}

/** Source strings are written in Arabic. Returns the English text when English is selected. */
fun tr(arabic: String): String {
    if (AppStrings.language == AppLanguage.ARABIC) return arabic
    return ENGLISH_TEXT[arabic] ?: arabic
}

/** Like [tr], then replaces each {0}, {1}, ... placeholder with the given values, in order. */
fun trf(arabic: String, vararg args: Any?): String {
    var text = tr(arabic)
    args.forEachIndexed { index, value -> text = text.replace("{$index}", value.toString()) }
    return text
}
