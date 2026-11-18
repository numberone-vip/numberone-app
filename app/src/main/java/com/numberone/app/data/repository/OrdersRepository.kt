package com.numberone.app.data.repository

import com.numberone.app.data.remote.SupabaseClientProvider
import com.numberone.app.domain.model.OrderStatus
import com.numberone.app.domain.model.PurchaseRequest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class OrderDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("service_name") val serviceName: String,
    @SerialName("country_name") val countryName: String? = null,
    val price: Double,
    @SerialName("receipt_path") val receiptPath: String? = null,
    @SerialName("transaction_ref") val transactionRef: String? = null,
    val status: String,
    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("sms_code") val smsCode: String? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("created_at") val createdAt: String
)

class OrdersRepository {
    private val client = SupabaseClientProvider.client

    suspend fun getMyOrders(): Result<List<PurchaseRequest>> = withContext(Dispatchers.IO) {
        runCatching {
            client.postgrest["orders"].select().decodeList<OrderDto>()
                .sortedByDescending { it.createdAt }
                .map { row ->
                    val state = when {
                        row.status.equals("REJECTED", true) -> OrderStatus.REJECTED
                        !row.smsCode.isNullOrBlank() -> OrderStatus.SMS_READY
                        !row.phoneNumber.isNullOrBlank() || row.status.equals("COMPLETED", true) -> OrderStatus.APPROVED
                        else -> OrderStatus.PENDING
                    }
                    PurchaseRequest(
                        id = row.id,
                        userId = row.userId.orEmpty(),
                        serviceName = row.serviceName,
                        countryName = row.countryName.orEmpty(),
                        amountUsd = row.price,
                        receiptUrl = row.receiptPath,
                        transactionRef = row.transactionRef,
                        status = state,
                        assignedNumber = row.phoneNumber,
                        otpCode = row.smsCode,
                        rejectionReason = row.rejectionReason,
                        createdAt = row.createdAt
                    )
                }
        }
    }
}
