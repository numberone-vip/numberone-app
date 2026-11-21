package com.numberone.numberone.domain.repository

import com.numberone.numberone.domain.model.PurchaseRequest

/**
 * Read-only access to the customer's legacy purchase requests.
 * Purchases are created server-side only (create_order / create_wallet_order RPCs);
 * the client never inserts purchase rows or supplies prices.
 */
interface PurchaseRepository {
    suspend fun getMyPurchaseRequests(): Result<List<PurchaseRequest>>
}
