package com.numberone.numberone.presentation.util

import com.numberone.numberone.core.components.StatusTone
import com.numberone.numberone.core.components.statusTone
import com.numberone.numberone.domain.model.AdminWalletDeposit
import com.numberone.numberone.domain.model.Order
import com.numberone.numberone.domain.model.isOpenStatus
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

/*
 * Pure order rules shared by the customer orders screen and the admin console.
 * No UI and no network access. This file decides what an order "needs", so the customer and
 * the admin always read the same order the same way. It does not change any payment logic.
 */

// Adds ":00" to a bare hour offset ("+03" -> "+03:00") after a time, because java.time needs it.
private val bareOffset = Regex("(\\d{2}:\\d{2}(?::\\d{2}(?:\\.\\d+)?)?)([+-]\\d{2})$")

/** Reads a server timestamp of any common shape. Returns null when it cannot be read. */
fun parseTimestamp(raw: Any?): Instant? {
    val text = raw?.toString()?.trim().orEmpty()
    if (text.isEmpty() || text == "null") return null
    val iso = bareOffset.replace(text.replace(' ', 'T')) { m -> m.groupValues[1] + m.groupValues[2] + ":00" }
    return runCatching { Instant.parse(iso) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(iso).toInstant() }.getOrNull()
        ?: runCatching { LocalDateTime.parse(iso).atZone(ZoneId.systemDefault()).toInstant() }.getOrNull()
        ?: runCatching { LocalDate.parse(iso.take(10)).atStartOfDay(ZoneId.systemDefault()).toInstant() }.getOrNull()
}

/** Whole hours since [created], never negative. Null when the timestamp cannot be read. */
fun ageHours(created: Any?, now: Instant = Instant.now()): Long? =
    parseTimestamp(created)?.let { Duration.between(it, now).toHours().coerceAtLeast(0L) }

/** Progress stages shown to the customer and the admin. [step] is the zero-based bar position. */
enum class OrderStage(val step: Int) {
    PAYMENT_PENDING(0),
    UNDER_REVIEW(2),
    COMPLETED(3),
    REJECTED(2)
}

/** Wallet purchases are paid from the balance at creation, so they never wait for a receipt. */
fun Order.isWalletOrder(): Boolean = paymentMethod.trim().equals("wallet", ignoreCase = true)

fun Order.hasReceipt(): Boolean = !receiptPath.isNullOrBlank()

/** Open order whose payment receipt is already uploaded. The admin must approve or reject it. */
fun Order.needsAdminAction(): Boolean = isOpenStatus(status) && hasReceipt()

/** Open manual-payment order with no receipt yet. The customer has to upload one. */
fun Order.waitingForCustomer(): Boolean = isOpenStatus(status) && !hasReceipt() && !isWalletOrder()

fun Order.stage(): OrderStage = when {
    statusTone(status) == StatusTone.SUCCESS -> OrderStage.COMPLETED
    statusTone(status) == StatusTone.ERROR -> OrderStage.REJECTED
    isOpenStatus(status) && !hasReceipt() && !isWalletOrder() -> OrderStage.PAYMENT_PENDING
    else -> OrderStage.UNDER_REVIEW
}

/** Oldest first. Orders with an unreadable date sort last. */
fun List<Order>.oldestFirst(): List<Order> = sortedBy { parseTimestamp(it.createdAt) ?: Instant.MAX }

/** Newest first. Orders with an unreadable date sort last. */
fun List<Order>.newestFirst(): List<Order> = sortedByDescending { parseTimestamp(it.createdAt) ?: Instant.MIN }

// ---- Transaction hash (TXID) conflicts -------------------------------------

/** Normalised transaction hash: trimmed, lower-case, without a 0x prefix. Null when blank. */
fun txidKey(raw: String?): String? =
    raw?.trim()?.lowercase()?.removePrefix("0x")?.takeIf { it.isNotEmpty() }

data class TxidRecord(val id: String, val status: String, val createdAt: Any?)

/**
 * [others] is how many other live records (not rejected) use this order's hash.
 * [blocking] is true when approving this order would reuse a hash that is already approved or
 * credited, or that was claimed by an earlier record.
 */
data class TxidConflict(val others: Int, val blocking: Boolean)

/** Groups every order and wallet deposit by its normalised transaction hash. */
fun indexTxids(orders: List<Order>, deposits: List<AdminWalletDeposit>): Map<String, List<TxidRecord>> {
    val map = HashMap<String, MutableList<TxidRecord>>()
    orders.forEach { o ->
        txidKey(o.transactionRef)?.let { key ->
            map.getOrPut(key) { mutableListOf() }.add(TxidRecord(o.id, o.status, o.createdAt))
        }
    }
    deposits.forEach { d ->
        txidKey(d.transactionHash)?.let { key ->
            map.getOrPut(key) { mutableListOf() }.add(TxidRecord(d.id, d.status, d.createdAt))
        }
    }
    return map
}

fun txidConflict(order: Order, index: Map<String, List<TxidRecord>>): TxidConflict {
    val key = txidKey(order.transactionRef) ?: return TxidConflict(0, false)
    val others = index[key].orEmpty().filter { it.id != order.id && statusTone(it.status) != StatusTone.ERROR }
    if (others.isEmpty()) return TxidConflict(0, false)
    val mine = parseTimestamp(order.createdAt) ?: Instant.MAX
    val blocking = others.any { other ->
        statusTone(other.status) == StatusTone.SUCCESS ||
            parseTimestamp(other.createdAt)?.isBefore(mine) == true
    }
    return TxidConflict(others.size, blocking)
}

// ---- Search ----------------------------------------------------------------

/** Converts Arabic-Indic and Persian digits to ASCII, so searches typed on an Arabic keyboard match. */
fun normalizeDigits(text: String): String = buildString(text.length) {
    for (c in text) {
        append(
            when (c) {
                in '\u0660'..'\u0669' -> '0' + (c - '\u0660')
                in '\u06F0'..'\u06F9' -> '0' + (c - '\u06F0')
                else -> c
            }
        )
    }
}

/** Matches order ID, customer e-mail, transaction hash, or phone number (digits compared loosely). */
fun orderMatchesSearch(order: Order, customerEmail: String, query: String): Boolean {
    val q = normalizeDigits(query.trim()).lowercase()
    if (q.isEmpty()) return true
    if (order.id.lowercase().contains(q)) return true
    if (customerEmail.lowercase().contains(q)) return true
    val txQuery = q.replace(" ", "").removePrefix("0x")
    if (txQuery.length >= 6 && order.transactionRef?.lowercase()?.contains(txQuery) == true) return true
    val digitsQuery = q.filter { it.isDigit() }.trimStart('0')
    if (digitsQuery.length >= 4) {
        val phoneDigits = normalizeDigits(order.phoneNumber.orEmpty()).filter { it.isDigit() }
        if (phoneDigits.contains(digitsQuery)) return true
    }
    return false
}

// ---- Stale orders ----------------------------------------------------------

/** Open orders older than [thresholdHours], oldest first, each paired with its age in hours. */
fun staleOpenOrders(orders: List<Order>, thresholdHours: Long, now: Instant = Instant.now()): List<Pair<Order, Long>> =
    orders.filter { isOpenStatus(it.status) }
        .mapNotNull { o -> ageHours(o.createdAt, now)?.let { h -> o to h } }
        .filter { (_, h) -> h >= thresholdHours }
        .sortedByDescending { (_, h) -> h }
