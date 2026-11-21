package com.numberone.numberone.presentation.screens.auth

import com.numberone.numberone.core.localization.tr
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.numberone.numberone.presentation.viewmodel.LoginViewModel

@Composable
fun SignupScreen(
    viewModel: LoginViewModel,
    onBack: () -> Unit,
    onRegistered: () -> Unit
) {
    val state = viewModel.uiState
    val palette = authPalette()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) { viewModel.startSignUp() }

    // Straight back to Sign-In as soon as the account is created.
    LaunchedEffect(state.isRegistered) {
        if (state.isRegistered) {
            viewModel.consumeRegistered()
            onRegistered()
        }
    }

    val passwordTransformation =
        if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation()
    val mismatch = state.confirmPassword.isNotEmpty() && state.confirmPassword != state.password
    val matches = state.confirmPassword.isNotEmpty() && state.confirmPassword == state.password
    val emailTaken = state.errorMessage?.let {
        it.contains("مسجل مسبقاً") || it.contains("already registered", ignoreCase = true)
    } == true

    AuthScaffold(
        palette = palette,
        headerHeight = 232.dp,
        onBack = onBack,
        header = {
            Text(
                text = tr("إنشاء حساب جديد"),
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = tr("ابدأ بإدارة أرقامك واشتراكاتك في دقائق"),
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    ) {
        Text(
            text = tr("أنشئ حسابك"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = palette.onCard
        )

        AuthTextField(
            palette = palette,
            label = tr("البريد الإلكتروني"),
            hint = tr("أدخل بريدك الإلكتروني"),
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            icon = Icons.Filled.Email,
            enabled = !state.isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
        )

        AuthTextField(
            palette = palette,
            label = tr("كلمة المرور"),
            hint = tr("8 أحرف على الأقل"),
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            icon = Icons.Filled.Lock,
            enabled = !state.isLoading,
            visualTransformation = passwordTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            trailing = {
                IconButton(onClick = viewModel::togglePasswordVisibility, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (state.isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (state.isPasswordVisible) tr("إخفاء كلمة المرور") else tr("إظهار كلمة المرور"),
                        tint = palette.muted
                    )
                }
            }
        )

        if (state.password.isNotEmpty()) {
            PasswordStrengthBar(palette = palette, password = state.password)
        }

        AuthTextField(
            palette = palette,
            label = tr("تأكيد كلمة المرور"),
            hint = tr("أعد كتابة كلمة المرور"),
            value = state.confirmPassword,
            onValueChange = viewModel::onConfirmPasswordChange,
            icon = Icons.Filled.Lock,
            enabled = !state.isLoading,
            visualTransformation = passwordTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                viewModel.signUp()
            })
        )

        if (mismatch) {
            Text(tr("كلمتا المرور غير متطابقتين"), color = palette.danger, style = MaterialTheme.typography.labelMedium)
        } else if (matches) {
            Text(tr("كلمتا المرور متطابقتان"), color = palette.success, style = MaterialTheme.typography.labelMedium)
        }

        state.errorMessage?.let { AuthNotice(palette, it, isError = true) }

        if (emailTaken) {
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text(tr("لدي حساب بهذا البريد — تسجيل الدخول"), color = palette.accent, fontWeight = FontWeight.Bold)
            }
        }

        AuthPrimaryButton(
            palette = palette,
            text = tr("إنشاء الحساب"),
            onClick = {
                focusManager.clearFocus()
                viewModel.signUp()
            },
            modifier = Modifier.fillMaxWidth(),
            loading = state.isLoading
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(tr("لديك حساب بالفعل؟"), color = palette.muted, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onBack, enabled = !state.isLoading) {
                Text(tr("تسجيل الدخول"), color = palette.accent, fontWeight = FontWeight.Bold)
            }
        }
    }
}
