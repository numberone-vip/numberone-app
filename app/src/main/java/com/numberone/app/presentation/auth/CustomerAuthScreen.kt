package com.numberone.app.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun CustomerAuthScreen(
    email: String,
    password: String,
    isSignUp: Boolean,
    loading: Boolean,
    error: String?,
    message: String?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleMode: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Number One",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (isSignUp) "إنشاء حساب عميل جديد" else "تسجيل دخول العميل",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("البريد الإلكتروني") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("كلمة المرور (8 أحرف على الأقل)") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        if (message != null) {
            Spacer(Modifier.height(12.dp))
            Text(message, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onSubmit,
            enabled = !loading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) CircularProgressIndicator()
            else Text(if (isSignUp) "إنشاء الحساب" else "دخول")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onToggleMode,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSignUp) "لدي حساب بالفعل — تسجيل الدخول" else "ليس لدي حساب — إنشاء حساب")
        }
    }
}
