package com.numberone.numberone.presentation.screens.admin

import com.numberone.numberone.core.components.StatusTone
import com.numberone.numberone.core.components.statusTone
import com.numberone.numberone.domain.model.AdminShamCashTopUp
import com.numberone.numberone.domain.model.AdminWalletDeposit
import com.numberone.numberone.domain.model.Order
import com.numberone.numberone.presentation.util.parseTimestamp
import java.time.LocalDate
import java.time.ZoneId

/** Deposits and sales for one period, in USDT. */
internal data class Totals(val deposits: Double = 0.0, val sales: Double = 0.0) {
    operator fun plus(other: Totals): Totals = Totals(deposits + other.deposits, sales + other.sales)
}

private fun dayOf(raw: Any?): LocalDate? =
    parseTimestamp(raw)?.atZone(ZoneId.systemDefault())?.toLocalDate()

/**
 * Credited amounts grouped by local calendar day, computed from the records already loaded
 * by the admin console (no extra server call):
 * - wallet deposits: credited (approved) deposits, dated by credit time;
 * - sham cash top-ups: approved top-ups, dated by creation time;
 * - sales: completed orders, dated by creation time.
 */
internal fun dailyTotals(
    orders: List<Order>,
    deposits: List<AdminWalletDeposit>,
    sham: List<AdminShamCashTopUp>
): Map<LocalDate, Totals> {
    val map = HashMap<LocalDate, Totals>()
    fun add(day: LocalDate?, t: Totals) {
        if (day != null) map[day] = (map[day] ?: Totals()) + t
    }
    orders.filter { statusTone(it.status) == StatusTone.SUCCESS }
        .forEach { add(dayOf(it.createdAt), Totals(sales = it.price.toDouble())) }
    deposits.filter { statusTone(it.status) == StatusTone.SUCCESS }
        .forEach { add(dayOf(it.creditedAt ?: it.createdAt), Totals(deposits = it.verifiedAmount ?: it.requestedAmount)) }
    sham.filter { statusTone(it.status) == StatusTone.SUCCESS }
        .forEach { add(dayOf(it.createdAt), Totals(deposits = it.creditedUsdt?.toDouble() ?: 0.0)) }
    return map
}

/** Sum of the day totals whose date satisfies [predicate]. */
internal fun totalsWhere(daily: Map<LocalDate, Totals>, predicate: (LocalDate) -> Boolean): Totals =
    daily.entries.filter { predicate(it.key) }.fold(Totals()) { acc, e -> acc + e.value }
