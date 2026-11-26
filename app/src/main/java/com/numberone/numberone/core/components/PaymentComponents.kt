package com.numberone.numberone.core.components

import com.numberone.numberone.core.localization.tr
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.numberone.numberone.core.error.ErrorMapper
import com.numberone.numberone.core.util.ReceiptFile
import com.numberone.numberone.core.util.formatFileSize
import com.numberone.numberone.core.util.readReceiptFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

// ---------------------------------------------------------------------------
// Validation
// ---------------------------------------------------------------------------

private val TXID_REGEX = Regex("^(0[xX])?[0-9a-fA-F]{64}$")

/** BEP20 transaction hash: 64 hex characters, optionally prefixed with 0x. */
fun isValidTxId(value: String): Boolean = TXID_REGEX.matches(value.trim())

// ---------------------------------------------------------------------------
// Receipt picking
// ---------------------------------------------------------------------------

/**
 * Returns a function that opens the system photo picker (no storage permission needed).
 * The chosen image is read, validated and optimized off the main thread; the result (or a
 * friendly Arabic error) is delivered through [onPicked] / [onError].
 */
@Composable
fun rememberReceiptPicker(
    onPicked: (ReceiptFile) -> Unit,
    onError: (String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestPicked by rememberUpdatedState(onPicked)
    val latestError by rememberUpdatedState(onError)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val file = withContext(Dispatchers.IO) { readReceiptFile(context, uri) }
                    latestPicked(file)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    latestError(ErrorMapper.toUserMessage(e))
                }
            }
        }
    }
    return remember(launcher) {
        { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    }
}

/** Sub-sampled decode so a big screenshot never blows up memory just to be previewed. */
private fun decodeForPreview(bytes: ByteArray, maxEdge: Int): ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxEdge) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    return try {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)?.asImageBitmap()
    } catch (e: OutOfMemoryError) {
        null
    }
}

@Composable
internal fun rememberReceiptBitmap(file: ReceiptFile, maxEdge: Int): State<ImageBitmap?> =
    produceState<ImageBitmap?>(initialValue = null, file, maxEdge) {
        value = withContext(Dispatchers.Default) { decodeForPreview(file.bytes, maxEdge) }
    }

private fun Modifier.dashedBorder(color: Color, radius: Dp, width: Dp = 1.5.dp): Modifier = drawBehind {
    val w = width.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(w / 2f, w / 2f),
        size = Size(size.width - w, size.height - w),
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f)))
    )
}

/** Inviting empty state of the receipt step: one big tap target. */
@Composable
fun ReceiptDropZone(
    onPick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    title: String = tr("ارفع صورة الإيصال"),
    hint: String = tr("اختر لقطة شاشة التحويل من معرض الصور\nJPG أو PNG أو WEBP")
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onPick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = 0.06f),
        modifier = modifier.fillMaxWidth().dashedBorder(accent.copy(alpha = 0.55f), 18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconBadge(Icons.Filled.AddPhotoAlternate, size = 52.dp)
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Preview of the picked receipt with a tap-to-zoom viewer, file info and the submit / replace / remove actions.
 * Nothing is uploaded until the user presses the confirm button.
 */
@Composable
fun ReceiptPreviewCard(
    file: ReceiptFile,
    loading: Boolean,
    onConfirm: () -> Unit,
    onReplace: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    confirmLabel: String = tr("إرسال الإيصال"),
    confirmEnabled: Boolean = true,
    confirmHint: String? = null
) {
    val preview by rememberReceiptBitmap(file, 1280)
    var zoomed by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(enabled = preview != null) { zoomed = true },
            contentAlignment = Alignment.Center
        ) {
            val bitmap = preview
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = tr("معاينة الإيصال"),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                )
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.ZoomIn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(tr("تكبير"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = successColor(), modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tr("تم اختيار الإيصال"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                val dims = if (file.width > 0 && file.height > 0) " • ${file.width}×${file.height}" else ""
                Text(
                    ltr(formatFileSize(file.bytes.size) + dims),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (file.optimized) {
            Text(
                tr("حسّنّا الصورة تلقائياً (تحويل/ضغط) لتناسب حدّ الرفع، وتبقى بيانات الإيصال واضحة."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (confirmHint != null) {
            Text(confirmHint, style = MaterialTheme.typography.bodySmall, color = warningColor(), fontWeight = FontWeight.SemiBold)
        }

        NOButton(text = confirmLabel, onClick = onConfirm, enabled = confirmEnabled && !loading, loading = loading)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onReplace,
                enabled = !loading,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(tr("تغيير الصورة"))
            }
            TextButton(onClick = onRemove, enabled = !loading, modifier = Modifier.weight(1f)) {
                Text(tr("إزالة"), color = dangerColor(), fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (zoomed) {
        FullScreenImageDialog(title = tr("معاينة الإيصال"), onDismiss = { zoomed = false }) {
            val big by rememberReceiptBitmap(file, 2400)
            val bitmap = big ?: preview
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = tr("الإيصال"),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Zoomable full screen viewer (customer preview + admin inspection)
// ---------------------------------------------------------------------------

/** Pinch to zoom (1x to 6x), drag while zoomed, double tap to toggle zoom. Pan is clamped to the content. */
@Composable
fun ZoomableBox(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var box by remember { mutableStateOf(IntSize.Zero) }

    fun clamp(o: Offset, s: Float): Offset {
        val maxX = box.width * (s - 1f) / 2f
        val maxY = box.height * (s - 1f) / 2f
        return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { box = it }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    if (scale > 1f) {
                        scale = 1f
                        offset = Offset.Zero
                    } else {
                        scale = 2.5f
                    }
                })
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val next = (scale * zoom).coerceIn(1f, 6f)
                    scale = next
                    offset = if (next > 1f) clamp(offset + pan, next) else Offset.Zero
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

@Composable
fun FullScreenImageDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            ZoomableBox(modifier = Modifier.fillMaxSize(), content = content)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = tr("إغلاق"), tint = Color.White)
                }
                Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                tr("اضغط مرتين للتكبير • اسحب للتحريك"),
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp)
            )
        }
    }
}

/** Admin receipt inspection: full-screen, zoomable, with explicit loading and error states. */
@Composable
fun ReceiptViewerDialog(url: String, onDismiss: () -> Unit) {
    var state by remember(url) { mutableIntStateOf(0) } // 0 loading, 1 loaded, 2 failed
    FullScreenImageDialog(title = tr("إيصال الدفع"), onDismiss = onDismiss) {
        AsyncImage(
            model = url,
            contentDescription = tr("صورة الإيصال"),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
            onLoading = { state = 0 },
            onSuccess = { state = 1 },
            onError = { state = 2 }
        )
        when (state) {
            0 -> CircularProgressIndicator(color = Color.White)
            2 -> Text(
                tr("تعذر تحميل صورة الإيصال. قد ينتهي الرابط خلال 5 دقائق، أغلق النافذة وافتحه من جديد."),
                color = Color.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(32.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Method cards, steps, copy rows
// ---------------------------------------------------------------------------

/** Large selectable payment method card (used in a two-up row). */
@Composable
fun PaymentMethodCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    logo: (@Composable () -> Unit)? = null
) {
    val border by animateColorAsState(
        if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
        label = "methodBorder"
    )
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) accent.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, border)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (logo != null) logo() else IconBadge(icon, tint = accent, size = 42.dp)
                Spacer(Modifier.weight(1f))
                if (selected) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = tr("محدد"), tint = accent, modifier = Modifier.size(24.dp))
                } else {
                    Box(modifier = Modifier.size(22.dp).border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Numbered step header; shows a check once the step is complete. */
@Composable
fun PaymentStepHeader(number: Int, title: String, subtitle: String? = null, done: Boolean = false) {
    val tone = if (done) successColor() else MaterialTheme.colorScheme.primary
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier.size(30.dp).clip(CircleShape).background(tone),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            } else {
                Text(
                    number.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Pill used for amount presets and currency choices. */
@Composable
fun ChoicePill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) accent else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp), contentAlignment = Alignment.Center) {
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * One-tap copy row: the whole row is the button. Gives instant feedback (check mark + haptic)
 * so the user knows the value is on the clipboard.
 */
@Composable
fun CopyRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    copyValue: String = value,
    emphasized: Boolean = false,
    monospace: Boolean = false
) {
    val clipboard = LocalClipboardManager.current
    val haptics = LocalHapticFeedback.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }
    val accent = if (copied) successColor() else MaterialTheme.colorScheme.primary
    var valueStyle = (if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium)
        .copy(textDirection = TextDirection.Ltr)
    if (monospace) valueStyle = valueStyle.copy(fontFamily = FontFamily.Monospace)

    Surface(
        onClick = {
            clipboard.setText(AnnotatedString(copyValue))
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            copied = true
        },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = valueStyle, fontWeight = if (emphasized) FontWeight.Black else FontWeight.SemiBold)
            }
            Surface(shape = RoundedCornerShape(50), color = accent.copy(alpha = 0.14f), contentColor = accent) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(if (copied) tr("تم النسخ") else tr("نسخ"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** Amber caution strip, e.g. "BEP20 only". */
@Composable
fun WarningStrip(text: String, modifier: Modifier = Modifier) {
    val color = warningColor()
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = color.copy(alpha = 0.13f)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}

// ---------------------------------------------------------------------------
// Brand badges
// ---------------------------------------------------------------------------

val BinanceYellow = Color(0xFFF0B90B)
val ShamCashGreen = Color(0xFF0E9F6E)

/** Binance-style badge: black diamond mark on the brand yellow, drawn in code (no image asset needed). */
@Composable
fun BinanceLogo(size: Dp = 44.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(size).clip(RoundedCornerShape(size * 0.28f)).background(BinanceYellow),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.66f)) {
            val c = this.size.width / 2f
            fun diamond(cx: Float, cy: Float, r: Float) = Path().apply {
                moveTo(cx, cy - r); lineTo(cx + r, cy); lineTo(cx, cy + r); lineTo(cx - r, cy); close()
            }
            val ink = Color(0xFF1E2026)
            val d = this.size.width * 0.30f
            drawPath(diamond(c, c, this.size.width * 0.11f), ink)
            drawPath(diamond(c, c - d, this.size.width * 0.11f), ink)
            drawPath(diamond(c, c + d, this.size.width * 0.11f), ink)
            drawPath(diamond(c - d, c, this.size.width * 0.11f), ink)
            drawPath(diamond(c + d, c, this.size.width * 0.11f), ink)
        }
    }
}

/** Sham Cash badge: green rounded monogram. Swap for the official drawable if you have one. */
@Composable
fun ShamCashLogo(size: Dp = 44.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(Brush.linearGradient(listOf(ShamCashGreen, Color(0xFF0B7A55)))),
        contentAlignment = Alignment.Center
    ) {
        Text("SC", color = Color.White, fontWeight = FontWeight.Black, fontSize = (size.value * 0.38f).sp)
    }
}

/** Logo + name strip placed directly above a payment method's transfer card. */
@Composable
fun PaymentBrandHeader(name: String, caption: String, logo: @Composable () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        logo()
        Column {
            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun BinanceBrandHeader(modifier: Modifier = Modifier) =
    PaymentBrandHeader("Binance", tr("تحويل USDT عبر شبكة BEP20"), { BinanceLogo(48.dp) }, modifier)

@Composable
fun ShamCashBrandHeader(modifier: Modifier = Modifier) =
    PaymentBrandHeader(tr("شام كاش"), tr("تحويل بالليرة السورية أو الدولار"), { ShamCashLogo(48.dp) }, modifier)
