package com.numberone.numberone.data.remote

import com.numberone.numberone.domain.model.SmsMessage
import com.numberone.numberone.domain.model.Subscription
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseRpcDataSource {

    private val supabase = SupabaseClientProvider.client

    suspend fun getSubscriptionById(subscriptionId: String): Subscription? {
        val cleanId = subscriptionId.trim()
        require(cleanId.isNotBlank()) { "Subscription ID cannot be empty." }
        val params = buildJsonObject { put("p_subscription_id", cleanId) }
        return supabase.postgrest
            .rpcDecode<List<Subscription>>("get_my_subscription_by_id", params)
            .singleOrNull()
    }

    suspend fun getSubscription(accessCode: String): Subscription? {
        require(accessCode.isNotBlank()) { "Access code cannot be empty." }
        val params = buildJsonObject { put("p_access_code", accessCode.trim()) }
        return supabase.postgrest
            .rpcDecode<List<Subscription>>("get_subscription", params)
            .singleOrNull()
    }

    suspend fun getMySms(accessCode: String): List<SmsMessage> {
        require(accessCode.isNotBlank()) { "Access code cannot be empty." }
        val params = buildJsonObject { put("p_access_code", accessCode.trim()) }
        return supabase.postgrest
            .rpcDecode<List<SmsMessage>>("get_my_sms", params)
    }
}
