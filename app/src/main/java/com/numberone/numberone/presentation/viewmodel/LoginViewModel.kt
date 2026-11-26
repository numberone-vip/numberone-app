package com.numberone.numberone.presentation.viewmodel

import com.numberone.numberone.core.localization.tr
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.numberone.numberone.core.error.AppError
import com.numberone.numberone.core.error.ErrorMapper
import com.numberone.numberone.core.security.BiometricAuthManager
import com.numberone.numberone.di.AppContainer
import com.numberone.numberone.domain.repository.UserRepository
import com.numberone.numberone.notifications.PushTokenRegistrar
import kotlinx.coroutines.launch

private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

data class LoginUiState(
    val email: String = "", val password: String = "", val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false, val isLoginSuccessful: Boolean = false,
    /** Set once after a successful sign-up so the screen can send the user back to Sign-In. */
    val isRegistered: Boolean = false,
    val errorMessage: String? = null, val notice: String? = null
)

class LoginViewModel(
    private val userRepo: UserRepository = AppContainer.userRepository,
    /** Forgets the saved fingerprint credentials. Defaults to the Android-backed implementation. */
    private val clearSavedCredentials: () -> Unit = { BiometricAuthManager.clear(AppContainer.applicationContext()) },
    /** Runs after a successful sign-in (binds the device push token). */
    private val onSignedIn: suspend () -> Unit = { PushTokenRegistrar.registerCurrent() }
) : ViewModel() {
    var uiState by mutableStateOf(LoginUiState()); private set
    fun onEmailChange(v: String) { uiState = uiState.copy(email = v, errorMessage = null, notice = null) }
    fun onPasswordChange(v: String) { uiState = uiState.copy(password = v, errorMessage = null, notice = null) }
    fun onConfirmPasswordChange(v: String) { uiState = uiState.copy(confirmPassword = v, errorMessage = null, notice = null) }
    fun togglePasswordVisibility() { uiState = uiState.copy(isPasswordVisible = !uiState.isPasswordVisible) }
    /** Clears the one-shot success flag and drops the typed password from memory. */
    fun resetLoginSuccess() { uiState = uiState.copy(isLoginSuccessful = false, password = "") }
    fun consumeRegistered() { uiState = uiState.copy(isRegistered = false) }
    fun showError(message: String) { uiState = uiState.copy(errorMessage = message, notice = null) }
    fun showNotice(message: String) { uiState = uiState.copy(notice = message, errorMessage = null) }

    /** Called when the sign-up screen opens: start from a clean form but keep the typed email. */
    fun startSignUp() {
        uiState = uiState.copy(
            password = "", confirmPassword = "", isPasswordVisible = false,
            errorMessage = null, notice = null, isRegistered = false
        )
    }

    fun login() {
        if (uiState.isLoading) return
        val email = uiState.email.trim()
        val password = uiState.password
        if (email.isBlank() || password.isBlank()) {
            uiState = uiState.copy(errorMessage = tr("يرجى إدخال البريد الإلكتروني وكلمة المرور"), notice = null)
            return
        }
        performLogin(email, password, viaBiometric = false)
    }

    /** Sign-in with the email/password that the fingerprint just unlocked. */
    fun loginWithSavedCredentials(email: String, password: String) {
        if (uiState.isLoading) return
        uiState = uiState.copy(email = email, password = password)
        performLogin(email, password, viaBiometric = true)
    }

    private fun performLogin(email: String, password: String, viaBiometric: Boolean) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null, notice = null)
            val result = userRepo.signIn(email, password)
            val error = result.exceptionOrNull()
            // The saved password no longer works (changed elsewhere): forget it instead of retrying forever.
            if (viaBiometric && error != null) {
                if (ErrorMapper.classify(error) == AppError.InvalidCredentials) {
                    clearSavedCredentials()
                }
            }
            uiState = uiState.copy(
                isLoading = false,
                isLoginSuccessful = result.isSuccess,
                password = if (viaBiometric && result.isFailure) "" else uiState.password,
                errorMessage = if (result.isFailure) ErrorMapper.toUserMessage(error) else null
            )
            // Bind this device's FCM token to the new session right away (Home also retries).
            if (result.isSuccess) onSignedIn()
        }
    }

    fun signUp() {
        if (uiState.isLoading) return
        val email = uiState.email.trim()
        val password = uiState.password
        val problem = when {
            !EMAIL_REGEX.matches(email) -> tr("أدخل بريداً إلكترونياً صحيحاً")
            password.length < 8 -> tr("كلمة المرور يجب ألا تقل عن 8 أحرف")
            password != uiState.confirmPassword -> tr("كلمتا المرور غير متطابقتين")
            else -> null
        }
        if (problem != null) {
            uiState = uiState.copy(errorMessage = problem, notice = null)
            return
        }
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null, notice = null)
            val result = userRepo.signUp(email, password)
            if (result.isSuccess) {
                // Sign-up may open a session automatically; end it so the user lands on Sign-In.
                runCatching { userRepo.signOut() }
                uiState = LoginUiState(
                    email = email,
                    isRegistered = true,
                    notice = tr("تم إنشاء حسابك بنجاح. سجّل الدخول للمتابعة (وإن وصلتك رسالة تأكيد على بريدك فأكّده أولاً).")
                )
            } else {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = ErrorMapper.toUserMessage(result.exceptionOrNull())
                )
            }
        }
    }
}
