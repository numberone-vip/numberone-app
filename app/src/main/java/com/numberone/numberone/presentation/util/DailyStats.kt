package com.numberone.numberone.presentation.util

import com.numberone.numberone.domain.model.Order
import java.time.LocalDate

/** One calendar day of admin activity. */
data class DayStat(val date: LocalDate, val revenue: Double, val orders: Int)

/**
 * Orders and revenue for the last [days] days ending at [today] (oldest first, always [days] entries).
 * An order counts toward its creation date (the first 10 characters of created_at, i.e. the stored UTC date);
 * it adds to revenue only when [countsAsRevenue] is true. Rows with an unreadable date are ignored.
 * Pure function: no UI, no network.
 */
fun dailyStats(
    orders: List<Order>,
    today: LocalDate,
    days: Int = 7,
    countsAsRevenue: (Order) -> Boolean
): List<DayStat> {
    require(days in 1..90) { "days must be between 1 and 90" }
    val start = today.minusDays((days - 1).toLong())
    val revenue = HashMap<LocalDate, Double>()
    val counts = HashMap<LocalDate, Int>()
    for (o in orders) {
        val d = runCatching { LocalDate.parse(o.createdAt.trim().take(10)) }.getOrNull() ?: continue
        if (d.isBefore(start) || d.isAfter(today)) continue
        counts[d] = (counts[d] ?: 0) + 1
        if (countsAsRevenue(o)) revenue[d] = (revenue[d] ?: 0.0) + o.price
    }
    return (0 until days).map { i ->
        val d = start.plusDays(i.toLong())
        DayStat(d, revenue[d] ?: 0.0, counts[d] ?: 0)
    }
}
