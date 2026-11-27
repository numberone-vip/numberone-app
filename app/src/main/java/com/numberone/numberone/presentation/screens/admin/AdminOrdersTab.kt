@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.numberone.numberone.presentation.screens.admin

import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.core.localization.trf
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.numberone.numberone.core.components.CopyField
import com.numberone.numberone.core.components.EmptyState
import com.numberone.numberone.core.components.ExpiryBadge
import com.numberone.numberone.core.components.NOCard
import com.numberone.numberone.core.components.StatusChip
import com.numberone.numberone.core.components.StatusTone
import com.numberone.numberone.core.components.dangerColor
import com.numberone.numberone.core.components.formatAmount
import com.numberone.numberone.core.components.formatDateTime
import com.numberone.numberone.core.components.ltr
import com.numberone.numberone.core.components.statusTone
import com.numberone.numberone.core.components.successColor
import com.numberone.numberone.core.components.warningColor
import com.numberone.numberone.domain.model.AdminService
import com.numberone.numberone.domain.model.AdminSubscription
import com.numberone.numberone.domain.model.AdminUser
import com.numberone.numberone.domain.model.Order
import com.numberone.numberone.domain.model.isOpenStatus
import com.numberone.numberone.presentation.components.OrderProgress
import com.numberone.numberone.presentation.util.TxidConflict
import com.numberone.numberone.presentation.util.TxidRecord
import com.numberone.numberone.presentation.util.ageHours
import com.numberone.numberone.presentation.util.hasReceipt
import com.numberone.numberone.presentation.util.isWalletOrder
import com.numberone.numberone.presentation.util.needsAdminAction
import com.numberone.numberone.presentation.util.newestFirst
import com.numberone.numberone.presentation.util.oldestFirst
import com.numberone.numberone.presentation.util.orderMatchesSearch
import com.numberone.numberone.presentation.util.stage
import com.numberone.numberone.presentation.util.txidConflict
import com.numberone.numberone.presentation.util.waitingForCustomer
import com.numberone.numberone.presentation.viewmodel.AdminViewModel

/** Orders at or beyond this age are shown in the error colour on the review card. */
private const val STALE_HOURS = 6L

/** Expiry shortcuts offered when approving an order. */
private val APPROVAL_DURATION_DAYS = listOf(7, 30, 60, 90, 180, 365)

internal enum class OrderFilter(private val labelKey: String) {
    ACTION("يحتاج قرارك"),
    WAITING("بانتظار العميل"),
    APPROVED("معتمد"),
    REJECTED("مرفوض"),
    ALL("الكل"),
    SUBSCRIPTIONS("الاشتراكات");

    /** Localized label, read on each use so it follows the selected language. */
    val label: String get() = tr(labelKey)
}

private fun Order.matches(filter: OrderFilter): Boolean = when (filter) {
    OrderFilter.ACTION -> needsAdminAction()
    OrderFilter.WAITING -> waitingForCustomer()
    OrderFilter.APPROVED -> statusTone(status) == StatusTone.SUCCESS
    OrderFilter.REJECTED -> statusTone(status) == StatusTone.ERROR
    OrderFilter.ALL -> true
    OrderFilter.SUBSCRIPTIONS -> false
}

/**
 * Orders tab. Two clearly separated groups: "يحتاج قرارك" (receipt uploaded, admin must decide)
 * and "بانتظار العميل" (customer still has to upload a receipt). Oldest first, so the longest
 * waiting order is always at the top. Search matches order ID, e-mail, phone, or TXID.
 */
internal fun LazyListScope.orderItems(
    vm: AdminViewModel,
    filter: OrderFilter,
    onFilter: (OrderFilter) -> Unit,
    query: String,
    onQuery: (String) -> Unit,
    txidIndex: Map<String, List<TxidRecord>>,
    onAssign: () -> Unit,
    onApprove: (Order) -> Unit,
    onReject: (Order) -> Unit
) {
    item {
        Button(
            onClick = onAssign,
            enabled = !vm.loading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(tr("إسناد اشتراك لمستخدم"), fontWeight = FontWeight.Bold)
        }
    }
    item {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            singleLine = true,
            placeholder = { Text(tr("رقم الطلب أو البريد أو الهاتف…")) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
    }
    item {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OrderFilter.entries.forEach { f ->
                val count = when (f) {
                    OrderFilter.SUBSCRIPTIONS -> vm.subscriptions.size
                    else -> vm.orders.count { it.matches(f) }
                }
                FilterChip(selected = filter == f, onClick = { onFilter(f) }, label = { Text("${f.label} ($count)") })
            }
        }
    }

    if (filter == OrderFilter.SUBSCRIPTIONS) {
        if (vm.subscriptions.isEmpty() && !vm.loading) item { EmptyState(tr("لا توجد اشتراكات")) }
        items(vm.subscriptions) { sub -> SubscriptionCard(vm, sub) }
        return
    }

    val filtered = vm.orders.filter {
        it.matches(filter) && orderMatchesSearch(it, vm.emailFor(it.userId, it.customerEmail), query)
    }
    if (filtered.isEmpty() && !vm.loading) {
        item {
            EmptyState(
                when {
                    vm.orders.isEmpty() -> tr("لا توجد طلبات")
                    query.isNotBlank() -> tr("لا توجد طلبات تطابق البحث")
                    else -> tr("لا توجد طلبات ضمن هذا التصنيف")
                }
            )
        }
        return
    }

    val action = filtered.filter { it.needsAdminAction() }.oldestFirst()
    val waiting = filtered.filter { it.waitingForCustomer() }.oldestFirst()
    // Triple(title, orders, emphasized). "Emphasized" marks the group that needs the admin.
    val sections: List<Triple<String, List<Order>, Boolean>> = when (filter) {
        OrderFilter.ACTION -> listOf(Triple(OrderFilter.ACTION.label, action, true))
        OrderFilter.WAITING -> listOf(Triple(OrderFilter.WAITING.label, waiting, false))
        OrderFilter.ALL -> listOf(
            Triple(OrderFilter.ACTION.label, action, true),
            Triple(OrderFilter.WAITING.label, waiting, false),
            Triple(tr("السجل"), filtered.filter { !it.needsAdminAction() && !it.waitingForCustomer() }.newestFirst(), false)
        )
        else -> listOf(Triple(filter.label, filtered.newestFirst(), false))
    }

    sections.forEach { (title, list, emphasized) ->
        if (list.isNotEmpty()) {
            item(key = "hdr-$title") { SectionHeader(title = title, count = list.size, emphasized = emphasized) }
            items(list, key = { "order-" + it.id }) { o ->
                OrderReviewCard(
                    order = o,
                    email = vm.emailFor(o.userId, o.customerEmail),
                    conflict = txidConflict(o, txidIndex),
                    loading = vm.loading,
                    onView = { vm.showReceipt(o.receiptPath) },
                    onApprove = { onApprove(o) },
                    onReject = { onReject(o) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int, emphasized: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        CountPill(count.toString(), if (emphasized) warningColor() else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OrderReviewCard(
    order: Order,
    email: String,
    conflict: TxidConflict,
    loading: Boolean,
    onView: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val open = isOpenStatus(order.status)
    val hours = if (open) ageHours(order.createdAt) else null
    NOCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    order.serviceName.ifBlank { tr("طلب شراء") },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    formatDateTime(order.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (hours != null) {
                    Text(
                        ageText(hours),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hours >= STALE_HOURS) dangerColor() else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            StatusChip(order.status)
        }
        Text(
            ltr("${formatAmount(order.price)} USDT"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            tr("طريقة الدفع: ") + orderPaymentLabel(order.paymentMethod),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (order.isWalletOrder()) {
            Text(
                tr("دُفع من رصيد المحفظة"),
                style = MaterialTheme.typography.bodySmall,
                color = successColor(),
                fontWeight = FontWeight.SemiBold
            )
        } else {
            OrderProgress(stage = order.stage())
        }
        if (conflict.others > 0) {
            Text(
                trf("تنبيه: رقم المعاملة مستخدم في {0} سجل آخر.", conflict.others),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (conflict.blocking) dangerColor() else warningColor()
            )
        }
        order.phoneNumber?.takeIf { it.isNotBlank() }?.let { CopyField(label = tr("الرقم المُسنَد"), value = it) }
        CopyField(label = tr("معرّف الطلب"), value = order.id)
        order.transactionRef?.takeIf { it.isNotBlank() }?.let { CopyField(label = tr("رقم المعاملة"), value = it) }
        order.rejectionReason?.takeIf { it.isNotBlank() }?.let {
            Text(trf("سبب الرفض: {0}", it), color = dangerColor(), style = MaterialTheme.typography.bodySmall)
        }
        if (open && !order.hasReceipt() && !order.isWalletOrder()) {
            Text(
                tr("بانتظار إرسال العميل لإثبات الدفع"),
                style = MaterialTheme.typography.bodySmall,
                color = warningColor(),
                fontWeight = FontWeight.SemiBold
            )
        }
        ReviewActions(
            canDecide = order.needsAdminAction(),
            hasReceipt = order.hasReceipt(),
            loading = loading,
            approveText = tr("موافقة وإسناد رقم"),
            approveEnabled = !conflict.blocking,
            onView = onView,
            onApprove = onApprove,
            onReject = onReject
        )
    }
}

@Composable
private fun SubscriptionCard(vm: AdminViewModel, sub: AdminSubscription) {
    NOCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    sub.serviceName?.takeIf { it.isNotBlank() } ?: tr("اشتراك"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    vm.emailFor(sub.userId, sub.customerEmail),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusChip(sub.status)
        }
        if (sub.phoneNumber.isNotBlank()) CopyField(label = tr("الرقم"), value = sub.phoneNumber)
        if (sub.id.isNotBlank()) CopyField(label = tr("معرّف الاشتراك (UUID)"), value = sub.id)
        ExpiryBadge(sub.expiryDate)
        sub.adminNote?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        var smsOpen by remember(sub.id) { mutableStateOf(false) }
        var sender by remember(sub.id) { mutableStateOf("Number One") }
        var body by remember(sub.id) { mutableStateOf("") }
        androidx.compose.material3.OutlinedButton(onClick = { smsOpen = true }, enabled = !vm.loading, modifier = Modifier.fillMaxWidth()) {
            Text(tr("إرسال كود / رسالة SMS للعميل"), fontWeight = FontWeight.Bold)
        }
        if (smsOpen) {
            AlertDialog(
                onDismissRequest = { smsOpen = false },
                title = { Text(tr("إرسال كود SMS للعميل"), fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = sender, onValueChange = { sender = it }, label = { Text(tr("المرسل (مثال: Facebook / WhatsApp)")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text(tr("نص الرسالة أو كود التفعيل")) }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (sender.isNotBlank() && body.isNotBlank()) {
                            vm.sendSubscriptionSms(sub.id, sender, body)
                            smsOpen = false; body = ""
                        }
                    }) { Text(tr("إرسال الآن"), color = successColor(), fontWeight = FontWeight.Bold) }
                },
                dismissButton = { TextButton(onClick = { smsOpen = false }) { Text(tr("إلغاء")) } }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Dialogs
// ---------------------------------------------------------------------------

/** "موافقة وإسناد رقم": order summary, phone, generated access code, expiry with duration shortcuts. */
@Composable
internal fun ApproveOrderDialog(
    order: Order,
    email: String,
    conflict: TxidConflict,
    onDismiss: () -> Unit,
    onConfirm: (phone: String, expiry: String) -> Unit
) {
    var phone by remember(order.id) { mutableStateOf(order.phoneNumber.orEmpty()) }
    var expiry by remember(order.id) { mutableStateOf("") }
    var phoneError by remember(order.id) { mutableStateOf<String?>(null) }
    var expiryError by remember(order.id) { mutableStateOf<String?>(null) }
    var formError by remember(order.id) { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(tr("موافقة وإسناد رقم"), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()) {
                // Summary of what is being approved, filled in from the order itself.
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(order.serviceName.ifBlank { tr("طلب شراء") }, fontWeight = FontWeight.SemiBold)
                    Text(email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        ltr("${formatAmount(order.price)} USDT") + "  •  " + orderPaymentLabel(order.paymentMethod),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (conflict.blocking) {
                    Text(
                        tr("لا يمكن اعتماد هذا الطلب لأن رقم المعاملة معتمد أو مسجّل قبله في عملية أخرى."),
                        style = MaterialTheme.typography.bodySmall,
                        color = dangerColor(),
                        fontWeight = FontWeight.SemiBold
                    )
                } else if (conflict.others > 0) {
                    Text(
                        trf("تنبيه: رقم المعاملة مستخدم في {0} سجل آخر. تحقق قبل الاعتماد.", conflict.others),
                        style = MaterialTheme.typography.bodySmall,
                        color = warningColor(),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; phoneError = null; formError = null },
                    label = { Text(tr("رقم الهاتف")) },
                    singleLine = true,
                    isError = phoneError != null,
                    supportingText = { phoneError?.let { Text(it) } },
                    textStyle = ltrTextStyle(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = expiry, onValueChange = { expiry = it; expiryError = null; formError = null }, label = { Text(tr("تاريخ الانتهاء (yyyy-MM-dd)")) }, singleLine = true, isError = expiryError != null, supportingText = { expiryError?.let { Text(it) } }, textStyle = ltrTextStyle(), modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    APPROVAL_DURATION_DAYS.forEach { days -> val target = java.time.LocalDate.now().plusDays(days.toLong()).toString(); androidx.compose.material3.FilterChip(selected = expiry.trim() == target, onClick = { expiry = target; expiryError = null; formError = null }, label = { Text(durationLabel(days)) }) }
                }
                formError?.let { Text(it, color = dangerColor(), style = MaterialTheme.typography.bodySmall) }
                Text(
                    tr("سيتم اعتماد الاشتراك وإرجاع معرّف UUID للرقم مباشرة دون رمز وصول."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val dErr = dateError(expiry, allowBlank = false)
                when {
                    conflict.blocking -> formError = tr("رقم المعاملة مستخدم في عملية أسبق أو معتمدة. لا يمكن اعتماد هذا الطلب.")
                    phone.isBlank() -> phoneError = tr("رقم الهاتف مطلوب")
                    dErr != null -> expiryError = dErr
                    else -> onConfirm(phone.trim(), expiry.trim())
                }
            }) { Text(tr("تأكيد الموافقة"), color = successColor(), fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("إلغاء")) } }
    )
}

/** "إسناد اشتراك لمستخدم": smart pickers for user and service, no UUID typing. */
@Composable
internal fun AssignSubscriptionDialog(
    users: List<AdminUser>,
    services: List<AdminService>,
    preselectedUser: AdminUser? = null,
    onDismiss: () -> Unit,
    onConfirm: (userId: String, serviceId: String, phone: String, code: String, expiry: String) -> Unit
) {
    var user by remember { mutableStateOf(preselectedUser) }
    var service by remember { mutableStateOf<AdminService?>(null) }
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf(newAccessCode()) }
    var expiry by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }
    var expiryError by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(tr("إسناد اشتراك لمستخدم"), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()) {
                PickerField(
                    label = tr("المستخدم"),
                    selected = user,
                    options = users,
                    itemLabel = { it.email ?: it.userId },
                    onSelect = { user = it; formError = null },
                    placeholder = tr("اختر مستخدماً"),
                    enabled = preselectedUser == null
                )
                PickerField(
                    label = tr("الخدمة"),
                    selected = service,
                    options = services,
                    itemLabel = { s -> listOf(s.name, s.country).filter { it.isNotBlank() }.joinToString(" • ") + " — " + formatAmount(s.price) + " USDT" },
                    onSelect = { service = it; formError = null },
                    placeholder = tr("اختر خدمة")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; formError = null },
                    label = { Text(tr("رقم الهاتف المُسنَد")) },
                    singleLine = true,
                    textStyle = ltrTextStyle(),
                    modifier = Modifier.fillMaxWidth()
                )
                AccessFields(
                    code = code,
                    onCode = { code = it; formError = null },
                    expiry = expiry,
                    onExpiry = { expiry = it; expiryError = null },
                    expiryError = expiryError
                )
                formError?.let { Text(it, color = dangerColor(), style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val dErr = dateError(expiry, allowBlank = false)
                when {
                    user == null -> formError = tr("اختر المستخدم")
                    service == null -> formError = tr("اختر الخدمة")
                    phone.isBlank() -> formError = tr("رقم الهاتف مطلوب")
                    code.isBlank() -> formError = tr("رمز الوصول مطلوب")
                    dErr != null -> expiryError = dErr
                    else -> onConfirm(user!!.userId, service!!.id, phone.trim(), code.trim(), expiry.trim())
                }
            }) { Text(tr("إسناد"), color = successColor(), fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("إلغاء")) } }
    )
}
