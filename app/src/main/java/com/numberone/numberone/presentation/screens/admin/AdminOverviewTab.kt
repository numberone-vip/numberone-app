@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.numberone.numberone.presentation.screens.admin

import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.core.localization.trf
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.numberone.numberone.core.components.EmptyState
import com.numberone.numberone.core.components.NOButton
import com.numberone.numberone.core.components.NOCard
import com.numberone.numberone.core.components.NoticeBanner
import com.numberone.numberone.core.components.SectionTitle
import com.numberone.numberone.core.components.StatusTone
import com.numberone.numberone.core.components.formatAmount
import com.numberone.numberone.core.components.formatDateTime
import androidx.compose.material3.HorizontalDivider
import com.numberone.numberone.core.components.ltr
import com.numberone.numberone.core.components.statusTone
import com.numberone.numberone.core.components.successColor
import com.numberone.numberone.core.components.warningColor
import com.numberone.numberone.core.components.dangerColor
import com.numberone.numberone.domain.model.AppPaymentConfig
import com.numberone.numberone.presentation.util.staleOpenOrders
import com.numberone.numberone.presentation.viewmodel.AdminViewModel
import java.time.LocalDate
import java.time.YearMonth

/** Thresholds (hours) offered for the "stale orders" list. */
private val STALE_THRESHOLDS = listOf(2, 6, 12, 24)

/** Number of wallet ledger rows shown on the dashboard. */
private const val LEDGER_PREVIEW = 20

/**
 * Dashboard. Counters match the Orders and Sham tabs: they count only items the admin must decide
 * (receipt uploaded). Items still waiting for a customer receipt are reported separately as info.
 */
internal fun LazyListScope.overviewItems(
    vm: AdminViewModel,
    actionOrders: Int,
    waitingOrders: Int,
    actionSham: Int,
    waitingSham: Int,
    onGo: (Int) -> Unit
) {
    val summary = vm.summary
    if (summary == null) {
        item { EmptyState(if (vm.loading) tr("جارٍ تحميل البيانات…") else tr("لا توجد بيانات بعد. اضغط زر التحديث.")) }
        return
    }

    val today = LocalDate.now()
    val activeNumbers = vm.subscriptions.count { sub ->
        val expiry = runCatching { LocalDate.parse(sub.expiryDate.orEmpty().trim().take(10)) }.getOrNull()
        val live = statusTone(sub.status) != StatusTone.ERROR
        live && (expiry == null || !expiry.isBefore(today))
    }
    // Revenue = orders that were approved/completed (price is USDT).
    val revenue = vm.orders.filter { statusTone(it.status) == StatusTone.SUCCESS }.sumOf { it.price }
    val usersCount = if (vm.users.isNotEmpty()) vm.users.size else summary.customers

    item {
        NOCard {
            Text(tr("يحتاج إلى مراجعتك"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (actionOrders == 0 && actionSham == 0) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = successColor())
                    Text(tr("كل شيء مُراجَع. لا توجد طلبات تنتظر قرارك."), color = successColor(), fontWeight = FontWeight.SemiBold)
                }
            } else {
                if (actionOrders > 0) {
                    AttentionRow(
                        text = tr("طلبات الشراء والاشتراكات"),
                        detail = tr("إيصالات مرفوعة بانتظار قرارك"),
                        count = actionOrders,
                        action = tr("مراجعة")
                    ) { onGo(TAB_ORDERS) }
                }
                if (actionSham > 0) {
                    AttentionRow(
                        text = tr("إيداعات شام كاش"),
                        detail = tr("إيصالات مرفوعة بانتظار قرارك"),
                        count = actionSham,
                        action = tr("مراجعة")
                    ) { onGo(TAB_SHAM) }
                }
            }
            val waitingTotal = waitingOrders + waitingSham
            if (waitingTotal > 0) {
                Text(
                    trf("{0} بانتظار رفع العميل للإيصال (لا يتطلب إجراءً منك)", waitingTotal),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (summary.pendingDeposits > 0) {
                Text(
                    trf("{0} إيداع USDT قيد التحقق التلقائي (لا يتطلب إجراءً).", summary.pendingDeposits),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    item {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiCard(Modifier.weight(1f), tr("إجمالي المستخدمين"), usersCount.toString(), Icons.Filled.Person)
                KpiCard(Modifier.weight(1f), tr("الأرقام النشطة"), activeNumbers.toString(), Icons.Filled.Phone)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiCard(Modifier.weight(1f), tr("إجمالي الطلبات"), vm.orders.size.toString(), Icons.Filled.ShoppingCart)
                KpiCard(Modifier.weight(1f), tr("حجم الإيرادات"), ltr("${formatAmount(revenue)} USDT"), Icons.Filled.AttachMoney)
            }
        }
    }

    item { StaleOrdersCard(vm = vm, onGo = onGo) }
    item { FinanceReportCard(vm = vm) }
    item { WalletLedgerCard(vm = vm) }
    item { PaymentConfigCard(vm) }
}

/** Open orders that have waited longer than a configurable number of hours. Oldest first. */
@Composable
private fun StaleOrdersCard(vm: AdminViewModel, onGo: (Int) -> Unit) {
    var thresholdHours by remember { mutableIntStateOf(6) }
    val stale = remember(vm.orders, thresholdHours) { staleOpenOrders(vm.orders, thresholdHours.toLong()) }
    NOCard {
        SectionTitle(tr("طلبات متأخرة"), tr("طلبات مفتوحة لم يُتخذ فيها قرار منذ المدة المحددة"))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            STALE_THRESHOLDS.forEach { h ->
                FilterChip(
                    selected = thresholdHours == h,
                    onClick = { thresholdHours = h },
                    label = { Text(trf("أكثر من {0} ساعة", h)) }
                )
            }
        }
        if (stale.isEmpty()) {
            Text(
                trf("لا توجد طلبات معلّقة منذ أكثر من {0} ساعة.", thresholdHours),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            stale.take(8).forEach { (order, hours) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            order.serviceName.ifBlank { tr("طلب شراء") },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            vm.emailFor(order.userId, order.customerEmail),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        ageText(hours),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = dangerColor()
                    )
                }
            }
            if (stale.size > 8) {
                TextButton(onClick = { onGo(TAB_ORDERS) }) { Text(trf("عرض الكل ({0})", stale.size)) }
            }
        }
    }
}

/** Daily and monthly deposits/sales plus total wallet balances, computed from loaded records. */
@Composable
private fun FinanceReportCard(vm: AdminViewModel) {
    val today = LocalDate.now()
    val currentMonth = YearMonth.from(today)
    val daily = remember(vm.orders, vm.deposits, vm.shamTopUps) { dailyTotals(vm.orders, vm.deposits, vm.shamTopUps) }
    val todayT = daily[today] ?: Totals()
    val monthT = totalsWhere(daily) { YearMonth.from(it) == currentMonth }
    val walletsTotal = vm.users.sumOf { it.balance }
    val last7 = (0L..6L).map { today.minusDays(it) }.map { d -> d.toString() to (daily[d] ?: Totals()) }
    val last6 = (0L..5L).map { currentMonth.minusMonths(it) }.map { ym ->
        ym.toString() to totalsWhere(daily) { YearMonth.from(it) == ym }
    }

    NOCard {
        SectionTitle(
            tr("التقارير المالية"),
            tr("الإيداعات بتاريخ الاعتماد، والمبيعات بتاريخ الإنشاء، من السجلات المحمّلة")
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KpiCard(Modifier.weight(1f), tr("إيداعات اليوم"), ltr("${formatAmount(todayT.deposits)} USDT"), Icons.Filled.AccountBalanceWallet)
            KpiCard(Modifier.weight(1f), tr("مبيعات اليوم"), ltr("${formatAmount(todayT.sales)} USDT"), Icons.Filled.AttachMoney)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KpiCard(Modifier.weight(1f), tr("إيداعات الشهر"), ltr("${formatAmount(monthT.deposits)} USDT"), Icons.Filled.AccountBalanceWallet)
            KpiCard(Modifier.weight(1f), tr("مبيعات الشهر"), ltr("${formatAmount(monthT.sales)} USDT"), Icons.Filled.AttachMoney)
        }
        KpiCard(Modifier.fillMaxWidth(), tr("إجمالي أرصدة المحافظ"), ltr("${formatAmount(walletsTotal)} USDT"), Icons.Filled.AccountBalanceWallet)
        ReportTable(title = tr("آخر 7 أيام"), rows = last7)
        ReportTable(title = tr("آخر 6 أشهر"), rows = last6)
    }
}

@Composable
private fun ReportTable(title: String, rows: List<Pair<String, Totals>>) {
    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(tr("الفترة"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(tr("إيداعات"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
        Text(tr("مبيعات"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
    }
    rows.forEach { (label, t) ->
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(ltr(label), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            Text(ltr(formatAmount(t.deposits)), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
            Text(ltr(formatAmount(t.sales)), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
        }
    }
}

@Composable
private fun AttentionRow(text: String, detail: String, count: Int, action: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Filled.Schedule, contentDescription = null, tint = warningColor(), modifier = Modifier.size(20.dp))
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        CountPill(count.toString(), warningColor())
        TextButton(onClick = onClick) { Text(action, fontWeight = FontWeight.Bold) }
    }
}

/** Live payment + app configuration (get_payment_config / admin_update_payment_config). */
@Composable
private fun PaymentConfigCard(vm: AdminViewModel) {
    val base = vm.paymentConfig ?: AppPaymentConfig()
    var wallet by remember(base) { mutableStateOf(base.binanceWalletAddress) }
    var shamAccount by remember(base) { mutableStateOf(base.shamCashAccount) }
    var shamName by remember(base) { mutableStateOf(base.shamCashName) }
    var sypRate by remember(base) { mutableStateOf(if (base.sypUsdRate > 0) base.sypUsdRate.toString() else "") }
    var promo by remember(base) { mutableStateOf(base.promoBannerText) }
    var localError by remember(base) { mutableStateOf<String?>(null) }

    val parsedSypRate = sypRate.trim().ifEmpty { "0" }.toDoubleOrNull()
    val dirty = wallet.trim() != base.binanceWalletAddress ||
        shamAccount.trim() != base.shamCashAccount ||
        shamName.trim() != base.shamCashName ||
        parsedSypRate != base.sypUsdRate ||
        promo.trim() != base.promoBannerText

    fun edited() { localError = null; vm.clearConfigNotice() }

    NOCard {
        SectionTitle(tr("إعدادات الدفع والتطبيق"), tr("تُحفظ على الخادم وتظهر للمستخدمين مباشرة"))
        OutlinedTextField(
            value = wallet,
            onValueChange = { wallet = it; edited() },
            label = { Text(tr("عنوان محفظة Binance (BEP20)")) },
            singleLine = true,
            textStyle = ltrTextStyle(),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = shamAccount,
            onValueChange = { shamAccount = it; edited() },
            label = { Text(tr("رقم حساب شام كاش")) },
            singleLine = true,
            textStyle = ltrTextStyle(),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = shamName,
            onValueChange = { shamName = it; edited() },
            label = { Text(tr("اسم صاحب حساب شام كاش")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = sypRate,
            onValueChange = { sypRate = it; edited() },
            label = { Text(tr("سعر صرف الليرة مقابل الدولار (اختياري)")) },
            singleLine = true,
            textStyle = ltrTextStyle(),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = promo,
            onValueChange = { promo = it; edited() },
            label = { Text(tr("نص الإعلان داخل التطبيق (اختياري)")) },
            minLines = 2,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            tr("إعدادات الدفع والإعلان تُقرأ من الخادم وتنعكس على حسابات المستخدمين."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        (localError ?: vm.configNotice)?.let { text ->
            NoticeBanner(text = text, isError = localError != null || vm.configNoticeIsError)
        }
        NOButton(
            text = tr("حفظ الإعدادات"),
            loading = vm.loading,
            enabled = dirty,
            onClick = {
                val rate = sypRate.trim().ifEmpty { "0" }.toDoubleOrNull()
                if (rate == null || !rate.isFinite() || rate < 0) {
                    localError = tr("أدخل سعر صرف صحيحاً أو اترك الحقل فارغاً")
                } else {
                    vm.savePaymentConfig(
                        AppPaymentConfig(
                            binanceWalletAddress = wallet.trim(),
                            binanceNetwork = base.binanceNetwork,
                            shamCashAccount = shamAccount.trim(),
                            shamCashName = shamName.trim(),
                            sypUsdRate = rate,
                            promoBannerText = promo.trim()
                        )
                    )
                }
            }
        )
    }
}

/** Latest wallet ledger rows across all users, each with the balance after that transaction. */
@Composable
private fun WalletLedgerCard(vm: AdminViewModel) {
    NOCard {
        SectionTitle(tr("سجل حركات المحافظ"), tr("آخر الحركات لكل المستخدمين مع الرصيد بعد كل عملية"))
        val rows = vm.ledger.take(LEDGER_PREVIEW)
        if (rows.isEmpty()) {
            Text(
                tr("لا توجد حركات بعد"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            rows.forEach { tx ->
                val debit = tx.direction.trim().equals("debit", ignoreCase = true)
                val sign = if (debit) "−" else "+"
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            vm.emailFor(tx.userId),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            ltr(sign + formatAmount(tx.amount) + " " + tx.currency),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (debit) dangerColor() else successColor()
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            ledgerTypeLabel(tx.transactionType) + " • " + formatDateTime(tx.createdAt),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            tr("الرصيد: ") + ltr(formatAmount(tx.balanceAfter)),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

/** Human label for a ledger transaction type. Unknown types are shown as stored. */
private fun ledgerTypeLabel(type: String): String = when (type.trim()) {
    "admin_adjustment" -> tr("تعديل إداري")
    else -> type.replace('_', ' ')
}

