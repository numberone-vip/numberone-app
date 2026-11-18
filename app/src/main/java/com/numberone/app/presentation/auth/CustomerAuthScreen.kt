package com.numberone.app.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.numberone.app.data.auth.AuthRepository
import com.numberone.app.core.localization.AppLanguage
import kotlinx.coroutines.launch

@Composable
fun CustomerAuthScreen(
    authRepository: AuthRepository,
    onAuthenticated: (String) -> Unit,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val isArabic = language == AppLanguage.ARABIC

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Number One",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "YOUR NUMBERS. YOUR CONTROL.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isRegisterMode) if (isArabic) "إنشاء حساب زبون" else "Create customer account" else if (isArabic) "تسجيل دخول الزبون" else "Customer sign in",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isRegisterMode)
                        if (isArabic) "أنشئ حساباً بالبريد الإلكتروني وكلمة المرور لمتابعة طلباتك." else "Create an account with your email and password to manage your orders."
                    else
                        if (isArabic) "سجّل الدخول للوصول إلى المتجر وطلباتك واشتراكاتك." else "Sign in to access the store, orders, and subscriptions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        error = null
                        message = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (isArabic) "البريد الإلكتروني" else "Email address") },
                    singleLine = true,
                    enabled = !busy
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        error = null
                        message = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (isArabic) "كلمة المرور" else "Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    enabled = !busy
                )

                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (message != null) {
                    Text(message!!, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = {
                        scope.launch {
                            busy = true
                            error = null
                            message = null
                            if (isRegisterMode) {
                                authRepository.signUpCustomer(email, password)
                                    .onSuccess { signedIn ->
                                        if (signedIn) {
                                            val userId = authRepository.currentCustomerUserId()
                                            if (userId != null) onAuthenticated(userId)
                                            else error = if (isArabic) "تعذر قراءة حسابك بعد إنشاء الحساب." else "Could not load your account after registration."
                                        } else {
                                            message = if (isArabic) "تم إنشاء الحساب. افتح بريدك الإلكتروني وأكّد الحساب، ثم سجّل الدخول." else "Account created. Check your inbox to confirm your email, then sign in."
                                            isRegisterMode = false
                                        }
                                    }
                                    .onFailure { error = it.localizedMessage ?: if (isArabic) "تعذر إنشاء الحساب." else "Could not create account." }
                            } else {
                                authRepository.signInCustomer(email, password)
                                    .onSuccess { userId -> onAuthenticated(userId) }
                                    .onFailure { error = it.localizedMessage ?: if (isArabic) "تعذر تسجيل الدخول." else "Could not sign in." }
                            }
                            busy = false
                        }
                    },
                    enabled = !busy && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (busy) CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        strokeWidth = 2.dp
                    ) else Text(if (isRegisterMode) if (isArabic) "إنشاء الحساب" else "Create account" else if (isArabic) "تسجيل الدخول" else "Sign in")
                }

                if (!isRegisterMode) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                busy = true
                                error = null
                                message = null
                                authRepository.sendCustomerPasswordReset(email)
                                    .onSuccess { message = if (isArabic) "إذا كان البريد مسجلاً، ستصلك رسالة لإعادة تعيين كلمة المرور." else "If the email is registered, a password reset message will be sent." }
                                    .onFailure { error = it.localizedMessage ?: if (isArabic) "تعذر إرسال رسالة الاستعادة." else "Could not send the recovery email." }
                                busy = false
                            }
                        },
                        enabled = !busy && email.isNotBlank(),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(if (isArabic) "نسيت كلمة المرور؟" else "Forgot password?")
                    }
                }

                TextButton(
                    onClick = {
                        isRegisterMode = !isRegisterMode
                        error = null
                        message = null
                    },
                    enabled = !busy,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        if (isRegisterMode) if (isArabic) "لديك حساب؟ سجّل الدخول" else "Already have an account? Sign in"
                        else if (isArabic) "ليس لديك حساب؟ أنشئ حساباً جديداً" else "New here? Create an account"
                    )
                }
            }
        }
    }
}
