package com.numberone.numberone.presentation.viewmodel

import com.numberone.numberone.core.error.ErrorMapper
import com.numberone.numberone.domain.model.AppPaymentConfig
import com.numberone.numberone.domain.model.Order
import com.numberone.numberone.domain.model.Service
import com.numberone.numberone.domain.model.Wallet
import com.numberone.numberone.domain.model.WalletTopUp
import com.numberone.numberone.domain.model.WalletTransaction
import com.numberone.numberone.domain.repository.SubscriptionRepository
import com.numberone.numberone.domain.repository.UserRepository
import com.numberone.numberone.domain.repository.WalletRepository
import com.numberone.numberone.support.stubOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private val unusedSubscriptions = stubOf<SubscriptionRepository> { name -> error("unexpected $name") }
    private val unusedUsers = stubOf<UserRepository> { name -> error("unexpected $name") }

    @Test fun failed_refresh_shows_the_classified_message_and_stops_loading() = runTest {
        val wallet = stubOf<WalletRepository> { name ->
            when (name) {
                "getWallet" -> Result.failure<Wallet>(IOException("offline"))
                else -> error("unexpected $name")
            }
        }
        val vm = AppViewModel(wallet, unusedSubscriptions, unusedUsers)

        vm.refresh()

        assertEquals(ErrorMapper.toUserMessage(IOException("offline")), vm.error)
        assertFalse(vm.loading)
    }

    @Test fun successful_refresh_loads_every_section_without_error() = runTest {
        val wallet = stubOf<WalletRepository> { name ->
            when (name) {
                "getWallet" -> Result.success(Wallet())
                "getTransactions" -> Result.success(emptyList<WalletTransaction>())
                "getMyTopUpRequests" -> Result.success(emptyList<WalletTopUp>())
                "getServices" -> Result.success(emptyList<Service>())
                "getMyOrders" -> Result.success(emptyList<Order>())
                "getPaymentConfig" -> Result.success<AppPaymentConfig?>(null)
                else -> error("unexpected $name")
            }
        }
        val vm = AppViewModel(wallet, unusedSubscriptions, unusedUsers)

        vm.refresh()

        assertNull(vm.error)
        assertFalse(vm.loading)
        assertEquals(Wallet(), vm.wallet)
        assertTrue(vm.services.isEmpty())
    }
}
