package com.numberone.numberone.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {

    @Test fun adminUser_roleIsCaseInsensitive() {
        assertTrue(AdminUser(userId = "u", role = "Admin").isAdmin)
        assertTrue(AdminUser(userId = "u", role = " admin ").isAdmin)
        assertFalse(AdminUser(userId = "u", role = "User").isAdmin)
        assertFalse(AdminUser(userId = "u", role = null).isAdmin)
    }

    @Test fun isOpenStatus_terminalStatusesAreClosed() {
        listOf("approved", "credited", "completed", "rejected", "cancelled", "expired", "active")
            .forEach { assertFalse(it, isOpenStatus(it)) }
        assertFalse(isOpenStatus("  APPROVED "))
    }

    @Test fun isOpenStatus_reviewStatusesAreOpen() {
        assertTrue(isOpenStatus("PAYMENT_REVIEW"))
        assertTrue(isOpenStatus("pending"))
    }

    private val operator = VirtualNumberOperator(name = "any", availableQty = 1, basePrice = 2.0)

    @Test fun quote_percentMargin() {
        val q = VirtualNumberQuote(countryCode = "us", serviceCode = "tg", marginMode = "percent", marginPercent = 25.0)
        assertEquals(2.5, q.finalPrice(operator), 1e-9)
    }

    @Test fun quote_fixedMargin() {
        val q = VirtualNumberQuote(countryCode = "us", serviceCode = "tg", marginMode = "FIXED", marginFixed = 0.75)
        assertEquals(2.75, q.finalPrice(operator), 1e-9)
    }

    @Test fun quote_zeroMarginKeepsBasePrice() {
        val q = VirtualNumberQuote(countryCode = "us", serviceCode = "tg")
        assertEquals(2.0, q.finalPrice(operator), 1e-9)
    }
}
