package com.numberone.numberone.data.remote

import com.numberone.numberone.domain.model.AdminAuditEntry
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RpcResultsTest {

    // ---- parseNumberResult -------------------------------------------------

    @Test fun number_bareValue() = assertEquals(12.5, parseNumberResult("rpc", "12.5"), 1e-9)

    @Test fun number_oneRowArray() = assertEquals(7.0, parseNumberResult("rpc", "[7]"), 1e-9)

    @Test fun number_objectPrefersBalanceKey() {
        val raw = """{"amount": 1.0, "new_balance": 42.0}"""
        assertEquals(42.0, parseNumberResult("rpc", raw), 1e-9)
    }

    @Test fun number_arrayOfObjects() {
        val raw = """[{"balance_after": 3.25}]"""
        assertEquals(3.25, parseNumberResult("rpc", raw), 1e-9)
    }

    @Test fun number_unexpectedShapeThrowsWithRpcName() {
        try {
            parseNumberResult("admin_adjust_wallet_balance", """{"ok": true}""")
            fail("expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message.orEmpty().startsWith("admin_adjust_wallet_balance"))
        }
    }

    @Test fun number_garbageThrows() {
        try {
            parseNumberResult("rpc", "not json")
            fail("expected IllegalStateException")
        } catch (_: IllegalStateException) {
        }
    }

    // ---- parseMarginOrNull -------------------------------------------------

    @Test fun margin_bareValue() = assertEquals(15.0, parseMarginOrNull("15")!!, 1e-9)

    @Test fun margin_objectPrefersMarginKey() {
        val raw = """{"id": 1, "margin_percent": 20.5}"""
        assertEquals(20.5, parseMarginOrNull(raw)!!, 1e-9)
    }

    @Test fun margin_oneRowTable() {
        val raw = """[{"margin_percent": 8}]"""
        assertEquals(8.0, parseMarginOrNull(raw)!!, 1e-9)
    }

    @Test fun margin_noNumberIsNull() {
        assertNull(parseMarginOrNull("null"))
        assertNull(parseMarginOrNull("""{"ok": true}"""))
        assertNull(parseMarginOrNull(""))
    }

    // ---- parseBooleanResult ------------------------------------------------

    @Test fun boolean_explicitValues() {
        assertTrue(parseBooleanResult("true"))
        assertFalse(parseBooleanResult("false"))
    }

    @Test fun boolean_emptyBodyCountsAsSuccess() {
        assertTrue(parseBooleanResult(""))
        assertTrue(parseBooleanResult("null"))
    }

    // ---- Serialization contract of the audit-log RPC ------------------------

    @Test fun auditEntry_decodesRowsWithNullsAndUnknownColumns() {
        val raw = """
            [{"id": 5, "admin_id": "11111111-1111-1111-1111-111111111111", "admin_email": null,
              "action": "order_approved", "target_id": "abc", "details": null,
              "created_at": "2026-11-21T10:00:00Z", "future_column": 1}]
        """.trimIndent()
        val rows = rpcJson.decodeFromString(ListSerializer(AdminAuditEntry.serializer()), raw)
        assertEquals(1, rows.size)
        assertEquals(5L, rows[0].id)
        assertNull(rows[0].adminEmail)
        assertEquals("order_approved", rows[0].action)
        assertNull(rows[0].details)
    }
}
