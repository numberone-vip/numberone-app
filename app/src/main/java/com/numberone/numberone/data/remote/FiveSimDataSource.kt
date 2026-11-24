package com.numberone.numberone.data.remote

import com.numberone.numberone.domain.model.VirtualNumberCatalog
import com.numberone.numberone.domain.model.VirtualNumberOrder
import com.numberone.numberone.domain.model.VirtualNumberQuote
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.call.body
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Calls one authenticated Supabase Edge Function. The 5SIM bearer token is never shipped
 * to Android; the Edge Function calls 5SIM and delegates all wallet mutations to DB RPCs.
 */
class FiveSimDataSource {
    private val supabase = SupabaseClientProvider.client

    @Serializable
    private data class LatestResponse(val activation: VirtualNumberOrder? = null)

    @Serializable
    private data class EmptyResponse(val ok: Boolean = true)

    private data class Request(
        val action: String,
        val countryCode: String? = null,
        val serviceCode: String? = null,
        val operator: String? = null,
        val activationId: String? = null,
        val requestId: String? = null,
        val fcmToken: String? = null,
        val platform: String? = null
    )

    suspend fun catalog(): VirtualNumberCatalog = invoke(Request(action = "catalog"))

    suspend fun quote(countryCode: String, serviceCode: String): VirtualNumberQuote = invoke(
        Request(action = "quote", countryCode = countryCode, serviceCode = serviceCode)
    )

    suspend fun purchase(
        countryCode: String,
        serviceCode: String,
        operator: String,
        requestId: String
    ): VirtualNumberOrder = invoke(
        Request(
            action = "purchase",
            countryCode = countryCode,
            serviceCode = serviceCode,
            operator = operator,
            requestId = requestId
        )
    )

    suspend fun poll(activationId: String): VirtualNumberOrder = invoke(
        Request(action = "poll", activationId = activationId)
    )

    suspend fun latestActivation(): VirtualNumberOrder? =
        invoke<LatestResponse>(Request(action = "latest_activation")).activation

    /** Persists the user-owned activation hide action using an auth.uid()-scoped RPC. */
    suspend fun hideActivation(activationId: String) {
        require(activationId.isNotBlank()) { "Activation ID is required" }
        val params = buildJsonObject { put("p_activation_id", activationId) }
        supabase.postgrest.rpcRaw("virtual_numbers_hide_my_activation", params)
    }

    /** Refreshes the active activation to return the newest SMS received on that same number. */
    suspend fun requestNextSms(activationId: String): VirtualNumberOrder = invoke(
        Request(action = "request_next_sms", activationId = activationId)
    )

    suspend fun cancel(activationId: String): VirtualNumberOrder = invoke(
        Request(action = "cancel", activationId = activationId)
    )

    suspend fun registerDeviceToken(token: String) {
        invoke<EmptyResponse>(Request(action = "register_device_token", fcmToken = token, platform = "android"))
    }

    private suspend inline fun <reified T> invoke(request: Request): T {
        val payload = buildJsonObject {
            put("action", request.action)
            request.countryCode?.let { put("country_code", it) }
            request.serviceCode?.let { put("service_code", it) }
            request.operator?.let { put("operator", it) }
            request.activationId?.let { put("activation_id", it) }
            request.requestId?.let { put("request_id", it) }
            request.fcmToken?.let { put("fcm_token", it) }
            request.platform?.let { put("platform", it) }
        }
        return supabase.functions.invoke(function = "fivesim-api", body = payload).body()
    }
}
