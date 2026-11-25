package com.numberone.numberone.data.repository

import com.numberone.numberone.core.error.AppError
import com.numberone.numberone.core.error.ErrorMapper
import com.numberone.numberone.data.remote.WalletDataSource
import com.numberone.numberone.domain.model.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class WalletRepositoryImplTest {

    /** Records the user ids it receives; only the methods exercised here are implemented. */
    private class FakeWalletDataSource : WalletDataSource {
        var failure: Throwable? = null
        var walletCalls = 0
        var lastUserId: String? = null

        override suspend fun getWallet(userId: String): Wallet {
            walletCalls++; lastUserId = userId; failure?.let { throw it }; return Wallet()
        }
        override suspend fun getTransactions(userId: String): List<WalletTransaction> = emptyList()
        override suspend fun createTopUpRequest(userId: String, request: CreateWalletTopUp): WalletTopUp = unsupported()
        override suspend fun getMyTopUpRequests(userId: String): List<WalletTopUp> = emptyList()
        override suspend fun submitDepositTransaction(requestId: String, transactionHash: String): Boolean = unsupported()
        override suspend fun verifyDeposit(requestId: String, transactionHash: String): String = unsupported()
        override suspend fun getServices(): List<Service> = unsupported()
        override suspend fun getMyOrders(userId: String): List<Order> = unsupported()
        override suspend fun createOrder(serviceId: String): PaymentOrderCreation = unsupported()
        override suspend fun uploadReceipt(orderId: String, bytes: ByteArray, mimeType: String, extension: String): String = unsupported()
        override suspend fun submitOrderPayment(orderId: String, transactionHash: String, receiptPath: String): Boolean = unsupported()
        override suspend fun purchaseServiceWithWallet(serviceId: String, requestId: String): WalletOrderResult = unsupported()
        override suspend fun createShamCashTopUp(paymentMethod: String, paidAmount: Double): String = unsupported()
        override suspend fun submitShamCashReceipt(requestId: String, bytes: ByteArray, mimeType: String, extension: String): Boolean = unsupported()
        override suspend fun getMyShamCashTopUps(): List<ShamCashTopUp> = unsupported()
        override suspend fun getPaymentConfig(): AppPaymentConfig = unsupported()

        private fun unsupported(): Nothing = throw UnsupportedOperationException("not used by this test")
    }

    @Test fun missing_session_fails_before_the_data_source_is_called() = runTest {
        val ds = FakeWalletDataSource()
        val result = WalletRepositoryImpl(ds) { null }.getWallet()
        assertFalse(result.isSuccess)
        assertEquals(0, ds.walletCalls)
        assertEquals(AppError.Localized::class, ErrorMapper.classify(result.exceptionOrNull())::class)
    }

    @Test fun signed_in_user_id_is_passed_to_the_data_source() = runTest {
        val ds = FakeWalletDataSource()
        val result = WalletRepositoryImpl(ds) { "u-1" }.getWallet()
        assertTrue(result.isSuccess)
        assertEquals("u-1", ds.lastUserId)
    }

    @Test fun network_failure_from_the_data_source_is_classified() = runTest {
        val ds = FakeWalletDataSource().apply { failure = IOException("offline") }
        val result = WalletRepositoryImpl(ds) { "u-1" }.getWallet()
        assertEquals(AppError.Network, ErrorMapper.classify(result.exceptionOrNull()))
    }
}
