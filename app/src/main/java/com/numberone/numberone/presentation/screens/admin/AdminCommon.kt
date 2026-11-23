@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.numberone.numberone.presentation.screens.admin

import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.core.localization.trf
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.numberone.numberone.core.components.IconBadge
import com.numberone.numberone.core.components.NOCard
import com.numberone.numberone.core.components.ToneButton
import com.numberone.numberone.core.components.dangerColor
import com.numberone.numberone.core.components.successColor
import java.security.SecureRandom
import java.time.LocalDate

/** 16-character access code without look-alike characters (0/O, 1/I). */
internal fun newAccessCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    val r = SecureRandom()
    return (1..16).map { chars[r.nextInt(chars.length)] }.joinToString("")
}

internal fun shamLabel(method: String): String = when (method) {
    "SHAM_CASH_SYP" -> tr("شام كاش • ليرة سورية")
    "SHAM_CASH_USD" -> tr("شام كاش • دولار")
    else -> tr("شام كاش")
}

/** "منذ N ساعة" / "منذ N يوم" for the age of an open order. */
internal fun ageText(hours: Long): String =
    if (hours < 24) trf("منذ {0} ساعة", hours) else trf("منذ {0} يوم", hours / 24)

/** Label for an expiry shortcut, in days from today. */
internal fun durationLabel(days: Int): String = when (days) {
    7 -> tr("7 أيام")
    30 -> tr("30 يوماً")
    60 -> tr("60 يوماً")
    90 -> tr("90 يوماً")
    180 -> tr("180 يوماً")
    365 -> tr("سنة")
    else -> trf("{0} يوماً", days)
}

internal fun orderPaymentLabel(method: String): String = when (method.trim().lowercase()) {
    "wallet" -> tr("رصيد المحفظة")
    "" -> "—"
    else -> method.replace('_', ' ')
}

@Composable
internal fun ltrTextStyle(): TextStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)

@Composable
internal fun KpiCard(modifier: Modifier, label: String, value: String, icon: ImageVector) {
    NOCard(modifier = modifier) {
        IconBadge(icon, size = 38.dp)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, maxLines = 1)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Small tinted count pill used for tab/alert badges and role labels. */
@Composable
internal fun CountPill(text: String, color: androidx.compose.ui.graphics.Color) {
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.16f), contentColor = color) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
internal fun ReviewActions(
    canDecide: Boolean,
    hasReceipt: Boolean,
    loading: Boolean,
    approveText: String,
    approveEnabled: Boolean = true,
    onView: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        if (hasReceipt) {
            OutlinedButton(
                onClick = onView,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(tr("  معاينة الإيصال"), style = MaterialTheme.typography.labelLarge)
            }
        }
        if (canDecide) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ToneButton(
                    text = approveText,
                    color = successColor(),
                    onClick = onApprove,
                    modifier = Modifier.weight(1f).height(48.dp),
                    enabled = !loading && approveEnabled
                )
                ToneButton(
                    text = tr("رفض"),
                    color = dangerColor(),
                    onClick = onReject,
                    modifier = Modifier.weight(1f).height(48.dp),
                    enabled = !loading
                )
            }
        }
    }
}

/** Rejection dialog: the reason is required and validated inline, so the dialog never closes on bad input. */
@Composable
internal fun ReasonDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    tr("سيظهر السبب للعميل، اكتبه بوضوح."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it; showError = false },
                    label = { Text(tr("سبب الرفض")) },
                    isError = showError,
                    supportingText = { if (showError) Text(tr("سبب الرفض مطلوب")) },
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (reason.isBlank()) showError = true else onConfirm(reason.trim())
            }) { Text(tr("تأكيد الرفض"), color = dangerColor(), fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("إلغاء")) } }
    )
}

/**
 * Access code + expiry editor shared by "approve order", "assign subscription" and "reset access".
 * The code has a generate button; expiry has 30 / 90 / 365-day shortcuts that highlight when active.
 */
@Composable
internal fun AccessFields(
    code: String,
    onCode: (String) -> Unit,
    expiry: String,
    onExpiry: (String) -> Unit,
    expiryError: String?,
    codeLabel: String = tr("رمز الوصول (16 خانة)"),
    shortcutDays: List<Int> = listOf(30, 90, 365)
) {
    val style = ltrTextStyle()
    OutlinedTextField(
        value = code,
        onValueChange = onCode,
        label = { Text(codeLabel) },
        singleLine = true,
        textStyle = style,
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = { TextButton(onClick = { onCode(newAccessCode()) }) { Text(tr("توليد"), fontWeight = FontWeight.Bold) } }
    )
    OutlinedTextField(
        value = expiry,
        onValueChange = onExpiry,
        label = { Text(tr("تاريخ الانتهاء (yyyy-MM-dd)")) },
        singleLine = true,
        isError = expiryError != null,
        supportingText = { expiryError?.let { Text(it) } },
        textStyle = style,
        modifier = Modifier.fillMaxWidth()
    )
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        shortcutDays.forEach { days ->
            val target = LocalDate.now().plusDays(days.toLong()).toString()
            FilterChip(
                selected = expiry.trim() == target,
                onClick = { onExpiry(target) },
                label = { Text(durationLabel(days)) }
            )
        }
    }
}

/** Returns an Arabic error for an invalid yyyy-MM-dd date, or null when valid (blank counts as valid only if [allowBlank]). */
internal fun dateError(value: String, allowBlank: Boolean): String? {
    val t = value.trim()
    if (t.isEmpty()) return if (allowBlank) null else tr("تاريخ الانتهاء مطلوب")
    return if (runCatching { LocalDate.parse(t) }.isSuccess) null else tr("صيغة التاريخ غير صحيحة، مثال: 2026-12-31")
}

/** Read-only field that opens a searchable list dialog: a "smart dropdown" with no manual UUID typing. */
@Composable
internal fun <T> PickerField(
    label: String,
    selected: T?,
    options: List<T>,
    itemLabel: (T) -> String,
    onSelect: (T) -> Unit,
    placeholder: String = tr("اختر…"),
    enabled: Boolean = true
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected?.let(itemLabel).orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        Box(modifier = Modifier.matchParentSize().clickable(enabled = enabled) { open = true })
    }
    if (open) {
        var query by remember { mutableStateOf("") }
        val filtered = remember(query, options) {
            val q = query.trim().lowercase()
            if (q.isEmpty()) options else options.filter { itemLabel(it).lowercase().contains(q) }
        }
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(label, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        placeholder = { Text(tr("بحث…")) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (filtered.isEmpty()) {
                        Text(tr("لا توجد نتائج"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                            items(filtered) { item ->
                                Text(
                                    itemLabel(item),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelect(item); open = false }
                                        .padding(vertical = 12.dp, horizontal = 4.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text(tr("إغلاق")) } }
        )
    }
}
