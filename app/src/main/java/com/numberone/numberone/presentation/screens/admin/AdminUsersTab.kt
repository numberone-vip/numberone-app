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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.numberone.numberone.core.components.CopyIconButton
import com.numberone.numberone.core.components.EmptyState
import com.numberone.numberone.core.components.ExpiryBadge
import com.numberone.numberone.core.components.InitialAvatar
import com.numberone.numberone.core.components.NOCard
import com.numberone.numberone.core.components.StatusChip
import com.numberone.numberone.core.components.dangerColor
import com.numberone.numberone.core.components.formatAmount
import com.numberone.numberone.core.components.ltr
import com.numberone.numberone.core.components.successColor
import com.numberone.numberone.domain.model.AdminSubscription
import com.numberone.numberone.domain.model.AdminUser
import com.numberone.numberone.domain.model.Order
import com.numberone.numberone.presentation.viewmodel.AdminViewModel

internal fun LazyListScope.userItems(
    vm: AdminViewModel,
    query: String,
    onQuery: (String) -> Unit,
    onAdjust: (AdminUser) -> Unit,
    onDetails: (AdminUser) -> Unit
) {
    phoneSearchItems(vm)
    item {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            singleLine = true,
            placeholder = { Text(tr("ابحث بالبريد الإلكتروني…")) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
    }
    val q = query.trim().lowercase()
    val list = if (q.isEmpty()) vm.users else vm.users.filter { (it.email ?: "").lowercase().contains(q) }
    if (list.isEmpty() && !vm.loading) {
        item { EmptyState(if (vm.users.isEmpty()) tr("لا يوجد مستخدمون") else tr("لا توجد نتائج مطابقة")) }
    }
    items(list, key = { "user-" + it.userId }) { u ->
        UserCard(u, subsCount = vm.subscriptions.count { it.userId == u.userId }, loading = vm.loading,
            onAdjust = { onAdjust(u) }, onDetails = { onDetails(u) })
    }
}

@Composable
private fun UserCard(user: AdminUser, subsCount: Int, loading: Boolean, onAdjust: () -> Unit, onDetails: () -> Unit) {
    NOCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InitialAvatar(user.email, size = 44.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(user.email ?: user.userId, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                if (user.role != null) {
                    CountPill(
                        if (user.isAdmin) "Admin" else "User",
                        if (user.isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            CopyIconButton(value = user.email ?: user.userId)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("الرصيد"), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                ltr("${formatAmount(user.balance)} ${user.currency}"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onAdjust, enabled = !loading, modifier = Modifier.weight(1f)) { Text(tr("تعديل الرصيد")) }
            OutlinedButton(onClick = onDetails, enabled = !loading, modifier = Modifier.weight(1f)) { Text(trf("الأرقام ({0})", subsCount)) }
        }
    }
}

/** +/- USDT adjustment with a mandatory audit note and a live preview of the resulting balance. */
@Composable
internal fun AdjustBalanceDialog(user: AdminUser, onDismiss: () -> Unit, onConfirm: (amount: Double, note: String, requestId: String) -> Unit) {
    // One id per opened dialog: a retried confirm cannot apply the adjustment twice.
    val requestId = remember(user.userId) { java.util.UUID.randomUUID().toString() }
    var add by remember(user.userId) { mutableStateOf(true) }
    var amount by remember(user.userId) { mutableStateOf("") }
    var note by remember(user.userId) { mutableStateOf("") }
    var error by remember(user.userId) { mutableStateOf<String?>(null) }
    val value = amount.trim().toDoubleOrNull()
    val signed = if (value != null) (if (add) value else -value) else null
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(tr("تعديل رصيد المحفظة"), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()) {
                Text(user.email ?: user.userId, fontWeight = FontWeight.SemiBold)
                Text(
                    tr("الرصيد الحالي: ") + ltr("${formatAmount(user.balance)} ${user.currency}"),
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = add, onClick = { add = true; error = null }, label = { Text(tr("إضافة (+)")) })
                    FilterChip(selected = !add, onClick = { add = false; error = null }, label = { Text(tr("خصم (−)")) })
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it; error = null },
                    label = { Text(tr("المبلغ بالـ USDT")) },
                    singleLine = true,
                    textStyle = ltrTextStyle(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it; error = null },
                    label = { Text(tr("ملاحظة التدقيق (مطلوبة)")) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                if (signed != null && signed.isFinite() && signed != 0.0) {
                    val after = user.balance + signed
                    Text(
                        tr("الرصيد بعد التعديل: ") + ltr("${formatAmount(after)} ${user.currency}"),
                        color = if (after < 0) dangerColor() else successColor(),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                error?.let { Text(it, color = dangerColor(), style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    signed == null || !signed.isFinite() || signed == 0.0 -> error = tr("أدخل مبلغاً صحيحاً أكبر من صفر")
                    kotlin.math.abs(signed) > 1_000_000.0 -> error = tr("المبلغ كبير جداً")
                    user.balance + signed < 0.0 -> error = tr("لا يمكن أن يصبح الرصيد سالباً")
                    note.isBlank() -> error = tr("اكتب ملاحظة التدقيق")
                    else -> onConfirm(signed, note.trim(), requestId)
                }
            }) { Text(tr("تأكيد"), color = successColor(), fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("إلغاء")) } }
    )
}

/** User's numbers, recent orders and access controls (renew expiry / rotate access code). */
@Composable
internal fun UserDetailsDialog(
    user: AdminUser,
    subscriptions: List<AdminSubscription>,
    orders: List<Order>,
    onDismiss: () -> Unit,
    onManageAccess: (AdminSubscription) -> Unit,
    onAssign: () -> Unit,
    canChangeRole: Boolean = false,
    onChangeRole: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(user.email ?: user.userId, fontWeight = FontWeight.Bold, maxLines = 1) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()) {
                Text(trf("الأرقام والاشتراكات ({0})", subscriptions.size), fontWeight = FontWeight.Bold)
                if (subscriptions.isEmpty()) {
                    Text(tr("لا توجد اشتراكات لهذا المستخدم"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                subscriptions.forEach { s ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                (s.serviceName?.takeIf { it.isNotBlank() } ?: tr("اشتراك")) + " • " + ltr(s.phoneNumber),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            StatusChip(s.status)
                        }
                        ExpiryBadge(s.expiryDate)
                        TextButton(onClick = { onManageAccess(s) }) { Text(tr("تحديث الوصول / التمديد")) }
                    }
                    HorizontalDivider()
                }
                TextButton(onClick = onAssign) { Text(tr("إسناد اشتراك جديد"), fontWeight = FontWeight.Bold) }
                if (canChangeRole) {
                    TextButton(onClick = onChangeRole) {
                        Text(
                            if (user.isAdmin) tr("إزالة صلاحية المدير") else tr("ترقية إلى مدير"),
                            color = if (user.isAdmin) dangerColor() else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(trf("آخر الطلبات ({0})", orders.size), fontWeight = FontWeight.Bold)
                if (orders.isEmpty()) {
                    Text(tr("لا توجد طلبات"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                orders.take(8).forEach { o ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            o.serviceName.ifBlank { tr("طلب") } + " • " + ltr("${formatAmount(o.price)} USDT"),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        StatusChip(o.status)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("إغلاق")) } }
    )
}

/** Reset/update access for one subscription: rotate the code and/or set a new expiry. */
@Composable
internal fun ResetAccessDialog(sub: AdminSubscription, onDismiss: () -> Unit, onConfirm: (code: String, expiry: String) -> Unit) {
    var code by remember(sub.id) { mutableStateOf("") }
    var expiry by remember(sub.id) { mutableStateOf("") }
    var error by remember(sub.id) { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(tr("تحديث وصول الاشتراك"), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()) {
                Text(
                    (sub.serviceName ?: tr("اشتراك")) + " • " + ltr(sub.phoneNumber),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    tr("اترك الحقل فارغاً للإبقاء على القيمة الحالية. رمز الوصول الجديد يُبطل الرمز القديم فوراً."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AccessFields(
                    code = code,
                    onCode = { code = it; error = null },
                    expiry = expiry,
                    onExpiry = { expiry = it; error = null },
                    expiryError = error,
                    codeLabel = tr("رمز وصول جديد (اختياري)")
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val dErr = dateError(expiry, allowBlank = true)
                when {
                    code.isBlank() && expiry.isBlank() -> error = tr("أدخل رمزاً جديداً أو تاريخ انتهاء")
                    dErr != null -> error = dErr
                    else -> onConfirm(code.trim(), expiry.trim())
                }
            }) { Text(tr("حفظ"), color = successColor(), fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("إلغاء")) } }
    )
}
