package com.numberone.numberone.core.components

import com.numberone.numberone.core.localization.tr
import android.content.Context
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.numberone.numberone.core.error.AppLocale
import com.numberone.numberone.core.theme.NumberOneColors
import com.numberone.numberone.core.util.ReceiptFile
import java.time.LocalDate

// ---------------------------------------------------------------------------
// Status model
// ---------------------------------------------------------------------------

enum class StatusTone { SUCCESS, PENDING, ERROR, NEUTRAL }

private val SUCCESS_STATUSES = setOf("approved", "credited", "completed", "active", "verified", "paid", "delivered")
private val ERROR_STATUSES = setOf("rejected", "declined", "failed", "cancelled", "canceled", "expired")

fun statusTone(status: String?): StatusTone {
    val s = status.orEmpty().trim().lowercase()
    return when {
        s.isEmpty() -> StatusTone.NEUTRAL
        s in SUCCESS_STATUSES -> StatusTone.SUCCESS
        s in ERROR_STATUSES -> StatusTone.ERROR
        else -> StatusTone.PENDING
    }
}

fun statusLabel(status: String?): String = when (status.orEmpty().trim().lowercase()) {
    "approved" -> tr("معتمد")
    "credited" -> tr("مُضاف للرصيد")
    "completed" -> tr("مكتمل")
    "active" -> tr("نشط")
    "verified" -> tr("متحقق")
    "paid" -> tr("مدفوع")
    "delivered" -> tr("تم التسليم")
    "rejected", "declined" -> tr("مرفوض")
    "cancelled", "canceled" -> tr("ملغى")
    "expired" -> tr("منتهي")
    "failed" -> tr("فشل")
    "awaiting_payment" -> tr("بانتظار الدفع")
    "", "pending", "submitted", "payment_review" -> tr("قيد المراجعة")
    else -> status.orEmpty().replace('_', ' ')
}

@Composable
private fun isDarkTheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

@Composable fun successColor(): Color = if (isDarkTheme()) Color(0xFF34D399) else Color(0xFF047857)
@Composable fun warningColor(): Color = if (isDarkTheme()) Color(0xFFFBBF24) else Color(0xFFB45309)
@Composable fun dangerColor(): Color = if (isDarkTheme()) Color(0xFFF87171) else Color(0xFFB91C1C)

@Composable
private fun toneColor(tone: StatusTone): Color = when (tone) {
    StatusTone.SUCCESS -> successColor()
    StatusTone.PENDING -> warningColor()
    StatusTone.ERROR -> dangerColor()
    StatusTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** Color-coded status pill: Approved = green, Pending = amber, Rejected = red. */
@Composable
fun StatusChip(status: String?, modifier: Modifier = Modifier) {
    val color = toneColor(statusTone(status))
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
        contentColor = color
    ) {
        Text(
            text = statusLabel(status),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ---------------------------------------------------------------------------
// Layout
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NOTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("رجوع"))
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

/** Standard scrollable screen: top bar, 16dp screen padding, 12dp spacing, safe-area insets from Scaffold. */
@Composable
fun NOScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { NOTopBar(title = title, onBack = onBack, actions = actions) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

/** Elevated surface card with 20dp corners and 16dp inner padding. */
@Composable
fun NOCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    val elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    val inner: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
    if (onClick != null) {
        ElevatedCard(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            elevation = elevation,
            content = inner
        )
    } else {
        ElevatedCard(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            elevation = elevation,
            content = inner
        )
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Buttons
// ---------------------------------------------------------------------------

/** Primary full-width CTA. Shows a spinner inside the button while loading. */
@Composable
fun NOButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(text, fontWeight = FontWeight.Bold)
        }
    }
}

/** Filled button in a given tone, used for approve (green) and reject-style actions. */
@Composable
fun ToneButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

// ---------------------------------------------------------------------------
// Hero, brand and tiles
// ---------------------------------------------------------------------------

/** Deep Navy to Royal Indigo gradient used by the logo mark and the hero card. */
private val BrandGradient = listOf(
    NumberOneColors.NavyHeader,
    NumberOneColors.IndigoDeep,
    NumberOneColors.IndigoRoyal,
    NumberOneColors.ButtonPurple
)

/** Navy and indigo gradient logo mark. */
@Composable
fun BrandMark(size: Dp = 64.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(BrandGradient)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "N1",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = (size.value * 0.36f).sp
        )
    }
}

/** Featured gradient balance card. The footer slot holds quick actions or badges. */
@Composable
fun BalanceHeroCard(
    label: String,
    amount: String,
    modifier: Modifier = Modifier,
    footer: (@Composable ColumnScope.() -> Unit)? = null
) {
    val shape = RoundedCornerShape(24.dp)
    val glow = NumberOneColors.IndigoRoyal
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(18.dp, shape, ambientColor = glow, spotColor = glow)
            .clip(shape)
            .background(Brush.linearGradient(BrandGradient))
            .drawBehind {
                // Soft light bloom in the top corner for a glassy finish.
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.26f), Color.Transparent),
                        center = Offset(size.width * 0.9f, size.height * 0.05f),
                        radius = size.width * 0.7f
                    )
                )
            }
            .border(
                1.dp,
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0.04f))),
                shape
            )
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.78f)
            )
            Text(
                amount,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            if (footer != null) {
                footer()
            }
        }
    }
}

/** Square-ish navigation tile with an icon, title and subtitle. */
@Composable
fun ActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false
) {
    val container = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val content = if (highlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val accent = if (highlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 128.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = container, contentColor = content),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (highlighted) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Top notifications
// ---------------------------------------------------------------------------

/**
 * Host for in-app notifications. Place it inside a Box and align it to TopCenter with
 * status-bar padding: each message is a floating pill that slides in from the top edge.
 */
@Composable
fun TopSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
        snackbar = { data -> FloatingPillSnackbar(data) }
    )
}

@Composable
private fun FloatingPillSnackbar(data: SnackbarData) {
    val visibleState = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { visibleState.targetState = true }
    val shape = RoundedCornerShape(50)
    AnimatedVisibility(
        visibleState = visibleState,
        enter = slideInVertically(initialOffsetY = { -it * 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it * 2 }) + fadeOut()
    ) {
        Row(
            modifier = Modifier
                .shadow(14.dp, shape, ambientColor = NumberOneColors.IndigoRoyal, spotColor = NumberOneColors.IndigoRoyal)
                .clip(shape)
                .background(Brush.horizontalGradient(listOf(NumberOneColors.IndigoRoyal, NumberOneColors.ButtonPurple)))
                .border(1.dp, Color.White.copy(alpha = 0.22f), shape)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = data.visuals.message,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f, fill = false)
            )
            val actionLabel = data.visuals.actionLabel
            if (actionLabel != null) {
                TextButton(onClick = { data.performAction() }) {
                    Text(actionLabel, color = NumberOneColors.Gold, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Copy, dates and feedback
// ---------------------------------------------------------------------------

/** Shows a labelled value with a copy-to-clipboard button. */
@Composable
fun CopyField(label: String, value: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = {
                clipboard.setText(AnnotatedString(value))
                copiedToast(context)
            }) {
                Icon(Icons.Filled.ContentCopy, contentDescription = tr("نسخ"), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

fun copiedToast(context: Context) {
    Toast.makeText(context, if (AppLocale.isArabic) tr("تم النسخ") else "Copied", Toast.LENGTH_SHORT).show()
}

/** Pill showing an expiry date. Turns red once the date has passed. */
@Composable
fun ExpiryBadge(expiry: String?, modifier: Modifier = Modifier) {
    if (expiry.isNullOrBlank()) return
    val date = runCatching { LocalDate.parse(expiry.trim().take(10)) }.getOrNull()
    val expired = date != null && date.isBefore(LocalDate.now())
    val color = if (expired) dangerColor() else MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
        contentColor = color
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(
                text = (if (expired) tr("انتهى في ") else tr("ينتهي في ")) + formatDateTime(expiry),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** Compact, readable timestamp from an ISO string, e.g. "2026-11-20 10:30". */
fun formatDateTime(raw: String?): String {
    if (raw.isNullOrBlank()) return "—"
    return raw.trim().replace('T', ' ').take(16)
}

/** Inline notice box. */
@Composable
fun NoticeBanner(text: String, isError: Boolean, modifier: Modifier = Modifier) {
    val color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isError) Icons.Filled.Warning else Icons.Filled.Schedule,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Text(text, color = color, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Standard loading bar plus error and success notices for a screen. */
@Composable
fun FeedbackBanner(loading: Boolean, error: String?, message: String?) {
    if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    if (error != null) NoticeBanner(text = error, isError = true)
    if (message != null) NoticeBanner(text = message, isError = false)
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(vertical = 12.dp)
    )
}

/**
 * Backwards-compatible wrapper: shows a locally picked receipt with explicit confirm and cancel
 * buttons before anything is uploaded. Delegates to the shared [ReceiptPreviewCard].
 */
@Composable
fun ReceiptConfirmCard(
    file: ReceiptFile,
    loading: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    ReceiptPreviewCard(
        file = file,
        loading = loading,
        onConfirm = onConfirm,
        onReplace = onCancel,
        onRemove = onCancel
    )
}
