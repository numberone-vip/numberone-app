package com.numberone.numberone.presentation.screens.auth

import com.numberone.numberone.core.localization.tr
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.numberone.numberone.R
import com.numberone.numberone.core.security.BiometricAuthManager
import com.numberone.numberone.core.localization.AppLanguage
import com.numberone.numberone.core.legal.LegalDocument
import com.numberone.numberone.core.legal.LegalDocuments
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.numberone.numberone.di.AppContainer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()
    val prefs = AppContainer.appPreferences
    val language by prefs.appLanguage.collectAsState(initial = AppLanguage.ENGLISH)
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val activity = remember(context) { context.findActivity() as? FragmentActivity }
    val biometricAvailable = remember(context) { BiometricAuthManager.isAvailable(context) }

    var showForgotPassword by remember { mutableStateOf(false) }
    var showEnableBiometric by remember { mutableStateOf(false) }
    var legalDoc by remember { mutableStateOf<LegalDocument?>(null) }

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

    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val reveal by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "loginReveal"
    )

    val obPrefs = remember(context) { context.getSharedPreferences("numberone_onboarding", android.content.Context.MODE_PRIVATE) }
    var showOb by remember { mutableStateOf(!obPrefs.getBoolean("seen", false)) }
    var obIdx by remember { mutableStateOf(0) }
    if (showOb) {
        val slides = listOf(
            Triple(tr("أرقام فورية بلمسة واحدة"), tr("استقبل رموز التفعيل لواتساب وتيليجرام خلال ثوانٍ وبشكل تلقائي."), "⚡"),
            Triple(tr("أرقام خاصة دائمة (VIP)"), tr("أرقام حقيقية مخصصة لك وحدك تقبلها المنصات الحساسة مثل فيسبوك."), "👑"),
            Triple(tr("شحن سريع وآمن 100%"), tr("اشحن محفظتك بسهولة عبر USDT (BEP20) أو شام كاش وتابع عملياتك فوراً."), "💎")
        )
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { obPrefs.edit().putBoolean("seen", true).apply(); showOb = false }) { Text(tr("تخطي"), fontWeight = FontWeight.Bold) }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(slides[obIdx].third, style = MaterialTheme.typography.displayLarge)
                    Text(slides[obIdx].first, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Text(slides[obIdx].second, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
                Button(onClick = { if (obIdx < 2) obIdx++ else { obPrefs.edit().putBoolean("seen", true).apply(); showOb = false } }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                    Text(if (obIdx < 2) tr("التالي") else tr("ابدأ الآن"), fontWeight = FontWeight.Black)
                }
            }
        }
        return
    }
    AuthScaffold(
        palette = palette,
        headerHeight = 268.dp,
        topEnd = {
            LanguageToggle(
                isArabic = language.isArabic,
                onSelect = { chosen -> scope.launch { prefs.setLanguage(chosen) } }
            )
        },
        header = {
            Text(
                text = "Number One",
                color = Color(0xFFE0E7FF),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AuthAccentBrush)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = tr("مرحباً بك في عالم الأرقام الافتراضية الذكية"),
                color = NumberOneColors.Ivory.copy(alpha = 0.78f),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
        }
    ) {
        Column(
            modifier = Modifier.graphicsLayer {
                alpha = reveal
                translationY = (1f - reveal) * 32f
            },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = tr("أهلاً بعودتك"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.onCard,
                    modifier = Modifier.semantics { heading() }
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
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
                    IconButton(onClick = viewModel::togglePasswordVisibility) {
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
                    loading = state.isLoading,
                    enabled = state.email.isNotBlank() && state.password.isNotBlank()
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = palette.fieldBorder)
                Text(tr("أو"), color = palette.muted, style = MaterialTheme.typography.labelMedium)
                HorizontalDivider(modifier = Modifier.weight(1f), color = palette.fieldBorder)
            }

            Text(
                text = tr("ليس لديك حساب؟"),
                color = palette.muted,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = onCreateAccountClick,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, AuthAccentBrush),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.accent)
            ) {
                Text(tr("إنشاء حساب جديد"), fontWeight = FontWeight.Bold)
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = tr("بمتابعتك فإنك توافق على"),
                    color = palette.muted,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { legalDoc = LegalDocuments.terms(language) }) {
                        Text(tr("الشروط"), color = palette.accent, style = MaterialTheme.typography.labelMedium)
                    }
                    Text("•", color = palette.muted)
                    TextButton(onClick = { legalDoc = LegalDocuments.privacy(language) }) {
                        Text(tr("سياسة الخصوصية"), color = palette.accent, style = MaterialTheme.typography.labelMedium)
                    }
                }
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

    legalDoc?.let { doc ->
        LegalDialog(doc = doc, palette = palette, onDismiss = { legalDoc = null })
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

/** Compact Arabic / English switch shown in the header, so the login screen is usable before signing in. */
@Composable
private fun LanguageToggle(isArabic: Boolean, onSelect: (AppLanguage) -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, NumberOneColors.Champagne.copy(alpha = 0.35f), shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        LanguageOption(label = "العربية", selected = isArabic) { onSelect(AppLanguage.ARABIC) }
        LanguageOption(label = "English", selected = !isArabic) { onSelect(AppLanguage.ENGLISH) }
    }
}

@Composable
private fun LanguageOption(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) NumberOneColors.GoldMid else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick)
            .heightIn(min = 40.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics { this.selected = selected },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color(0xFFC7D2FE),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LegalDialog(doc: LegalDocument, palette: AuthPalette, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.card,
        titleContentColor = palette.onCard,
        textContentColor = palette.muted,
        shape = RoundedCornerShape(28.dp),
        title = { Text(text = doc.title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = doc.updated,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.muted
                )
                doc.sections.forEach { section ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = section.heading,
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.onCard
                        )
                        Text(
                            text = section.body,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("إغلاق"), color = palette.accent, fontWeight = FontWeight.Bold)
            }
        }
    )
}
