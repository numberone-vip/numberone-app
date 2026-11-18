package com.numberone.app.data.repository

import com.numberone.app.data.remote.SupabaseClientProvider
import com.numberone.app.data.remote.dto.OrderDto
import com.numberone.app.data.remote.dto.toDomain
import com.numberone.app.domain.model.PurchaseRequest
import io.github.jan.supabase.gotrue.gotrue
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.eq
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OrdersRepository {
    private val client = SupabaseClientProvider.client

    suspend fun getMyOrders(): Result<List<PurchaseRequest>> = withContext(Dispatchers.IO) {
        runCatching {
            val userId = client.gotrue.currentSessionOrNull()?.user?.id
                ?: error("يجب تسجيل الدخول أولاً.")
            client.postgrest["orders"]
                .select { eq("user_id", userId) }
                .decodeList<OrderDto>()
                .map { it.toDomain() }
        }
    }
}
