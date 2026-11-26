package com.numberone.numberone.core.session

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionEventsTest {
    @After fun reset() { SessionEvents.acknowledge() }

    @Test fun marking_then_acknowledging_resets_the_signal() {
        SessionEvents.markExpired()
        assertTrue(SessionEvents.expired.value)
        SessionEvents.acknowledge()
        assertFalse(SessionEvents.expired.value)
    }
}
