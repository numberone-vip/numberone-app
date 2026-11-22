package com.numberone.numberone.core.util

import com.numberone.numberone.core.localization.tr

/** ISO-3166 alpha-2 code (e.g. "SY") to its flag emoji, or null when the code is not a two-letter code. */
fun flagEmoji(code: String?): String? {
    val c = code?.trim()?.uppercase().orEmpty()
    if (c.length != 2 || !c.all { it in 'A'..'Z' }) return null
    val base = 0x1F1E6
    return String(Character.toChars(base + (c[0] - 'A'))) + String(Character.toChars(base + (c[1] - 'A')))
}

/** Best-effort duration tag read from a service name ("شهري"/"سنوي"/monthly/yearly), or null. */
fun serviceDurationLabel(name: String): String? {
    val n = name.lowercase()
    return when {
        listOf("سنوي", "سنة", "yearly", "annual", "year", "12 month").any { it in n } -> tr("سنوي")
        listOf("شهري", "شهر", "monthly", "month").any { it in n } -> tr("شهري")
        else -> null
    }
}
