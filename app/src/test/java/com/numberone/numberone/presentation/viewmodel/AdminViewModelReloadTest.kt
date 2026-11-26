package com.numberone.numberone.presentation.viewmodel

import com.numberone.numberone.domain.model.AdminService
import com.numberone.numberone.domain.model.AppPaymentConfig
import com.numberone.numberone.domain.model.AdminShamCashTopUp
import com.numberone.numberone.domain.model.AdminSubscription
import com.numberone.numberone.domain.model.AdminSummary
import com.numberone.numberone.domain.model.AdminUser
import com.numberone.numberone.domain.model.AdminWalletDeposit
import com.numberone.numberone.domain.model.Order
import com.numberone.numberone.domain.model.WalletTransaction
import com.numberone.numberone.domain.repository.AdminRepository
import com.numberone.numberone.domain.repository.UserRepository
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
class AdminViewModelReloadTest {

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun users(isAdmin: Boolean) = stubOf<UserRepository> { name ->
        when (name) {
            "isAdmin" -> Result.success(isAdmin)
            else -> error("unexpected $name")
        }
    }

    /** Every list section succeeds with an empty list; [summaryFailure] replaces getSummary. */
    private fun admin(calls: MutableList<String>, summaryFailure: Throwable? = null) = stubOf<AdminRepository> { name ->
        calls += name
        when (name) {
            "getSummary" -> summaryFailure?.let { Result.failure<AdminSummary>(it) }
                ?: Result.success(AdminSummary(customers = 1, wallets = 1, deposits = 0, subscriptions = 0))
            "listShamCashTopUps" -> Result.success(emptyList<AdminShamCashTopUp>())
            "listOrders" -> Result.success(emptyList<Order>())
            "listDeposits" -> Result.success(emptyList<AdminWalletDeposit>())
            "listSubscriptions" -> Result.success(emptyList<AdminSubscription>())
            "listUsers" -> Result.success(emptyList<AdminUser>())
            "listWalletTransactions" -> Result.success(emptyList<WalletTransaction>())
            "listServices" -> Result.success(emptyList<AdminService>())
            "getPaymentConfig" -> Result.success(AppPaymentConfig())
            else -> error("unexpected $name")
        }
    }

    @Test fun non_admin_is_refused_before_any_section_loads() = runTest {
        val calls = mutableListOf<String>()
        val vm = AdminViewModel(admin(calls), users(isAdmin = false))

        vm.load()

        assertTrue(vm.error != null)
        assertTrue(calls.none { it.startsWith("list") || it == "getSummary" })
    }

    @Test fun when_every_section_loads_the_warning_is_cleared_and_summary_is_set() = runTest {
        val calls = mutableListOf<String>()
        val vm = AdminViewModel(admin(calls), users(isAdmin = true))

        vm.load()

        assertNull(vm.error)
        assertNull(vm.warning)
        assertEquals(AdminSummary(customers = 1, wallets = 1, deposits = 0, subscriptions = 0), vm.summary)
        assertFalse(vm.loading)
    }

    @Test fun one_failing_section_is_reported_and_does_not_block_the_rest() = runTest {
        val calls = mutableListOf<String>()
        val vm = AdminViewModel(admin(calls, summaryFailure = IOException("offline")), users(isAdmin = true))

        vm.load()

        // The write-failure banner stays clear: a read failure is a warning, not an error.
        assertNull(vm.error)
        assertFalse(vm.loading)
        // Every other section was still requested.
        assertTrue(calls.containsAll(listOf("listOrders", "listUsers", "listServices", "getPaymentConfig")))
        // The warning names the failing section and carries its classified reason.
        val warning = vm.warning.orEmpty()
        assertTrue(warning.contains("[NETWORK]"))
    }
}
