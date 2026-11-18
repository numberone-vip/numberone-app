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
import kotlinx.coroutines.launch

@Composable
fun CustomerAuthScreen(
    authRepository: AuthRepository,
    onAuthenticated: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

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
                    text = if (isRegisterMode) "إنشاء حساب زبون" else "تسجيل دخول الزبون",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isRegisterMode)
                        "أنشئ حساباً بالبريد الإلكتروني وكلمة المرور لمتابعة طلباتك."
                    else
                        "سجّل الدخول للوصول إلى المتجر وطلباتك واشتراكاتك.",
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
                    label = { Text("البريد الإلكتروني") },
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
                    label = { Text("كلمة المرور") },
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
                                            else error = "تعذر قراءة حسابك بعد إنشاء الحساب."
                                        } else {
                                            message = "تم إنشاء الحساب. افتح بريدك الإلكتروني وأكّد الحساب، ثم سجّل الدخول."
                                            isRegisterMode = false
                                        }
                                    }
                                    .onFailure { error = it.localizedMessage ?: "تعذر إنشاء الحساب." }
                            } else {
                                authRepository.signInCustomer(email, password)
                                    .onSuccess { userId -> onAuthenticated(userId) }
                                    .onFailure { error = it.localizedMessage ?: "تعذر تسجيل الدخول." }
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
                    ) else Text(if (isRegisterMode) "إنشاء الحساب" else "تسجيل الدخول")
                }

                if (!isRegisterMode) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                busy = true
                                error = null
                                message = null
                                authRepository.sendCustomerPasswordReset(email)
                                    .onSuccess { message = "إذا كان البريد مسجلاً، ستصلك رسالة لإعادة تعيين كلمة المرور." }
                                    .onFailure { error = it.localizedMessage ?: "تعذر إرسال رسالة الاستعادة." }
                                busy = false
                            }
                        },
                        enabled = !busy && email.isNotBlank(),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("نسيت كلمة المرور؟")
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
                        if (isRegisterMode) "لديك حساب؟ سجّل الدخول"
                        else "ليس لديك حساب؟ أنشئ حساباً جديداً"
                    )
                }
            }
        }
    }
}
