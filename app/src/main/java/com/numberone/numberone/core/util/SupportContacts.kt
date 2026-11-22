package com.numberone.numberone.core.util

import android.net.Uri

/**
 * Official support channels used by the "forgot password" dialog.
 *
 * TODO(owner): fill these two values before release. While a value is blank the matching
 * button is shown disabled, so no one is ever sent to a wrong chat.
 */
object SupportContacts {
    /** WhatsApp number in international format, digits only, no "+" (example: 963912345678). */
    val WHATSAPP_NUMBER: String = ""

    /** Telegram username without the "@" (example: NumberOneSupport). */
    val TELEGRAM_USERNAME: String = ""

    fun whatsappUri(message: String): Uri? {
        val digits = WHATSAPP_NUMBER.filter { it.isDigit() }
        if (digits.isEmpty()) return null
        return Uri.parse("https://wa.me/$digits?text=${Uri.encode(message)}")
    }

    fun telegramUri(): Uri? {
        val username = TELEGRAM_USERNAME.trim().removePrefix("@")
        if (username.isEmpty()) return null
        return Uri.parse("https://t.me/$username")
    }
}
