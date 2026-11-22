package com.numberone.numberone.presentation.screens.auth

import com.numberone.numberone.core.localization.tr
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.numberone.numberone.core.security.BiometricAuthManager
import com.numberone.numberone.core.theme.NumberOneColors
import com.numberone.numberone.presentation.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    onCreateAccountClick: () -> Unit
) {
    val state = viewModel.uiState
    val palette = authPalette()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val activity = remember(context) { context.findActivity() as? FragmentActivity }
    val biometricAvailable = remember(context) { BiometricAuthManager.isAvailable(context) }

    var showForgotPassword by remember { mutableStateOf(false) }
    var showEnableBiometric by remember { mutableStateOf(false) }

    val proceed: () -> Unit = {
        viewModel.resetLoginSuccess()
        onLoginSuccess()
    }

    // After a password sign-in, offer fingerprint login once (unless already enabled or declined).
    LaunchedEffect(state.isLoginSuccessful) {
        if (state.isLoginSuccessful) {
            val shouldOffer = biometricAvailable && activity != null &&
                !BiometricAuthManager.isOfferDeclined(context) &&
                !BiometricAuthManager.isEnabledFor(context, state.email)
            if (shouldOffer) showEnableBiometric = true else proceed()
        }
    }

    AuthScaffold(
        palette = palette,
        headerHeight = 268.dp,
        header = {
            Text(
                text = "Number One",
                color = NumberOneColors.Champagne,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = tr("مرحباً بك في عالم الأرقام الافتراضية الذكية"),
                color = NumberOneColors.Ivory.copy(alpha = 0.78f),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            SecurePill(tr("اتصال مشفّر وآمن 100%"))
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = tr("تسجيل الدخول"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = palette.onCard
            )
            Text(
                text = tr("سجّل دخولك للوصول إلى أرقامك ورصيدك"),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.muted
            )
        }

        state.notice?.let { AuthNotice(palette, it, isError = false) }

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
            hint = tr("أدخل كلمة المرور"),
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            icon = Icons.Filled.Lock,
            enabled = !state.isLoading,
            visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                viewModel.login()
            }),
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

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            TextButton(onClick = { showForgotPassword = true }, enabled = !state.isLoading) {
                Text(tr("نسيت كلمة المرور؟"), color = palette.accent, fontWeight = FontWeight.SemiBold)
            }
        }

        state.errorMessage?.let { AuthNotice(palette, it, isError = true) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AuthPrimaryButton(
                palette = palette,
                text = tr("تسجيل الدخول"),
                onClick = {
                    focusManager.clearFocus()
                    viewModel.login()
                },
                modifier = Modifier.weight(1f),
                loading = state.isLoading
            )
            if (biometricAvailable && activity != null) {
                AuthSquareIconButton(
                    palette = palette,
                    icon = Icons.Filled.Fingerprint,
                    description = tr("الدخول بالبصمة"),
                    enabled = !state.isLoading,
                    onClick = {
                        focusManager.clearFocus()
                        if (BiometricAuthManager.isEnabled(context)) {
                            BiometricAuthManager.unlock(
                                activity = activity,
                                onSuccess = { email, password -> viewModel.loginWithSavedCredentials(email, password) },
                                onError = { message -> viewModel.showError(message) }
                            )
                        } else {
                            // Let the offer appear again after the next password sign-in.
                            BiometricAuthManager.setOfferDeclined(context, false)
                            viewModel.showNotice(tr("سجّل الدخول بكلمة المرور مرة واحدة، ثم فعّل الدخول بالبصمة عند ظهور الخيار."))
                        }
                    }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(tr("ليس لديك حساب؟"), color = palette.muted, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onCreateAccountClick, enabled = !state.isLoading) {
                Text(tr("إنشاء حساب جديد"), color = palette.accent, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showForgotPassword) {
        ForgotPasswordDialog(
            palette = palette,
            email = state.email.trim(),
            onDismiss = { showForgotPassword = false }
        )
    }

    if (showEnableBiometric) {
        AlertDialog(
            onDismissRequest = {
                showEnableBiometric = false
                BiometricAuthManager.setOfferDeclined(context, true)
                proceed()
            },
            containerColor = palette.card,
            titleContentColor = palette.onCard,
            textContentColor = palette.muted,
            shape = RoundedCornerShape(28.dp),
            icon = {
                Icon(
                    Icons.Filled.Fingerprint,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = tr("تفعيل الدخول بالبصمة؟"),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = tr("سجّل دخولك بلمسة في المرة القادمة. تُحفظ بياناتك مشفّرة على جهازك ولا تُفتح إلا ببصمتك."),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showEnableBiometric = false
                    val target = activity
                    if (target == null) {
                        proceed()
                    } else {
                        BiometricAuthManager.enable(target, state.email, state.password) { proceed() }
                    }
                }) {
                    Text(tr("تفعيل"), color = palette.accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEnableBiometric = false
                    BiometricAuthManager.setOfferDeclined(context, true)
                    proceed()
                }) {
                    Text(tr("ليس الآن"), color = palette.muted)
                }
            }
        )
    }
}
