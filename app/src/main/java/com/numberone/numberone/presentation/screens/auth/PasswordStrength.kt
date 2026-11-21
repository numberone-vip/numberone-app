package com.numberone.numberone.presentation.screens.auth

import com.numberone.numberone.core.localization.tr

/** Password strength shown by the sign-up bar. [level] is the number of filled segments (0..4). */
internal enum class PasswordStrength(val level: Int, private val labelKey: String) {
    NONE(0, ""),
    WEAK(1, "ضعيفة"),
    FAIR(2, "مقبولة"),
    GOOD(3, "جيدة"),
    STRONG(4, "قوية");

    /** Localized label, read on each use so it follows the selected language. */
    val label: String get() = if (labelKey.isEmpty()) "" else tr(labelKey)
}

internal fun evaluatePasswordStrength(password: String): PasswordStrength {
    if (password.isEmpty()) return PasswordStrength.NONE
    if (password.length < 8) return PasswordStrength.WEAK
    var points = 0
    if (password.length >= 12) points++
    if (password.any { it.isLowerCase() } && password.any { it.isUpperCase() }) points++
    if (password.any { it.isDigit() }) points++
    if (password.any { !it.isLetterOrDigit() }) points++
    return when {
        points <= 0 -> PasswordStrength.WEAK
        points == 1 -> PasswordStrength.FAIR
        points == 2 -> PasswordStrength.GOOD
        else -> PasswordStrength.STRONG
    }
}
