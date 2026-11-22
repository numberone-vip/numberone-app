package com.numberone.numberone.presentation.util

import com.numberone.numberone.domain.model.Order
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DailyStatsTest {
    private val today = LocalDate.of(2026, 11, 21)

    private fun order(createdAt: String, price: Double, status: String = "APPROVED") =
        Order(createdAt = createdAt, price = price, status = status)

    private val isApproved: (Order) -> Boolean = { it.status == "APPROVED" }

    @Test fun alwaysReturnsRequestedNumberOfDays_oldestFirst() {
        val stats = dailyStats(emptyList(), today, 7, isApproved)
        assertEquals(7, stats.size)
        assertEquals(LocalDate.of(2026, 11, 15), stats.first().date)
        assertEquals(today, stats.last().date)
        assertEquals(0.0, stats.sumOf { it.revenue }, 1e-9)
    }

    @Test fun sumsRevenueAndCountsPerDay() {
        val orders = listOf(
            order("2026-11-21T08:00:00Z", 5.0),
            order("2026-11-21T09:30:00Z", 2.5),
            order("2026-11-21T10:00:00Z", 9.0, status = "REJECTED"),
            order("2026-11-20T23:59:59Z", 4.0)
        )
        val stats = dailyStats(orders, today, 7, isApproved)
        val last = stats.last()
        assertEquals(7.5, last.revenue, 1e-9)
        assertEquals(3, last.orders)
        val yesterday = stats[stats.size - 2]
        assertEquals(4.0, yesterday.revenue, 1e-9)
        assertEquals(1, yesterday.orders)
    }

    @Test fun ignoresOutOfRangeAndUnreadableDates() {
        val orders = listOf(
            order("2026-11-14T10:00:00Z", 100.0),
            order("2026-11-22T10:00:00Z", 100.0),
            order("garbage", 100.0),
            order("", 100.0)
        )
        val stats = dailyStats(orders, today, 7, isApproved)
        assertEquals(0, stats.sumOf { it.orders })
    }
}
