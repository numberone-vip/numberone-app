@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.numberone.numberone.presentation.screens.admin

import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.core.localization.trf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.numberone.numberone.core.components.NOCard
import com.numberone.numberone.core.components.SectionTitle
import com.numberone.numberone.core.components.StatusChip
import com.numberone.numberone.core.components.dangerColor
import com.numberone.numberone.core.components.formatAmount
import com.numberone.numberone.core.components.formatDateTime
import com.numberone.numberone.core.components.ltr
import com.numberone.numberone.core.components.successColor
import com.numberone.numberone.core.components.warningColor
import com.numberone.numberone.domain.model.AdminShamCashTopUp
import com.numberone.numberone.domain.model.AdminWalletDeposit
import com.numberone.numberone.domain.model.isOpenStatus
import com.numberone.numberone.presentation.viewmodel.AdminViewModel

internal fun LazyListScope.shamItems(
    vm: AdminViewModel,
    pendingOnly: Boolean,
    openCount: Int,
    onFilter: (Boolean) -> Unit,
    depositQuery: String,
    onDepositQuery: (String) -> Unit,
    onApprove: (AdminShamCashTopUp, Double) -> Unit,
    onReject: (AdminShamCashTopUp) -> Unit
) {
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = pendingOnly, onClick = { onFilter(true) }, label = { Text(trf("قيد المراجعة ({0})", openCount)) })
            FilterChip(selected = !pendingOnly, onClick = { onFilter(false) }, label = { Text(trf("الكل ({0})", vm.shamTopUps.size)) })
        }
    }
    // "Pending" means the admin has something to decide: an open top-up WITH a receipt.
    // Open top-ups still waiting for the customer's receipt appear only under "الكل".
    val list = if (pendingOnly) vm.shamTopUps.filter { isOpenStatus(it.status) && it.receiptPath != null } else vm.shamTopUps
    if (list.isEmpty() && !vm.loading) {
        item {
            EmptyState(
                if (pendingOnly && vm.shamTopUps.isNotEmpty()) tr("لا توجد إيداعات معلّقة. اختر «الكل» لعرض السجل.")
                else tr("لا توجد طلبات شام كاش")
            )
        }
    }
    val rate = vm.paymentConfig?.sypUsdRate ?: 0.0
    items(list, key = { "sham-" + it.id }) { t ->
        ShamReviewCard(
            topUp = t,
            email = vm.emailFor(t.userId, t.customerEmail),
            sypRate = rate,
            loading = vm.loading,
            onView = { vm.showReceipt(t.receiptPath) },
            onApprove = { usdt -> onApprove(t, usdt) },
            onReject = { onReject(t) }
        )
    }

    // Automatic USDT (Binance) deposits: read-only, searchable by e-mail, TXID or status.
    if (vm.deposits.isNotEmpty()) {
        val q = depositQuery.trim().lowercase()
        val matched = if (q.isEmpty()) vm.deposits.take(10) else vm.deposits.filter { d ->
            vm.emailFor(d.userId).lowercase().contains(q) ||
                (d.transactionHash ?: "").lowercase().contains(q) ||
                d.status.lowercase().contains(q)
        }.take(100)
        item { SectionTitle(tr("إيداعات USDT التلقائية"), trf("{0} عملية (تُعتمد تلقائياً بعد التحقق من الشبكة)", matched.size)) }
        item {
            OutlinedTextField(
                value = depositQuery,
                onValueChange = onDepositQuery,
                singleLine = true,
                placeholder = { Text(tr("البريد أو TXID أو الحالة…")) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (matched.isEmpty()) item { EmptyState(tr("لا توجد إيداعات تطابق البحث")) }
        items(matched, key = { "deposit-" + it.id }) { d -> DepositCard(d, vm.emailFor(d.userId)) }
    }
}

@Composable
private fun ShamReviewCard(
    topUp: AdminShamCashTopUp,
    email: String,
    sypRate: Double,
    loading: Boolean,
    onView: () -> Unit,
    onApprove: (Double) -> Unit,
    onReject: () -> Unit
) {
    val open = isOpenStatus(topUp.status)
    val canDecide = open && topUp.receiptPath != null
    // Suggested USDT amount: 1:1 for dollars, converted with the live rate for Syrian pounds.
    val suggestion = remember(topUp.id, sypRate) {
        val cur = topUp.paidCurrency.orEmpty().trim().uppercase()
        when {
            cur == "USD" || cur == "USDT" || topUp.paymentMethod == "SHAM_CASH_USD" -> formatAmount(topUp.paidAmount)
            sypRate > 0.0 -> formatAmount(topUp.paidAmount / sypRate)
            else -> ""
        }
    }
    var usdt by remember(topUp.id, suggestion) { mutableStateOf(suggestion) }
    val parsed = usdt.trim().toDoubleOrNull()
    val valid = parsed != null && parsed.isFinite() && parsed > 0.0 && parsed <= 1_000_000.0

    NOCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(email, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    formatDateTime(topUp.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusChip(topUp.status)
        }
        Text(
            ltr("${formatAmount(topUp.paidAmount)} ${topUp.paidCurrency.orEmpty()}"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        Text(shamLabel(topUp.paymentMethod), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        topUp.creditedUsdt?.let {
            Text(tr("أُضيف للمحفظة: ") + ltr(formatAmount(it) + " USDT"), color = successColor(), fontWeight = FontWeight.SemiBold)
        }
        topUp.rejectionReason?.let {
            Text(trf("سبب الرفض: {0}", it), color = dangerColor(), style = MaterialTheme.typography.bodySmall)
        }
        if (open && topUp.receiptPath == null) {
            Text(
                tr("بانتظار رفع العميل للإيصال"),
                style = MaterialTheme.typography.bodySmall,
                color = warningColor(),
                fontWeight = FontWeight.SemiBold
            )
        }
        if (canDecide) {
            OutlinedTextField(
                value = usdt,
                onValueChange = { usdt = it },
                label = { Text(tr("المبلغ بالـ USDT الذي سيُضاف")) },
                singleLine = true,
                isError = usdt.isNotBlank() && !valid,
                supportingText = {
                    Text(
                        when {
                            sypRate > 0.0 && topUp.paymentMethod == "SHAM_CASH_SYP" -> tr("مقترح تلقائياً بسعر الصرف الحالي، عدّله عند الحاجة")
                            else -> tr("عدّل المبلغ حسب ما يظهر في الإيصال")
                        }
                    )
                },
                textStyle = ltrTextStyle(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        }
        ReviewActions(
            canDecide = canDecide,
            hasReceipt = topUp.receiptPath != null,
            loading = loading,
            approveText = tr("اعتماد"),
            approveEnabled = valid,
            onView = onView,
            onApprove = { parsed?.let(onApprove) },
            onReject = onReject
        )
    }
}

@Composable
private fun DepositCard(d: AdminWalletDeposit, email: String) {
    NOCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(email, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    formatDateTime(d.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusChip(d.status)
        }
        Text(
            ltr("${formatAmount(d.verifiedAmount ?: d.requestedAmount)} ${d.currency} • ${d.network}"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        d.transactionHash?.takeIf { it.isNotBlank() }?.let { hash ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    ltr(hash),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                CopyIconButton(value = hash)
            }
        }
    }
}
