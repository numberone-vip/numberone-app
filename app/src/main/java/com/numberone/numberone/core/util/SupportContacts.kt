package com.numberone.numberone.core.util

import android.net.Uri

object SupportContacts {
    const val WHATSAPP_NUMBER: String = "13343908826"
    const val TELEGRAM_USERNAME: String = "NumbersStore_Sy"

    fun whatsappUri(message: String): Uri? {
        val digits = WHATSAPP_NUMBER.filter { it.isDigit() }
        if (digits.isEmpty()) return null
        return Uri.parse("https://whatsapp.com{Uri.encode(message)}")
    }

    fun telegramUri(): Uri? {
        val username = TELEGRAM_USERNAME.trim().removePrefix("@")
        if (username.isEmpty()) return null
        return Uri.parse("https://t.me")
    }
}
