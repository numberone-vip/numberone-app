package com.numberone.numberone.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One message in the customer-admin chat (live RPCs customer_list_messages / admin_list_chat). */
@Serializable
data class ChatMessage(
    @SerialName("id") val id: Long = 0,
    @SerialName("sender") val sender: String = "",
    @SerialName("body") val body: String = "",
    @SerialName("created_at") val createdAt: String = ""
) {
    val fromAdmin: Boolean get() = sender == "admin"
}

/** One conversation in the admin inbox (live RPC admin_list_conversations). */
@Serializable
data class ChatConversation(
    @SerialName("user_id") val userId: String = "",
    @SerialName("email") val email: String? = null,
    @SerialName("last_body") val lastBody: String = "",
    @SerialName("last_at") val lastAt: String = "",
    @SerialName("unread") val unread: Long = 0
)
