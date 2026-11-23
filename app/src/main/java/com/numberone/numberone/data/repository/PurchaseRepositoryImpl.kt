package com.numberone.numberone.data.repository

import com.numberone.numberone.data.remote.SupabaseClientProvider
import com.numberone.numberone.data.remote.rpcJson
import com.numberone.numberone.domain.model.PurchaseRequest
import com.numberone.numberone.domain.repository.PurchaseRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

/** Read-only legacy purchase history. RLS must restrict rows to auth.uid(). */
class PurchaseRepositoryImpl : PurchaseRepository {

    private val supabase = SupabaseClientProvider.client

    override suspend fun getMyPurchaseRequests(): Result<List<PurchaseRequest>> = runCatching {
        val currentUserId = supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("User must be signed in to view purchase requests.")

        supabase.postgrest.from("purchase_requests")
            .select {
                filter { eq("user_id", currentUserId) }
                order(column = "created_at", order = Order.DESCENDING)
            }
            .data.let { rpcJson.decodeFromString<List<PurchaseRequest>>(it) }
    }
}
