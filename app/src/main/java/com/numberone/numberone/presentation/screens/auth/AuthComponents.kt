package com.numberone.numberone.presentation.screens.auth

import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.core.localization.trf
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.numberone.numberone.R
import com.numberone.numberone.core.theme.NumberOneColors
import com.numberone.numberone.core.util.SupportContacts

// ---------------------------------------------------------------------------
// Palette: obsidian + champagne gold. Light and dark variants share the same identity.
// ---------------------------------------------------------------------------

internal data class AuthPalette(
    val background: Color,
    val card: Color,
    val onCard: Color,
    val muted: Color,
    val fieldBg: Color,
    val fieldBorder: Color,
    val accent: Color,
    val accentSoft: Color,
    val danger: Color,
    val success: Color
)

internal val AuthAccent = NumberOneColors.Champagne

/** Navy to indigo header, identical in both modes. */
internal val AuthHeaderBrush: Brush = Brush.verticalGradient(
    listOf(NumberOneColors.Obsidian, NumberOneColors.WarmBlack, NumberOneColors.GoldMid)
)

/** Indigo gradient used for hairline borders and accents. */
internal val AuthAccentBrush: Brush = Brush.linearGradient(
    listOf(NumberOneColors.Champagne, NumberOneColors.GoldMid, NumberOneColors.GoldDeep)
)

/** Indigo to purple gradient for the primary action. */
internal val AuthButtonBrush: Brush = Brush.linearGradient(
    listOf(NumberOneColors.GoldMid, NumberOneColors.GoldDeep)
)

@Composable
internal fun authPalette(): AuthPalette {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (dark) {
        AuthPalette(
            background = NumberOneColors.Obsidian,
            card = Color(0xFF151B33),
            onCard = Color(0xFFF1F3FF),
            muted = Color(0xFF9AA3C7),
            fieldBg = Color(0xFF1B2240),
            fieldBorder = Color(0xFF2A3358),
            accent = NumberOneColors.Champagne,
            accentSoft = NumberOneColors.Champagne.copy(alpha = 0.12f),
            danger = Color(0xFFFCA5A5),
            success = NumberOneColors.Emerald
        )
    } else {
        AuthPalette(
            background = Color(0xFFF3F4FB),
            card = Color.White,
            onCard = Color(0xFF0F1330),
            muted = Color(0xFF5B6385),
            fieldBg = Color(0xFFF5F6FC),
            fieldBorder = Color(0xFFD6DAEE),
            accent = NumberOneColors.GoldMid,
            accentSoft = NumberOneColors.GoldMid.copy(alpha = 0.09f),
            danger = Color(0xFFB3261E),
            success = NumberOneColors.EmeraldDeep
        )
    }
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// ---------------------------------------------------------------------------
// Page scaffold: obsidian header with gold ornaments, emblem straddling the edge,
// and a floating card with a hairline gold edge.
// ---------------------------------------------------------------------------

@Composable
internal fun AuthScaffold(
    palette: AuthPalette,
    headerHeight: Dp,
    onBack: (() -> Unit)? = null,
    headerBrush: Brush = AuthHeaderBrush,
    accentBrush: Brush = AuthAccentBrush,
    accentGlow: Color = NumberOneColors.GoldMid,
    ornamentColor: Color = NumberOneColors.Champagne,
    emblemRes: Int = R.drawable.ic_numberone_mark,
    emblemCard: Boolean = false,
    topEnd: (@Composable () -> Unit)? = null,
    header: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    WhiteStatusBarIcons()
    val emblemSize = 92.dp
    val overlap = emblemSize / 2

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                    .background(headerBrush)
                    .drawBehind { drawHeaderOrnaments(ornamentColor) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 28.dp)
                        .padding(bottom = overlap),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    content = header
                )
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr("رجوع"),
                            tint = NumberOneColors.Champagne
                        )
                    }
                }
            }

            if (topEnd != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(12.dp)
                ) { topEnd() }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = headerHeight - overlap)
                    .padding(horizontal = 20.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = overlap),
                    color = palette.card,
                    contentColor = palette.onCard,
                    shape = RoundedCornerShape(32.dp),
                    shadowElevation = 16.dp,
                    border = BorderStroke(1.dp, palette.fieldBorder.copy(alpha = 0.7f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 22.dp, end = 22.dp, top = overlap + 14.dp, bottom = 26.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        content = content
                    )
                }
                AuthEmblem(
                    palette = palette,
                    size = emblemSize,
                    modifier = Modifier.align(Alignment.TopCenter),
                    brush = accentBrush,
                    headerBrush = headerBrush,
                    glow = accentGlow,
                    drawableRes = emblemRes,
                    card = emblemCard
                )
            }
        }
        Spacer(Modifier.navigationBarsPadding().height(28.dp))
    }
}

/** Champagne glow in the corner and two hairline arcs at the base of the header. */
private fun DrawScope.drawHeaderOrnaments(ornament: Color) {
    val glowCenter = Offset(size.width * 0.92f, size.height * 0.04f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(ornament.copy(alpha = 0.22f), Color.Transparent),
            center = glowCenter,
            radius = size.width * 0.6f
        ),
        radius = size.width * 0.6f,
        center = glowCenter
    )
    val arcCenter = Offset(size.width * 0.5f, size.height * 1.08f)
    drawCircle(
        color = ornament.copy(alpha = 0.20f),
        radius = size.width * 0.42f,
        center = arcCenter,
        style = Stroke(width = 1.dp.toPx())
    )
    drawCircle(
        color = ornament.copy(alpha = 0.10f),
        radius = size.width * 0.56f,
        center = arcCenter,
        style = Stroke(width = 1.dp.toPx())
    )
}

/** Status bar icons stay light over the dark header, then revert when leaving the screen. */
@Composable
private fun WhiteStatusBarIcons() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        if (window == null) {
            onDispose { }
        } else {
            val controller = WindowCompat.getInsetsController(window, view)
            val previous = controller.isAppearanceLightStatusBars
            controller.isAppearanceLightStatusBars = false
            onDispose { controller.isAppearanceLightStatusBars = previous }
        }
    }
}

@Composable
internal fun AuthEmblem(
    palette: AuthPalette,
    size: Dp,
    modifier: Modifier = Modifier,
    brush: Brush = AuthAccentBrush,
    headerBrush: Brush = AuthHeaderBrush,
    glow: Color = NumberOneColors.GoldMid,
    drawableRes: Int = R.drawable.ic_numberone_mark,
    card: Boolean = true
) {
    val shape = RoundedCornerShape(if (card) 24.dp else 30.dp)
    val width = if (card) size * 1.3f else size
    Box(
        modifier = modifier
            .size(width = width, height = size)
            .shadow(20.dp, shape, ambientColor = glow, spotColor = glow)
            .clip(shape)
            .background(headerBrush)
            .border(BorderStroke(1.5.dp, brush), shape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(drawableRes),
            contentDescription = tr("شعار التطبيق"),
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        )
    }
}

/** Gold-tinted "secure connection" pill shown in the header. */
@Composable
internal fun SecurePill(text: String, tint: Color = NumberOneColors.Champagne) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.10f))
            .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Text(
            text = text,
            color = NumberOneColors.Ivory,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ---------------------------------------------------------------------------
// Inputs and buttons
// ---------------------------------------------------------------------------

@Composable
internal fun AuthTextField(
    palette: AuthPalette,
    label: String,
    hint: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)
    val borderColor by animateColorAsState(
        targetValue = if (focused) palette.accent else palette.fieldBorder,
        label = "authFieldBorder"
    )
    val iconTint by animateColorAsState(
        targetValue = if (focused) palette.accent else palette.muted,
        label = "authFieldIcon"
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.6f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = palette.muted
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .clip(shape)
                .background(palette.fieldBg)
                .border(if (focused) 1.5.dp else 1.dp, borderColor, shape)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text = hint,
                        color = palette.muted.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focused = it.isFocused }
                        .semantics {
                            contentDescription = label
                            if (visualTransformation != VisualTransformation.None) password()
                        },
                    enabled = enabled,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.onCard),
                    cursorBrush = SolidColor(palette.accent),
                    visualTransformation = visualTransformation,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions
                )
            }
            trailing?.invoke()
        }
    }
}

/** Gold gradient primary button with dark text, the signature action of the app. */
@Composable
internal fun AuthPrimaryButton(
    palette: AuthPalette,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true,
    brush: Brush = AuthButtonBrush,
    glow: Color = NumberOneColors.GoldMid,
    contentColor: Color = Color.White
) {
    val shape = RoundedCornerShape(18.dp)
    val active = enabled && !loading
    Box(
        modifier = modifier
            .heightIn(min = 56.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .shadow(if (active) 14.dp else 0.dp, shape, ambientColor = glow, spotColor = glow)
            .clip(shape)
            .background(brush)
            .clickable(enabled = active, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(22.dp)
                    .semantics { contentDescription = tr("جارٍ التحميل") },
                color = contentColor,
                strokeWidth = 2.5.dp
            )
        } else {
            Text(
                text = text,
                color = contentColor,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
internal fun AuthSquareIconButton(
    palette: AuthPalette,
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    brush: Brush = AuthAccentBrush
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .size(56.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(shape)
            .background(palette.accentSoft)
            .border(1.dp, brush, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = palette.accent, modifier = Modifier.size(28.dp))
    }
}

@Composable
internal fun AuthNotice(palette: AuthPalette, text: String, isError: Boolean) {
    val tone = if (isError) palette.danger else palette.success
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
            .clip(shape)
            .background(tone.copy(alpha = 0.10f))
            .border(1.dp, tone.copy(alpha = 0.35f), shape)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = if (isError) Icons.Filled.Warning else Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = tone,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            color = palette.onCard,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
    }
}

// ---------------------------------------------------------------------------
// Password strength bar
// ---------------------------------------------------------------------------

@Composable
internal fun PasswordStrengthBar(palette: AuthPalette, password: String) {
    val strength = remember(password) { evaluatePasswordStrength(password) }
    val activeColor by animateColorAsState(
        targetValue = when (strength) {
            PasswordStrength.NONE -> palette.fieldBorder
            PasswordStrength.WEAK -> palette.danger
            PasswordStrength.FAIR -> Color(0xFFE0A030)
            PasswordStrength.GOOD -> NumberOneColors.Champagne
            PasswordStrength.STRONG -> palette.success
        },
        label = "passwordStrengthColor"
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (index < strength.level) activeColor else palette.fieldBorder)
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(tr("قوة كلمة المرور"), color = palette.muted, style = MaterialTheme.typography.labelSmall)
            Text(
                text = strength.label,
                color = activeColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
        if (strength == PasswordStrength.WEAK || strength == PasswordStrength.FAIR) {
            Text(
                text = tr("استخدم 8 أحرف أو أكثر مع مزيج من الأحرف الكبيرة والأرقام والرموز"),
                color = palette.muted,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Forgot password: account recovery goes through support (WhatsApp / Telegram)
// ---------------------------------------------------------------------------

@Composable
internal fun ForgotPasswordDialog(palette: AuthPalette, email: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val message = if (email.isNotBlank()) {
        trf("مرحباً، نسيت كلمة المرور لحسابي المسجّل بالبريد: {0}", email)
    } else {
        tr("مرحباً، نسيت كلمة المرور لحسابي وأحتاج مساعدة في استعادته.")
    }
    val whatsappUri = SupportContacts.whatsappUri(message)
    val telegramUri = SupportContacts.telegramUri()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.card,
        titleContentColor = palette.onCard,
        textContentColor = palette.muted,
        shape = RoundedCornerShape(28.dp),
        icon = {
            Icon(
                Icons.Filled.SupportAgent,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = tr("نسيت كلمة المرور؟"),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = tr("استعادة الحساب تتم عبر فريق الدعم. تواصل معنا وأرسل بريدك الإلكتروني المسجّل لنساعدك في استعادة الوصول إلى حسابك."),
                    style = MaterialTheme.typography.bodyMedium
                )
                SupportChannelButton(
                    palette = palette,
                    text = tr("تواصل عبر واتساب"),
                    icon = Icons.Filled.Call,
                    brand = Color(0xFF25D366),
                    enabled = whatsappUri != null,
                    onClick = { whatsappUri?.let { openLink(context, it) } }
                )
                SupportChannelButton(
                    palette = palette,
                    text = tr("تواصل عبر تيليجرام"),
                    icon = Icons.AutoMirrored.Filled.Send,
                    brand = Color(0xFF229ED9),
                    enabled = telegramUri != null,
                    onClick = { telegramUri?.let { openLink(context, it) } }
                )
                if (whatsappUri == null && telegramUri == null) {
                    Text(
                        text = tr("قنوات الدعم غير مهيّأة حالياً. يرجى المحاولة لاحقاً."),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("إغلاق"), color = palette.accent, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun SupportChannelButton(
    palette: AuthPalette,
    text: String,
    icon: ImageVector,
    brand: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shape)
            .background(brand.copy(alpha = 0.12f))
            .border(1.dp, brand.copy(alpha = 0.55f), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = brand, modifier = Modifier.size(22.dp))
        Text(
            text = text,
            color = palette.onCard,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun openLink(context: Context, uri: Uri) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (e: Exception) {
        Toast.makeText(context, tr("تعذر فتح الرابط. تأكد من تثبيت التطبيق."), Toast.LENGTH_SHORT).show()
    }
}
