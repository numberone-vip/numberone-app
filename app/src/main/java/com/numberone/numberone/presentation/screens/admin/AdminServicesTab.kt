@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.numberone.numberone.presentation.screens.admin
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.imePadding

import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.core.localization.trf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.numberone.numberone.core.components.EmptyState
import com.numberone.numberone.core.components.NOCard
import com.numberone.numberone.core.components.dangerColor
import com.numberone.numberone.core.components.formatAmount
import com.numberone.numberone.core.components.ltr
import com.numberone.numberone.core.components.successColor
import com.numberone.numberone.domain.model.AdminService
import com.numberone.numberone.domain.model.ServiceDraft
import com.numberone.numberone.presentation.viewmodel.AdminViewModel

internal fun LazyListScope.serviceItems(
    vm: AdminViewModel,
    onAdd: () -> Unit,
    onEdit: (AdminService) -> Unit,
    onDelete: (AdminService) -> Unit
) {
    item { VirtualMarginCard(vm) }
    item {
        Button(
            onClick = onAdd,
            enabled = !vm.loading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(tr("  إضافة خدمة جديدة"), fontWeight = FontWeight.Bold)
        }
    }
    if (vm.services.isEmpty() && !vm.loading) {
        item { EmptyState(tr("لا توجد خدمات في الكتالوج بعد")) }
    }
    items(vm.services, key = { "svc-" + it.id }) { s ->
        ServiceCard(
            service = s,
            loading = vm.loading,
            onToggle = { vm.setServiceActive(s, it) },
            onEdit = { onEdit(s) },
            onDelete = { onDelete(s) }
        )
    }
}

@Composable
private fun ServiceCard(
    service: AdminService,
    loading: Boolean,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    NOCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(service.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                val meta = listOf(service.country, service.countryCode).filter { it.isNotBlank() }.joinToString(" • ")
                if (meta.isNotEmpty()) {
                    Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Switch(checked = service.isActive, onCheckedChange = onToggle, enabled = !loading)
        }
        Text(
            ltr("${formatAmount(service.price)} USDT"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        service.description?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (service.isActive) tr("مفعّلة") else tr("معطّلة"),
                color = if (service.isActive) successColor() else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onEdit, enabled = !loading) { Icon(Icons.Filled.Edit, contentDescription = tr("تعديل")) }
            IconButton(onClick = onDelete, enabled = !loading) {
                Icon(Icons.Filled.Delete, contentDescription = tr("حذف"), tint = dangerColor())
            }
        }
    }
}

/** Add (service == null) or edit a catalog package. */
@Composable
internal fun ServiceEditorDialog(service: AdminService?, onDismiss: () -> Unit, onSave: (ServiceDraft) -> Unit) {
    var name by remember(service?.id) { mutableStateOf(service?.name.orEmpty()) }
    var country by remember(service?.id) { mutableStateOf(service?.country.orEmpty()) }
    var code by remember(service?.id) { mutableStateOf(service?.countryCode.orEmpty()) }
    var price by remember(service?.id) { mutableStateOf(service?.let { formatAmount(it.price) }.orEmpty()) }
    var description by remember(service?.id) { mutableStateOf(service?.description.orEmpty()) }
    var active by remember(service?.id) { mutableStateOf(service?.isActive ?: true) }
    var error by remember(service?.id) { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(if (service == null) tr("إضافة خدمة") else tr("تعديل الخدمة"), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it; error = null },
                    label = { Text(tr("اسم الخدمة")) }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = country, onValueChange = { country = it },
                    label = { Text(tr("الدولة")) }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = code, onValueChange = { code = it.take(8) },
                    label = { Text(tr("رمز الدولة (مثال: SY أو +963)")) }, singleLine = true,
                    textStyle = ltrTextStyle(), modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price, onValueChange = { price = it; error = null },
                    label = { Text(tr("السعر بالـ USDT")) }, singleLine = true,
                    textStyle = ltrTextStyle(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text(tr("الوصف")) }, minLines = 2, modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("مفعّلة للعملاء"), modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Switch(checked = active, onCheckedChange = { active = it })
                }
                error?.let { Text(it, color = dangerColor(), style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val p = price.trim().toDoubleOrNull()
                when {
                    name.isBlank() -> error = tr("اسم الخدمة مطلوب")
                    p == null || !p.isFinite() || p < 0.0 -> error = tr("أدخل سعراً صحيحاً")
                    else -> onSave(ServiceDraft(service?.id, name.trim(), country.trim(), code.trim(), p, description.trim(), active))
                }
            }) { Text(tr("حفظ"), color = successColor(), fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("إلغاء")) } }
    )
}

@Composable
internal fun DeleteServiceDialog(service: AdminService, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(tr("حذف الخدمة"), fontWeight = FontWeight.Bold) },
        text = {
            Text(trf("سيتم حذف «{0}» نهائياً. إن كانت مرتبطة بطلبات سابقة فعطّلها بدلاً من حذفها.", service.name))
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(tr("حذف"), color = dangerColor(), fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("إلغاء")) } }
    )
}

/** Profit margin (percent) applied on top of the 5SIM base price for virtual numbers. */
@Composable
private fun VirtualMarginCard(vm: AdminViewModel) {
    val current = vm.virtualMargin
    var text by remember(current) { mutableStateOf(current?.let { formatAmount(it) }.orEmpty()) }
    var confirm by remember { mutableStateOf(false) }
    val parsed = text.trim().toDoubleOrNull()
    val valid = parsed != null && parsed.isFinite() && parsed >= 0.0 && parsed <= 500.0
    val changed = valid && (current == null || kotlin.math.abs(parsed!! - current) > 1e-9)

    NOCard {
        Text(tr("هامش ربح الأرقام الافتراضية"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            if (current == null) tr("تعذر قراءة الهامش الحالي") else tr("الهامش الحالي: ") + ltr(formatAmount(current) + "%"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(tr("النسبة المئوية %")) },
            singleLine = true,
            isError = text.isNotBlank() && !valid,
            supportingText = { Text(tr("تُضاف إلى سعر 5SIM للطلبات الجديدة فقط؛ الطلبات الجارية تحتفظ بهامشها.")) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = ltrTextStyle(),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { confirm = true },
            enabled = changed && !vm.loading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) { Text(tr("حفظ الهامش"), fontWeight = FontWeight.Bold) }
    }
    if (confirm && parsed != null) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(tr("تأكيد تغيير الهامش"), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    trf("سيصبح هامش الربح {0}% على الأرقام الافتراضية الجديدة.", formatAmount(parsed))
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.saveVirtualMargin(parsed); confirm = false }) {
                    Text(tr("تأكيد"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(tr("إلغاء")) } }
        )
    }
}
