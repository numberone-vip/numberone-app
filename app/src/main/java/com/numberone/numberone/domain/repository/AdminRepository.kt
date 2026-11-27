package com.numberone.numberone.domain.repository

import com.numberone.numberone.domain.model.AdminService
import com.numberone.numberone.domain.model.AdminShamCashTopUp
import com.numberone.numberone.domain.model.AdminSubscription
import com.numberone.numberone.domain.model.AdminUser
import com.numberone.numberone.domain.model.AdminWalletDeposit
import com.numberone.numberone.domain.model.AppPaymentConfig
import com.numberone.numberone.domain.model.ServiceDraft
import com.numberone.numberone.domain.model.AdminSummary
import com.numberone.numberone.domain.model.Order
import com.numberone.numberone.domain.model.WalletTransaction

/** Admin actions. All authorization is enforced server-side (private.is_admin() inside each RPC). */
interface AdminRepository {
    suspend fun getSummary(): Result<AdminSummary>
    suspend fun listShamCashTopUps(): Result<List<AdminShamCashTopUp>>
    suspend fun approveShamCashTopUp(requestId: String, creditedUsdt: Double): Result<Double>
    suspend fun rejectShamCashTopUp(requestId: String, reason: String): Result<Boolean>
    suspend fun listOrders(): Result<List<Order>>
    suspend fun approveOrder(orderId: String, phoneNumber: String, expiryDate: String): Result<String>
    suspend fun rejectOrder(orderId: String, reason: String): Result<Unit>
    /** Short-lived signed URL for a receipt in the private payment-receipts bucket. */
    suspend fun getReceiptUrl(path: String): Result<String>

    // ---- Command-center additions ------------------------------------------
    suspend fun listDeposits(): Result<List<AdminWalletDeposit>>
    suspend fun listSubscriptions(): Result<List<AdminSubscription>>
    suspend fun getPaymentConfig(): Result<AppPaymentConfig>
    suspend fun updatePaymentConfig(config: AppPaymentConfig): Result<Unit>
    suspend fun listServices(): Result<List<AdminService>>
    suspend fun upsertService(draft: ServiceDraft): Result<Unit>
    suspend fun deleteService(serviceId: String): Result<Unit>
    suspend fun listUsers(): Result<List<AdminUser>>
    /** Newest wallet ledger rows across all users (read-only, admin-only RPC). */
    suspend fun listWalletTransactions(limit: Int = 100): Result<List<WalletTransaction>>
    /** Adds (positive) or removes (negative) USDT. Returns the new balance. */
    /** [requestId] is a client-generated UUID; the server ignores a repeat of the same id (no duplicate adjustment). */
    suspend fun adjustWalletBalance(userId: String, amount: Double, note: String, requestId: String): Result<Double>
    suspend fun assignSubscription(userId: String, serviceId: String, phoneNumber: String, accessCode: String, expiryDate: String): Result<Unit>
    /** Rotates the access code and/or sets a new expiry. Blank arguments are left unchanged. */
    suspend fun sendSubscriptionSms(subscriptionId: String, sender: String, body: String): Result<Unit>
    suspend fun approveDepositRequest(requestId: String, note: String = ""): Result<Unit>
    suspend fun sendAdminNotification(userIds: List<String>, title: String, body: String): Result<Int>
    suspend fun resetSubscriptionAccess(subscriptionId: String, accessCode: String, expiryDate: String): Result<Unit>
}
