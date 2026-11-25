package com.numberone.numberone.presentation.viewmodel

import com.numberone.numberone.core.error.ErrorMapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
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
class FeedbackViewModelTest {

    private class Plain : FeedbackViewModel() {
        fun exec(task: suspend () -> Unit) = run(task)
    }

    private class Custom : FeedbackViewModel() {
        override fun describeError(e: Throwable): String = "custom:" + e::class.simpleName
        fun exec(task: suspend () -> Unit) = run(task)
    }

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun successful_task_leaves_no_error_and_stops_loading() = runTest {
        val vm = Plain()
        var ran = false
        vm.exec { ran = true }
        assertTrue(ran)
        assertFalse(vm.loading)
        assertNull(vm.error)
    }

    @Test fun failing_task_sets_the_described_error() = runTest {
        val vm = Plain()
        vm.exec { throw IOException("socket closed") }
        assertEquals(ErrorMapper.toUserMessage(IOException("socket closed")), vm.error)
        assertFalse(vm.loading)
    }

    @Test fun subclass_can_change_how_errors_are_described() = runTest {
        val vm = Custom()
        vm.exec { throw IllegalStateException("x") }
        assertEquals("custom:IllegalStateException", vm.error)
    }

    @Test fun new_task_is_ignored_while_one_is_in_flight() = runTest {
        val vm = Plain()
        val gate = CompletableDeferred<Unit>()
        var starts = 0
        vm.exec { starts++; gate.await() }
        assertTrue(vm.loading)
        vm.exec { starts++ }
        assertEquals(1, starts)
        gate.complete(Unit)
        assertFalse(vm.loading)
    }

    @Test fun cancellation_is_not_shown_as_an_error() = runTest {
        val vm = Plain()
        vm.exec { throw CancellationException("leaving screen") }
        assertNull(vm.error)
        assertFalse(vm.loading)
    }

    @Test fun a_new_task_clears_the_previous_error() = runTest {
        val vm = Plain()
        vm.exec { throw IOException("first") }
        assertTrue(vm.error != null)
        vm.exec { }
        assertNull(vm.error)
    }
}
