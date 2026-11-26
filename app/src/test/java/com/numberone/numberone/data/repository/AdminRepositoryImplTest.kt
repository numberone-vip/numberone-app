package com.numberone.numberone.data.repository

import com.numberone.numberone.core.error.AppError
import com.numberone.numberone.core.error.AppException
import com.numberone.numberone.data.remote.AdminRpc
import kotlinx.serialization.json.JsonObject
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class AdminRepositoryImplTest {

    /** Returns [response] for every RPC, or throws [failure] when set. Records RPC names. */
    private class FakeRpc : AdminRpc {
        var response: String = ""
        var failure: Throwable? = null
        val calls = mutableListOf<String>()

        override suspend fun rpcRaw(function: String, params: JsonObject?): String {
            calls += function
            failure?.let { throw it }
            return response
        }
    }

    private fun appErrorOf(result: Result<*>): AppError {
        val e = result.exceptionOrNull()
        assertTrue("expected failure", e != null)
        return (e as AppException).error
    }

    @Test fun blank_service_id_is_rejected_before_the_rpc() = runTest {
        val rpc = FakeRpc()
        assertFalse(AdminRepositoryImpl(rpc).deleteService(" ").isSuccess)
        assertTrue(rpc.calls.isEmpty())
    }

    @Test fun delete_service_calls_the_expected_rpc() = runTest {
        val rpc = FakeRpc()
        assertTrue(AdminRepositoryImpl(rpc).deleteService("svc-1").isSuccess)
        assertEquals(listOf("admin_delete_service"), rpc.calls)
    }

    @Test fun foreign_key_violation_becomes_service_in_use_without_exposing_sql() = runTest {
        val rpc = FakeRpc().apply { failure = IllegalStateException("violates foreign key constraint \"orders_service_fkey\"") }
        val result = AdminRepositoryImpl(rpc).deleteService("svc-1")
        assertEquals(AppError.ServiceInUse, appErrorOf(result))
        assertFalse(result.exceptionOrNull()?.message.orEmpty().contains("orders_service_fkey"))
    }

    @Test fun other_delete_failures_keep_their_real_classification() = runTest {
        val rpc = FakeRpc().apply { failure = IOException("timeout") }
        val result = AdminRepositoryImpl(rpc).deleteService("svc-1")
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test fun undecodable_list_response_is_a_typed_server_error_naming_no_rpc() = runTest {
        val rpc = FakeRpc().apply { response = "{not a list" }
        val result = AdminRepositoryImpl(rpc).listOrders()
        assertEquals(AppError.ServerResponse, appErrorOf(result))
        assertFalse(result.exceptionOrNull()?.message.orEmpty().contains("admin_list_orders"))
    }

    @Test fun null_list_response_is_an_empty_list() = runTest {
        val rpc = FakeRpc().apply { response = "null" }
        val result = AdminRepositoryImpl(rpc).listOrders()
        assertTrue(result.getOrThrow().isEmpty())
    }
}
