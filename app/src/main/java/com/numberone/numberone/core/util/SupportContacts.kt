package com.numberone.numberone.core.util

import android.net.Uri

object SupportContacts {
    const val WHATSAPP_NUMBER: String = "13343908826"
    const val TELEGRAM_USERNAME: String = "NumbersStore_Sy"

    fun whatsappUri(message: String): Uri? = Uri.parse("https://api.whatsapp.com/send?phone=13343908826&text=" + Uri.encode(message))
    fun telegramUri(): Uri? = Uri.parse("https://t.me/NumbersStore_Sy")
}
