
package com.numberone.numberone.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Number One - SMS Message Model
 * Matches the actual get_my_sms RPC response.
 */
@Serializable
data class SmsMessage(

    val sender: String = "",

    @SerialName("message_text")
    val messageText: String = "",

    @SerialName("created_at")
    val createdAt: String = "",

    @SerialName("is_read")
    val isRead: Boolean = false
)
