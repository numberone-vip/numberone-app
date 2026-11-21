package com.numberone.numberone.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Matches wallet_get_my_sham_cash_topups(). */
@Serializable
data class ShamCashTopUp(
    @SerialName("id") val id: String = "",
    @SerialName("payment_method") val paymentMethod: String = "",
    @SerialName("paid_amount") val paidAmount: Double = 0.0,
    @SerialName("paid_currency") val paidCurrency: String? = null,
    @SerialName("receipt_path") val receiptPath: String? = null,
    @SerialName("status") val status: String = "pending",
    @SerialName("credited_usdt") val creditedUsdt: Double? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("credited_at") val creditedAt: String? = null,
    @SerialName("reviewed_at") val reviewedAt: String? = null
)

/** Matches admin_list_sham_cash_topups(). */
@Serializable
data class AdminShamCashTopUp(
    @SerialName("id") val id: String = "",
    @SerialName("user_id") val userId: String? = null,
    @SerialName("customer_email") val customerEmail: String? = null,
    @SerialName("payment_method") val paymentMethod: String = "",
    @SerialName("paid_amount") val paidAmount: Double = 0.0,
    @SerialName("paid_currency") val paidCurrency: String? = null,
    @SerialName("receipt_path") val receiptPath: String? = null,
    @SerialName("status") val status: String = "pending",
    @SerialName("credited_usdt") val creditedUsdt: Double? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("credited_at") val creditedAt: String? = null,
    @SerialName("reviewed_at") val reviewedAt: String? = null
)

/**
 * Summary counts for the admin dashboard.
 * pendingDeposits counts open Binance deposit requests; sham cash is counted from its own list.
 */
data class AdminSummary(
    val customers: Int,
    val wallets: Int,
    val deposits: Int,
    val subscriptions: Int,
    val pendingDeposits: Int = 0
)

/** Statuses after which no further admin action is possible (server still enforces transitions). */
private val TERMINAL_STATUSES = setOf("approved", "credited", "completed", "rejected", "cancelled", "expired", "active")

fun isOpenStatus(status: String): Boolean = status.trim().lowercase() !in TERMINAL_STATUSES
