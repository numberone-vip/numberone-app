@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.numberone.numberone.presentation.screens.admin

import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.core.localization.trf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.numberone.numberone.core.components.FeedbackBanner
import com.numberone.numberone.core.components.NOTopBar
import com.numberone.numberone.core.components.NoticeBanner
import com.numberone.numberone.core.components.ReceiptViewerDialog
import com.numberone.numberone.domain.model.AdminService
import com.numberone.numberone.domain.model.AdminShamCashTopUp
import com.numberone.numberone.domain.model.AdminSubscription
import com.numberone.numberone.domain.model.AdminUser
import com.numberone.numberone.domain.model.Order
import com.numberone.numberone.domain.model.isOpenStatus
import com.numberone.numberone.presentation.util.indexTxids
import com.numberone.numberone.presentation.util.needsAdminAction
import com.numberone.numberone.presentation.util.txidConflict
import com.numberone.numberone.presentation.util.waitingForCustomer
import com.numberone.numberone.presentation.viewmodel.AdminViewModel

internal const val TAB_OVERVIEW = 0
internal const val TAB_ORDERS = 1
internal const val TAB_SHAM = 2
internal const val TAB_SERVICES = 3
internal const val TAB_USERS = 4

/**
 * Admin Command Center: five tabs (Overview, Orders & Subscriptions, Sham Cash & Deposits,
 * Services & Pricing, Users & Wallets). UI state lives here; every action goes through
 * [AdminViewModel] and the server re-checks is_admin() inside each RPC.
 */
@Composable
fun AdminScreen(onBack: () -> Unit) {
    val vm = remember { AdminViewModel() }
    LaunchedEffect(Unit) { vm.load() }

    var tab by remember { mutableIntStateOf(TAB_OVERVIEW) }
    var orderFilter by remember { mutableStateOf(OrderFilter.ACTION) }
    var orderQuery by remember { mutableStateOf("") }
    var shamPendingOnly by remember { mutableStateOf(true) }
    var userQuery by remember { mutableStateOf("") }

    var approveOrder by remember { mutableStateOf<Order?>(null) }
    var rejectOrder by remember { mutableStateOf<Order?>(null) }
    var rejectSham by remember { mutableStateOf<AdminShamCashTopUp?>(null) }
    var assignOpen by remember { mutableStateOf(false) }
    var assignUser by remember { mutableStateOf<AdminUser?>(null) }
    var serviceEditor by remember { mutableStateOf<AdminService?>(null) }
    var serviceEditorOpen by remember { mutableStateOf(false) }
    var deleteService by remember { mutableStateOf<AdminService?>(null) }
    var adjustUser by remember { mutableStateOf<AdminUser?>(null) }
    var detailUser by remember { mutableStateOf<AdminUser?>(null) }
    var accessSub by remember { mutableStateOf<AdminSubscription?>(null) }

    // Counters only include items the admin must decide (receipt uploaded). Items still waiting for
    // the customer's receipt are shown separately and do not inflate the "needs action" counts.
    val actionOrders = vm.orders.count { it.needsAdminAction() }
    val waitingOrders = vm.orders.count { it.waitingForCustomer() }
    val actionSham = vm.shamTopUps.count { isOpenStatus(it.status) && it.receiptPath != null }
    val waitingSham = vm.shamTopUps.count { isOpenStatus(it.status) && it.receiptPath == null }
    // Transaction-hash index across orders and deposits, used to flag and block reused TXIDs.
    val txidIndex = remember(vm.orders, vm.deposits) { indexTxids(vm.orders, vm.deposits) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NOTopBar(
                title = tr("لوحة الإدارة"),
                onBack = onBack,
                actions = {
                    IconButton(onClick = { vm.load() }, enabled = !vm.loading) {
                        Icon(Icons.Filled.Refresh, contentDescription = tr("تحديث"))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PrimaryScrollableTabRow(selectedTabIndex = tab, edgePadding = 8.dp) {
                val labels = listOf(
                    tr("نظرة عامة"),
                    if (actionOrders > 0) trf("الطلبات والاشتراكات ({0})", actionOrders) else tr("الطلبات والاشتراكات"),
                    if (actionSham > 0) trf("شام كاش والإيداعات ({0})", actionSham) else tr("شام كاش والإيداعات"),
                    tr("الخدمات والأسعار"),
                    tr("المستخدمون والأرصدة")
                )
                labels.forEachIndexed { index, label ->
                    Tab(
                        selected = tab == index,
                        onClick = { tab = index },
                        text = { Text(label, maxLines = 1) }
                    )
                }
            }

            // Feedback stays pinned above the list so it is visible whichever tab is scrolled.
            val hasFeedback = vm.loading || vm.error != null || vm.message != null || vm.warning != null
            if (hasFeedback) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FeedbackBanner(loading = vm.loading, error = vm.error, message = vm.message)
                    vm.warning?.let { NoticeBanner(text = it, isError = true) }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (tab) {
                    TAB_OVERVIEW -> {
                        overviewItems(vm = vm, actionOrders = actionOrders, waitingOrders = waitingOrders, actionSham = actionSham, waitingSham = waitingSham, onGo = { tab = it })
                        item(key = "admin-send-notif") { AdminSendNotificationCard(vm) }
                    }
                    TAB_ORDERS -> orderItems(
                        vm = vm,
                        filter = orderFilter,
                        onFilter = { orderFilter = it },
                        query = orderQuery,
                        onQuery = { orderQuery = it },
                        txidIndex = txidIndex,
                        onAssign = { assignUser = null; assignOpen = true },
                        onApprove = { approveOrder = it },
                        onReject = { rejectOrder = it }
                    )
                    TAB_SHAM -> shamItems(
                        vm = vm,
                        pendingOnly = shamPendingOnly,
                        openCount = actionSham,
                        onFilter = { shamPendingOnly = it },
                        onApprove = { t, usdt -> vm.approveShamCash(t.id, usdt) },
                        onReject = { rejectSham = it }
                    )
                    TAB_SERVICES -> {
                    item(key = "admin-telnyx-card") { com.numberone.numberone.data.remote.TelnyxAdminCard() }
                    serviceItems(
                        vm = vm,
                        onAdd = { serviceEditor = null; serviceEditorOpen = true },
                        onEdit = { serviceEditor = it; serviceEditorOpen = true },
                        onDelete = { deleteService = it }
                    )
                    else -> userItems(
                        vm = vm,
                        query = userQuery,
                        onQuery = { userQuery = it },
                        onAdjust = { adjustUser = it },
                        onDetails = { detailUser = it }
                    )
                }
            }
        }
    }

    // ---- Orders --------------------------------------------------------------
    approveOrder?.let { o ->
        ApproveOrderDialog(
            order = o,
            email = vm.emailFor(o.userId, o.customerEmail),
            conflict = txidConflict(o, txidIndex),
            onDismiss = { approveOrder = null },
            onConfirm = { phone, expiry ->
                vm.approveOrder(o.id, phone, expiry)
                approveOrder = null
            }
        )
    }
    rejectOrder?.let { o ->
        ReasonDialog(
            title = tr("رفض الطلب"),
            onDismiss = { rejectOrder = null },
            onConfirm = { reason -> vm.rejectOrder(o.id, reason); rejectOrder = null }
        )
    }
    if (assignOpen) {
        AssignSubscriptionDialog(
            users = vm.users,
            services = vm.services.filter { it.isActive },
            preselectedUser = assignUser,
            onDismiss = { assignOpen = false },
            onConfirm = { userId, serviceId, phone, code, expiry ->
                vm.assignSubscription(userId, serviceId, phone, code, expiry)
                assignOpen = false
            }
        )
    }

    // ---- Sham Cash -----------------------------------------------------------
    rejectSham?.let { t ->
        ReasonDialog(
            title = tr("رفض الإيداع"),
            onDismiss = { rejectSham = null },
            onConfirm = { reason -> vm.rejectShamCash(t.id, reason); rejectSham = null }
        )
    }

    // ---- Services ------------------------------------------------------------
    if (serviceEditorOpen) {
        ServiceEditorDialog(
            service = serviceEditor,
            onDismiss = { serviceEditorOpen = false },
            onSave = { draft -> vm.saveService(draft); serviceEditorOpen = false }
        )
    }
    deleteService?.let { s ->
        DeleteServiceDialog(
            service = s,
            onDismiss = { deleteService = null },
            onConfirm = { vm.deleteService(s.id); deleteService = null }
        )
    }

    // ---- Users ---------------------------------------------------------------
    adjustUser?.let { u ->
        AdjustBalanceDialog(
            user = u,
            onDismiss = { adjustUser = null },
            onConfirm = { amount, note, requestId -> vm.adjustBalance(u.userId, amount, note, requestId); adjustUser = null }
        )
    }
    detailUser?.let { u ->
        UserDetailsDialog(
            user = u,
            subscriptions = vm.subscriptions.filter { it.userId == u.userId },
            orders = vm.orders.filter { it.userId == u.userId },
            onDismiss = { detailUser = null },
            onManageAccess = { accessSub = it },
            onAssign = { assignUser = u; assignOpen = true }
        )
    }
    accessSub?.let { s ->
        ResetAccessDialog(
            sub = s,
            onDismiss = { accessSub = null },
            onConfirm = { code, expiry -> vm.resetAccess(s.id, code, expiry); accessSub = null }
        )
    }

    // ---- Receipt viewer (full-screen, zoomable) --------------------------------
    vm.receiptUrl?.let { url ->
        ReceiptViewerDialog(url = url, onDismiss = { vm.closeReceipt() })
    }
}

@Composable
private fun AdminSendNotificationCard(vm: AdminViewModel) {
    var sendAll by remember { mutableStateOf(true) }
    var selectedUser by remember { mutableStateOf<AdminUser?>(null) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    com.numberone.numberone.core.components.NOCard {
        com.numberone.numberone.core.components.SectionTitle(tr("📢 إرسال إشعار للعملاء"), tr("أرسل تنبيهاً فورياً لجميع العملاء أو لعميل محدد"))
        androidx.compose.foundation.layout.Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            androidx.compose.material3.FilterChip(selected = sendAll, onClick = { sendAll = true }, label = { Text(tr("جميع العملاء")) })
            androidx.compose.material3.FilterChip(selected = !sendAll, onClick = { sendAll = false }, label = { Text(tr("عميل محدد")) })
        }
        if (!sendAll) {
            PickerField(
                label = tr("اختر العميل"),
                selected = selectedUser,
                options = vm.users,
                itemLabel = { (it.email ?: it.userId) + " • " + it.userId.take(8) },
                onSelect = { selectedUser = it },
                placeholder = tr("اختر عميلاً")
            )
        }
        androidx.compose.material3.OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(tr("عنوان الإشعار")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        androidx.compose.material3.OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text(tr("نص الرسالة")) }, modifier = Modifier.fillMaxWidth())
        com.numberone.numberone.core.components.NOButton(
            text = if (sendAll) tr("إرسال لجميع العملاء") else tr("إرسال للعميل المحدد"),
            enabled = title.isNotBlank() && body.isNotBlank() && (sendAll || selectedUser != null),
            onClick = {
                vm.sendNotification(if (sendAll) null else selectedUser?.userId, title, body)
                title = ""; body = ""
            }
        )
    }
}
