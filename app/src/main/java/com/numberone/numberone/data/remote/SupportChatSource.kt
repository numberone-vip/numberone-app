package com.numberone.numberone.data.remote

import com.numberone.numberone.domain.model.ChatConversation
import com.numberone.numberone.domain.model.ChatMessage
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Thin wrapper over the chat RPCs. Message bodies are plain text; the server enforces length and rate limits. */
object SupportChatSource {
    private val db get() = SupabaseClientProvider.client.postgrest

    suspend fun customerList(limit: Int = 100): List<ChatMessage> =
        db.rpcDecode<List<ChatMessage>>("customer_list_messages", buildJsonObject { put("p_limit", limit) })

    suspend fun customerSend(body: String): Long =
        db.rpcRaw("customer_send_message", buildJsonObject { put("p_body", body) }).trim().toLongOrNull() ?: 0L

    suspend fun adminConversations(): List<ChatConversation> =
        db.rpcDecode<List<ChatConversation>>("admin_list_conversations", buildJsonObject { })

    suspend fun adminChat(userId: String, limit: Int = 200): List<ChatMessage> =
        db.rpcDecode<List<ChatMessage>>(
            "admin_list_chat",
            buildJsonObject { put("p_user_id", userId); put("p_limit", limit) }
        )

    suspend fun adminSend(userId: String, body: String): Long =
        db.rpcRaw(
            "admin_send_chat",
            buildJsonObject { put("p_user_id", userId); put("p_body", body) }
        ).trim().toLongOrNull() ?: 0L
}
