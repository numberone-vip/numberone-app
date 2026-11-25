package com.numberone.numberone.core.log

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AppLogTest {
    private val original = AppLog.sink
    private val seen = mutableListOf<String>()

    @Before fun install() {
        AppLog.sink = { level, tag, message, t -> seen += "$level|$tag|$message|${t?.message}" }
    }

    @After fun restore() { AppLog.sink = original }

    @Test fun warn_and_error_are_forwarded_with_their_level_and_cause() {
        AppLog.warn("Push", "skipped", IllegalStateException("no token"))
        AppLog.error("AdminRepo", "bad shape")
        assertEquals(
            listOf("WARN|Push|skipped|no token", "ERROR|AdminRepo|bad shape|null"),
            seen
        )
    }
}
