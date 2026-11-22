package com.numberone.numberone.domain.repository

import com.numberone.numberone.domain.model.AdminAuditEntry
import com.numberone.numberone.domain.model.AdminPendingPurchase
import com.numberone.numberone.domain.model.AdminPhoneMatch
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
    suspend fun approveOrder(orderId: String, phoneNumber: String, accessCode: String, expiryDate: String): Result<Unit>
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
    /** Promotes or demotes an administrator. The server blocks self-change and removal of the last admin. */
    suspend fun setAdminRole(userId: String, makeAdmin: Boolean): Result<Boolean>
    /** Current virtual-number (5SIM) profit margin in percent (live RPC admin_get_virtual_number_margin). */
    suspend fun getVirtualNumberMargin(): Result<Double>
    /** Sets the margin in percent (live RPC admin_set_virtual_number_margin). Returns the stored value if the server reports one. */
    suspend fun setVirtualNumberMargin(percent: Double): Result<Double>
    /** Newest admin actions first (admin-only RPC; needs the audit-log migration). */
    suspend fun listAuditLog(limit: Int = 100): Result<List<AdminAuditEntry>>
    /** Records an admin action. The server stores the caller as the actor. Never include secrets in [details]. */
    suspend fun logAdminAction(action: String, targetId: String? = null, details: String? = null): Result<Unit>
    /** Wallet-paid manual purchases still waiting for the number to be handed over. */
    suspend fun listPendingPurchases(): Result<List<AdminPendingPurchase>>
    /** Customers whose assigned number matches this phone (digits only are compared). */
    suspend fun findCustomersByPhone(phone: String): Result<List<AdminPhoneMatch>>
    /** Approves a manual purchase: stores the number on the customer's account and marks the request approved. */
    suspend fun approveManualPurchase(requestId: String, phone: String, expiry: String): Result<Unit>
    /** Adds an incoming SMS (e.g. a verification code) to a subscription. The customer sees it under their number. */
    suspend fun addSmsToSubscription(subscriptionId: String, sender: String, messageText: String): Result<Unit>
    suspend fun assignSubscription(userId: String, serviceId: String, phoneNumber: String, accessCode: String, expiryDate: String): Result<Unit>
    /** Rotates the access code and/or sets a new expiry. Blank arguments are left unchanged. */
    suspend fun resetSubscriptionAccess(subscriptionId: String, accessCode: String, expiryDate: String): Result<Unit>
}
