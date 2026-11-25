package com.numberone.numberone.presentation.viewmodel

import com.numberone.numberone.core.error.AppError
import com.numberone.numberone.core.error.AppException
import com.numberone.numberone.core.error.ErrorMapper
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

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModelTest {

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private val unusedUsers = stubOf<UserRepository> { name -> error("unexpected $name") }

    @Test fun showing_a_missing_receipt_fails_without_calling_the_repository() = runTest {
        var calls = 0
        val admin = stubOf<AdminRepository> { _ -> calls++; error("must not be called") }
        val vm = AdminViewModel(admin, unusedUsers)

        vm.showReceipt(null)

        assertTrue(vm.error != null)
        assertNull(vm.receiptUrl)
        assertEquals(0, calls)
    }

    @Test fun receipt_url_is_stored_when_the_repository_returns_it() = runTest {
        val admin = stubOf<AdminRepository> { name ->
            when (name) {
                "getReceiptUrl" -> Result.success("https://signed.example/r.png")
                else -> error("unexpected $name")
            }
        }
        val vm = AdminViewModel(admin, unusedUsers)

        vm.showReceipt("receipts/1.png")

        assertEquals("https://signed.example/r.png", vm.receiptUrl)
        assertNull(vm.error)
    }

    @Test fun failed_delete_shows_the_friendly_message_with_its_category_code() = runTest {
        val admin = stubOf<AdminRepository> { name ->
            when (name) {
                "deleteService" -> Result.failure<Unit>(AppException(AppError.ServiceInUse))
                else -> error("unexpected $name")
            }
        }
        val vm = AdminViewModel(admin, unusedUsers)

        vm.deleteService("svc-1")

        val shown = vm.error.orEmpty()
        assertTrue(shown.startsWith(ErrorMapper.toUserMessage(AppException(AppError.ServiceInUse))))
        assertTrue(shown.contains("[BUSINESS]"))
        assertFalse(vm.loading)
    }
}
