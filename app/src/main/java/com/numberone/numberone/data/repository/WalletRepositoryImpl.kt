package com.numberone.numberone.data.repository

import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.data.remote.SupabaseClientProvider
import com.numberone.numberone.data.remote.SupabaseWalletDataSource
import com.numberone.numberone.data.remote.WalletDataSource
import com.numberone.numberone.domain.model.*
import com.numberone.numberone.domain.repository.WalletRepository
import io.github.jan.supabase.auth.auth

/**
 * Client-side validation here is UX only. Every money-moving decision (price, balance,
 * credit, ownership, duplicate TXID, expiry) is enforced by Supabase RPCs / RLS / Edge Functions.
 */
class WalletRepositoryImpl(
    private val walletDataSource: WalletDataSource = SupabaseWalletDataSource()
) : WalletRepository {
    private val supabase = SupabaseClientProvider.client
    private fun currentUserId() = supabase.auth.currentUserOrNull()?.id
        ?: throw IllegalStateException(tr("يرجى تسجيل الدخول أولاً"))

    private companion object {
        const val MAX_TOP_UP = 100000.0
        val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        val TX_HASH_REGEX = Regex("^[0-9a-fA-F]{64}$")
    }

    private fun requireUuid(value: String): String {
        val v = value.trim()
        require(UUID_REGEX.matches(v)) { tr("معرّف غير صالح") }
        return v
    }

    /** Accepts a BEP20 hash with or without the 0x prefix; sends 64 lowercase hex chars. */
    private fun normalizeTxHash(raw: String): String {
        val h = raw.trim().removePrefix("0x").removePrefix("0X")
        require(TX_HASH_REGEX.matches(h)) { tr("أدخل رقم معاملة صالحاً بطول 64 خانة") }
        return h.lowercase()
    }

    override suspend fun getWallet() = runCatching { walletDataSource.getWallet(currentUserId()) }
    override suspend fun getTransactions() = runCatching { walletDataSource.getTransactions(currentUserId()) }
    override suspend fun createTopUpRequest(request: CreateWalletTopUp) = runCatching {
        require(request.amount.isFinite() && request.amount > 0.0 && request.amount <= MAX_TOP_UP) {
            tr("المبلغ يجب أن يكون أكبر من صفر وأقل من 100000 USDT")
        }
        walletDataSource.createTopUpRequest(currentUserId(), request)
    }
    override suspend fun getMyTopUpRequests() = runCatching { walletDataSource.getMyTopUpRequests(currentUserId()) }
    override suspend fun submitDepositTransaction(requestId: String, transactionHash: String) = runCatching {
        val id = requireUuid(requestId); val hash = normalizeTxHash(transactionHash)
        currentUserId(); walletDataSource.submitDepositTransaction(id, hash)
    }
    override suspend fun verifyDeposit(requestId: String, transactionHash: String) = runCatching {
        val id = requireUuid(requestId)
        // Blank means automatic matching by amount; a TXID, if given, is still validated.
        val hash = if (transactionHash.isBlank()) "" else normalizeTxHash(transactionHash)
        currentUserId(); walletDataSource.verifyDeposit(id, hash)
    }
    override suspend fun getServices() = runCatching { currentUserId(); walletDataSource.getServices() }
    override suspend fun getMyOrders() = runCatching { walletDataSource.getMyOrders(currentUserId()) }
    override suspend fun createOrder(serviceId: String) = runCatching {
        val id = requireUuid(serviceId); currentUserId(); walletDataSource.createOrder(id)
    }
    override suspend fun uploadReceipt(orderId: String, bytes: ByteArray, mimeType: String, extension: String) = runCatching {
        val id = requireUuid(orderId); currentUserId(); walletDataSource.uploadReceipt(id, bytes, mimeType, extension)
    }
    override suspend fun submitOrderPayment(orderId: String, transactionHash: String, receiptPath: String) = runCatching {
        val id = requireUuid(orderId); val hash = normalizeTxHash(transactionHash)
        val uid = currentUserId()
        // The receipt must live under the caller's own folder for this order (storage RLS enforces this too).
        require(receiptPath.startsWith("$uid/$id/") && !receiptPath.contains("..")) { tr("مسار الإيصال غير صالح") }
        walletDataSource.submitOrderPayment(id, hash, receiptPath)
    }
    override suspend fun purchaseServiceWithWallet(serviceId: String, requestId: String) = runCatching {
        val sid = requireUuid(serviceId); val rid = requireUuid(requestId)
        currentUserId(); walletDataSource.purchaseServiceWithWallet(sid, rid)
    }

    override suspend fun createShamCashTopUp(paymentMethod: String, paidAmount: Double) = runCatching {
        require(paymentMethod == "SHAM_CASH_SYP" || paymentMethod == "SHAM_CASH_USD") { tr("طريقة الدفع غير مدعومة") }
        val max = if (paymentMethod == "SHAM_CASH_SYP") 1.0e11 else MAX_TOP_UP
        require(paidAmount.isFinite() && paidAmount > 0.0 && paidAmount <= max) { tr("أدخل مبلغاً صحيحاً") }
        currentUserId(); walletDataSource.createShamCashTopUp(paymentMethod, paidAmount)
    }
    override suspend fun submitShamCashReceipt(requestId: String, bytes: ByteArray, mimeType: String, extension: String) = runCatching {
        val id = requireUuid(requestId); currentUserId()
        walletDataSource.submitShamCashReceipt(id, bytes, mimeType, extension)
    }
    override suspend fun getMyShamCashTopUps() = runCatching { currentUserId(); walletDataSource.getMyShamCashTopUps() }
}
