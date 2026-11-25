package com.numberone.numberone.data.repository

import com.numberone.numberone.core.error.AppError
import com.numberone.numberone.core.error.AppException
import com.numberone.numberone.core.error.ErrorMapper
import com.numberone.numberone.data.remote.FiveSimGateway
import com.numberone.numberone.domain.model.VirtualNumberCatalog
import com.numberone.numberone.domain.model.VirtualNumberOrder
import com.numberone.numberone.domain.model.VirtualNumberQuote
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class VirtualNumberRepositoryImplTest {

    /** Scripted gateway: each call either returns [order] or throws [failure]. */
    private class FakeGateway : FiveSimGateway {
        var failure: Throwable? = null
        var order = VirtualNumberOrder(activationId = "act-1", phone = "+15550001")
        var purchaseCalls = 0
        var pollCalls = 0

        private fun maybeThrow() { failure?.let { throw it } }

        override suspend fun catalog() = maybeThrow().let { VirtualNumberCatalog() }
        override suspend fun quote(countryCode: String, serviceCode: String) = maybeThrow().let {
            VirtualNumberQuote(countryCode = countryCode, serviceCode = serviceCode)
        }
        override suspend fun purchase(countryCode: String, serviceCode: String, operator: String, requestId: String): VirtualNumberOrder {
            purchaseCalls++; maybeThrow(); return order
        }
        override suspend fun poll(activationId: String): VirtualNumberOrder {
            pollCalls++; maybeThrow(); return order
        }
        override suspend fun latestActivation(): VirtualNumberOrder? { maybeThrow(); return order }
        override suspend fun requestNextSms(activationId: String): VirtualNumberOrder { maybeThrow(); return order }
        override suspend fun cancel(activationId: String): VirtualNumberOrder { maybeThrow(); return order }
        override suspend fun registerDeviceToken(token: String) { maybeThrow() }
    }

    private val validRequestId = "123e4567-e89b-12d3-a456-426614174000"

    private fun errorOf(result: Result<*>): AppError {
        val e = result.exceptionOrNull()
        assertTrue("expected a failure", e != null)
        return (e as AppException).error
    }

    @Test fun successful_catalog_is_returned_unchanged() = runTest {
        val repo = VirtualNumberRepositoryImpl(FakeGateway())
        assertTrue(repo.catalog().isSuccess)
    }

    @Test fun purchase_with_malformed_request_id_never_reaches_the_gateway() = runTest {
        val gateway = FakeGateway()
        val result = VirtualNumberRepositoryImpl(gateway).purchase("ru", "telegram", "any", "not-a-uuid")
        assertFalse(result.isSuccess)
        assertEquals(0, gateway.purchaseCalls)
    }

    @Test fun purchase_with_blank_operator_is_rejected_before_the_network() = runTest {
        val gateway = FakeGateway()
        val result = VirtualNumberRepositoryImpl(gateway).purchase("ru", "telegram", " ", validRequestId)
        assertFalse(result.isSuccess)
        assertEquals(0, gateway.purchaseCalls)
    }

    @Test fun valid_purchase_reaches_the_gateway_once() = runTest {
        val gateway = FakeGateway()
        val result = VirtualNumberRepositoryImpl(gateway).purchase("ru", "telegram", "any", validRequestId)
        assertTrue(result.isSuccess)
        assertEquals(1, gateway.purchaseCalls)
    }

    @Test fun network_failure_is_wrapped_as_typed_error_keeping_its_cause() = runTest {
        val gateway = FakeGateway().apply { failure = IOException("timeout") }
        val result = VirtualNumberRepositoryImpl(gateway).poll("act-1")
        assertEquals(AppError.Network, errorOf(result))
        assertTrue(result.exceptionOrNull()?.cause is IOException)
    }

    @Test fun provider_stock_message_maps_to_no_stock() = runTest {
        val gateway = FakeGateway().apply { failure = IllegalStateException("no stock for telegram") }
        assertEquals(AppError.NoStock, errorOf(VirtualNumberRepositoryImpl(gateway).quote("ru", "tg")))
    }

    @Test fun unconfirmed_payment_maps_to_pending_review_not_a_generic_error() = runTest {
        val gateway = FakeGateway().apply { failure = IllegalStateException("payment not confirmed") }
        assertEquals(AppError.PendingReview, errorOf(VirtualNumberRepositoryImpl(gateway).purchase("ru", "tg", "any", validRequestId)))
    }

    @Test fun failure_message_shown_to_user_is_the_classified_text() = runTest {
        val gateway = FakeGateway().apply { failure = IOException("socket") }
        val result = VirtualNumberRepositoryImpl(gateway).cancel("act-1")
        assertEquals(ErrorMapper.toUserMessage(result.exceptionOrNull()), ErrorMapper.toUserMessage(AppException(AppError.Network)))
    }

    @Test fun cancellation_is_rethrown_so_polling_stops_when_screen_leaves() = runTest {
        val gateway = FakeGateway().apply { failure = CancellationException("screen left") }
        try {
            VirtualNumberRepositoryImpl(gateway).poll("act-1")
            fail("CancellationException must propagate")
        } catch (e: CancellationException) {
            // expected
        }
    }

    @Test fun blank_activation_id_is_rejected_for_poll() = runTest {
        val gateway = FakeGateway()
        assertFalse(VirtualNumberRepositoryImpl(gateway).poll("").isSuccess)
        assertEquals(0, gateway.pollCalls)
    }
}
