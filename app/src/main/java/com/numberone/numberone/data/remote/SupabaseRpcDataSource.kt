
package com.numberone.numberone.data.remote

import com.numberone.numberone.domain.model.SmsMessage
import com.numberone.numberone.domain.model.Subscription
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Number One - Supabase RPC Data Source
 *
 * Handles subscription and SMS RPC calls.
 * Uses the verified Supabase function signatures.
 */
class SupabaseRpcDataSource {

    private val supabase = SupabaseClientProvider.client

    /**
     * Retrieves a subscription using its access code.
     *
     * Returns null when no subscription is found.
     */
    suspend fun getSubscription(
        accessCode: String
    ): Subscription? {

        require(accessCode.isNotBlank()) {
            "Access code cannot be empty."
        }

        val params = buildJsonObject { put("p_access_code", accessCode.trim()) }

        return supabase.postgrest
            .rpcDecode<List<Subscription>>("get_subscription", params)
            .singleOrNull()
    }

    /**
     * Retrieves SMS messages using the access code.
     */
    /** The signed-in account's numbers (manual and approved). Server returns 'expired' for past expiry. */
    suspend fun getMyAccountSubscriptions(): List<Subscription> {
        return supabase.postgrest.rpcDecode<List<Subscription>>("get_my_subscriptions", buildJsonObject { })
    }

    /** Messages of the signed-in account's active numbers. No access code needed (server checks auth.uid()). */
    suspend fun getMyAccountSms(): List<SmsMessage> {
        return supabase.postgrest.rpcDecode<List<SmsMessage>>("get_my_account_sms", buildJsonObject { })
    }

    suspend fun getMySms(
        accessCode: String
    ): List<SmsMessage> {

        require(accessCode.isNotBlank()) {
            "Access code cannot be empty."
        }

        val params = buildJsonObject { put("p_access_code", accessCode.trim()) }

        return supabase.postgrest
            .rpcDecode<List<SmsMessage>>("get_my_sms", params)
    }
}
