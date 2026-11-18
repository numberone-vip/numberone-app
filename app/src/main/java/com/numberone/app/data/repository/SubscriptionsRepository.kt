package com.numberone.app.data.repository

import com.numberone.app.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class CustomerSubscriptionDto(
    val id: String,
    @SerialName("phone_number") val phoneNumber: String,
    val status: String? = null,
    @SerialName("expiry_date") val expiryDate: String? = null,
    @SerialName("admin_note") val adminNote: String? = null,
    @SerialName("service_name") val serviceName: String? = null,
    @SerialName("latest_otp") val latestOtp: String? = null
)

data class CustomerSubscription(
    val id: String,
    val phoneNumber: String,
    val status: String,
    val expiryDate: String?,
    val adminNote: String?,
    val serviceName: String,
    val latestOtp: String?
)

class SubscriptionsRepository {
    private val client = SupabaseClientProvider.client
    suspend fun getMySubscriptions(): Result<List<CustomerSubscription>> = withContext(Dispatchers.IO) {
        runCatching {
            client.postgrest.rpc("get_my_subscriptions").decodeList<CustomerSubscriptionDto>().map {
                CustomerSubscription(
                    id = it.id,
                    phoneNumber = it.phoneNumber,
                    status = it.status ?: "unknown",
                    expiryDate = it.expiryDate,
                    adminNote = it.adminNote,
                    serviceName = it.serviceName ?: "Virtual number",
                    latestOtp = it.latestOtp
                )
            }
        }
    }
}
