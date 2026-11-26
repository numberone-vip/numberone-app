package com.numberone.numberone.presentation.viewmodel

import com.numberone.numberone.core.error.ErrorMapper
import com.numberone.numberone.domain.model.UserProfile
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private val profile = UserProfile(id = "u-1", email = "a@b.co", phone = null)

    private fun users(signIn: Result<UserProfile>, calls: MutableList<String>) = stubOf<UserRepository> { name ->
        calls += name
        when (name) {
            "signIn" -> signIn
            else -> error("unexpected $name")
        }
    }

    @Test fun blank_form_is_rejected_without_calling_the_repository() = runTest {
        val calls = mutableListOf<String>()
        val vm = LoginViewModel(users(Result.success(profile), calls), {}, {})
        vm.login()
        assertTrue(vm.uiState.errorMessage != null)
        assertTrue(calls.isEmpty())
    }

    @Test fun successful_login_marks_success_and_runs_the_post_sign_in_hook() = runTest {
        val calls = mutableListOf<String>()
        var hookRuns = 0
        val vm = LoginViewModel(users(Result.success(profile), calls), {}, { hookRuns++ })
        vm.onEmailChange("a@b.co"); vm.onPasswordChange("secret-123")

        vm.login()

        assertTrue(vm.uiState.isLoginSuccessful)
        assertEquals(null, vm.uiState.errorMessage)
        assertFalse(vm.uiState.isLoading)
        assertEquals(1, hookRuns)
    }

    @Test fun failed_login_shows_the_classified_message_and_skips_the_hook() = runTest {
        val calls = mutableListOf<String>()
        var hookRuns = 0
        val failure = Exception("Invalid login credentials")
        val vm = LoginViewModel(users(Result.failure(failure), calls), {}, { hookRuns++ })
        vm.onEmailChange("a@b.co"); vm.onPasswordChange("wrong")

        vm.login()

        assertEquals(ErrorMapper.toUserMessage(failure), vm.uiState.errorMessage)
        assertFalse(vm.uiState.isLoginSuccessful)
        assertEquals(0, hookRuns)
    }

    @Test fun rejected_saved_credentials_are_forgotten() = runTest {
        val calls = mutableListOf<String>()
        var cleared = 0
        val vm = LoginViewModel(
            users(Result.failure(Exception("invalid_credentials")), calls),
            { cleared++ },
            {}
        )

        vm.loginWithSavedCredentials("a@b.co", "old-password")

        assertEquals(1, cleared)
        assertEquals("", vm.uiState.password)
    }

    @Test fun network_failure_with_saved_credentials_keeps_them() = runTest {
        val calls = mutableListOf<String>()
        var cleared = 0
        val vm = LoginViewModel(
            users(Result.failure(java.io.IOException("offline")), calls),
            { cleared++ },
            {}
        )

        vm.loginWithSavedCredentials("a@b.co", "pw")

        assertEquals(0, cleared)
    }
}
