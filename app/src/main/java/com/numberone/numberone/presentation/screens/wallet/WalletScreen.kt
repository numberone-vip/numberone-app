package com.numberone.numberone.presentation.screens.wallet

import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.core.localization.trf
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.numberone.numberone.core.components.BalanceHeroCard
import com.numberone.numberone.core.components.BinanceBrandHeader
import com.numberone.numberone.core.components.BinanceLogo
import com.numberone.numberone.core.components.BinanceYellow
import com.numberone.numberone.core.components.ShamCashBrandHeader
import com.numberone.numberone.core.components.ShamCashGreen
import com.numberone.numberone.core.components.ShamCashLogo
import com.numberone.numberone.core.components.ChoicePill
import com.numberone.numberone.core.components.CopyRow
import com.numberone.numberone.core.components.EmptyState
import com.numberone.numberone.core.components.FeedbackBanner
import com.numberone.numberone.core.components.IconBadge
import com.numberone.numberone.core.components.NOButton
import com.numberone.numberone.core.components.NOCard
import com.numberone.numberone.core.components.NOScaffold
import com.numberone.numberone.core.components.NoticeBanner
import com.numberone.numberone.core.components.PaymentMethodCard
import com.numberone.numberone.core.components.PaymentStepHeader
import com.numberone.numberone.core.components.ReceiptDropZone
import com.numberone.numberone.core.components.ReceiptPreviewCard
import com.numberone.numberone.core.components.SectionTitle
import com.numberone.numberone.core.components.StatusChip
import com.numberone.numberone.core.components.StatusTone
import com.numberone.numberone.core.components.WarningStrip
import com.numberone.numberone.core.components.dangerColor
import com.numberone.numberone.core.components.formatAmount
import com.numberone.numberone.core.components.formatDateTime
import com.numberone.numberone.core.components.isValidTxId
import com.numberone.numberone.core.components.ltr
import com.numberone.numberone.core.components.rememberReceiptPicker
import com.numberone.numberone.core.components.statusTone
import com.numberone.numberone.core.components.successColor
import com.numberone.numberone.core.util.PaymentConfig
import com.numberone.numberone.core.util.ReceiptFile
import com.numberone.numberone.domain.model.ShamCashTopUp
import com.numberone.numberone.domain.model.WalletTopUp
import com.numberone.numberone.domain.model.WalletTransaction
import com.numberone.numberone.domain.model.isOpenStatus
import com.numberone.numberone.presentation.viewmodel.AppViewModel

private const val TX_PREVIEW_COUNT = 8
private val BinanceGold = BinanceYellow

@Composable
fun WalletScreen(vm: AppViewModel, onBack: () -> Unit) {
    LaunchedEffect(Unit) { vm.loadWallet() }

    var method by rememberSaveable { mutableIntStateOf(0) }
    var receiptTargetId by remember { mutableStateOf<String?>(null) }
    var pendingReceipt by remember { mutableStateOf<ReceiptFile?>(null) }
    var showAllTx by remember { mutableStateOf(false) }

    val pickReceipt = rememberReceiptPicker(
        onPicked = { pendingReceipt = it },
        onError = { vm.fail(it) }
    )

    // The local copy is only dropped once the server confirms it holds the receipt, so a failed
    // upload keeps the preview on screen and the customer can simply press send again.
    LaunchedEffect(vm.shamTopUps) {
        val target = receiptTargetId
        if (target != null && vm.shamTopUps.firstOrNull { it.id == target }?.receiptPath != null) {
            pendingReceipt = null
            receiptTargetId = null
        }
    }

    NOScaffold(title = tr("المحفظة"), onBack = onBack) {
        BalanceHeroCard(
            label = tr("رصيدك المتاح"),
            amount = ltr("${formatAmount(vm.wallet.balance)} ${vm.wallet.currency}"),
            footer = {
                // Frosted-glass badge with a subtle light edge.
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                ) {
                    Text(
                        tr("الشحن عبر شبكة BEP20 أو شام كاش"),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        )

        FeedbackBanner(loading = vm.loading, error = vm.error, message = vm.message)

        SectionTitle(tr("اختر طريقة الشحن"), tr("اختر ما يناسبك، وسنرشدك خطوة بخطوة"))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            PaymentMethodCard(
                title = "USDT • Binance",
                subtitle = tr("شبكة BEP20 • تحقق تلقائي"),
                icon = Icons.Filled.CurrencyBitcoin,
                accent = BinanceGold,
                logo = { BinanceLogo(42.dp) },
                selected = method == 0,
                onClick = { method = 0 },
                modifier = Modifier.weight(1f)
            )
            PaymentMethodCard(
                title = tr("شام كاش"),
                subtitle = tr("ليرة أو دولار • إيصال ومراجعة"),
                icon = Icons.Filled.Payments,
                accent = ShamCashGreen,
                logo = { ShamCashLogo(42.dp) },
                selected = method == 1,
                onClick = { method = 1 },
                modifier = Modifier.weight(1f)
            )
        }

        if (method == 0) {
            BinanceSection(vm)
        } else {
            ShamSection(
                vm = vm,
                pendingReceipt = pendingReceipt,
                receiptTargetId = receiptTargetId,
                onPickReceipt = { topUpId ->
                    if (receiptTargetId != topUpId) pendingReceipt = null
                    receiptTargetId = topUpId
                    pickReceipt()
                },
                onConfirmReceipt = {
                    val file = pendingReceipt
                    val target = receiptTargetId
                    if (file != null && target != null) {
                        vm.selectSham(target)
                        vm.submitShamReceipt(file.bytes, file.mimeType, file.extension)
                    }
                },
                onRemoveReceipt = {
                    pendingReceipt = null
                    receiptTargetId = null
                }
            )
        }

        NOCard {
            SectionTitle(tr("سجل الحركات"), if (vm.transactions.isEmpty()) null else tr("آخر العمليات على محفظتك"))
            if (vm.transactions.isEmpty()) {
                EmptyState(tr("لا توجد حركات بعد. ستظهر هنا عمليات الشحن والشراء."))
            } else {
                val visible = if (showAllTx) vm.transactions else vm.transactions.take(TX_PREVIEW_COUNT)
                visible.forEach { tx -> TransactionRow(tx) }
                if (vm.transactions.size > TX_PREVIEW_COUNT) {
                    TextButton(onClick = { showAllTx = !showAllTx }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (showAllTx) tr("إخفاء") else trf("عرض الكل ({0})", vm.transactions.size), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Binance (USDT) flow
// ---------------------------------------------------------------------------

@Composable
private fun BinanceSection(vm: AppViewModel) {
    val clipboard = LocalClipboardManager.current
    val txValue = vm.txHash.trim()
    val txInvalid = txValue.isNotEmpty() && !isValidTxId(txValue)
    val selected = vm.deposits.firstOrNull { it.id == vm.selectedDeposit && isOpenStatus(it.status) }
    val address = vm.pendingOrder?.walletAddress?.takeIf { it.isNotBlank() }

    NOCard {
        PaymentStepHeader(1, tr("اختر المبلغ"), tr("كم USDT تريد إضافته إلى محفظتك؟"), done = selected != null)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("10", "25", "50", "100").forEach { preset ->
                ChoicePill(
                    text = preset,
                    selected = vm.amount.trim() == preset,
                    onClick = { vm.setAmount(preset) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        OutlinedTextField(
            value = vm.amount,
            onValueChange = vm::setAmount,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(tr("أو أدخل مبلغاً آخر")) },
            suffix = { Text("USDT", fontWeight = FontWeight.SemiBold) },
            textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(16.dp)
        )
        NOButton(text = tr("إنشاء طلب الإيداع"), onClick = vm::topUp, enabled = !vm.loading && vm.amount.isNotBlank())
    }

    BinanceBrandHeader()
    NOCard {
        PaymentStepHeader(2, tr("حوّل المبلغ"), tr("انسخ التفاصيل ونفّذ التحويل من Binance"), done = false)
        if (selected == null) {
            Text(
                tr("أنشئ طلباً أو اختر طلباً مفتوحاً من القائمة أدناه لتظهر تفاصيل التحويل هنا."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            WarningStrip(tr("حوّل عبر شبكة BEP20 فقط. أي شبكة أخرى قد تضيع أموالك."))
            CopyRow(
                label = tr("المبلغ المطلوب (USDT)"),
                value = ltr(formatAmount(selected.requestedAmount) + " USDT"),
                copyValue = formatAmount(selected.requestedAmount),
                emphasized = true
            )
            CopyRow(label = tr("الشبكة"), value = selected.network.ifBlank { "BEP20" })
            if (address != null) {
                CopyRow(label = tr("عنوان الاستلام • USDT BEP20"), value = address, monospace = true)
            }
        }
    }

    NOCard {
        PaymentStepHeader(3, tr("أكّد بالـ TXID"), tr("الصق رقم المعاملة بعد التحويل للتحقق التلقائي"))
        OutlinedTextField(
            value = vm.txHash,
            onValueChange = vm::setTxHash,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = txInvalid,
            label = { Text(tr("رقم المعاملة TXID")) },
            textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
            supportingText = {
                Text(
                    if (txInvalid) tr("يجب أن يتكوّن من 64 خانة (أرقام وحروف a-f)")
                    else "${ltr(txValue.removePrefix("0x").removePrefix("0X").length.toString())} / 64"
                )
            },
            trailingIcon = {
                IconButton(onClick = {
                    clipboard.getText()?.text?.let { vm.setTxHash(it.trim()) }
                }) {
                    Icon(Icons.Filled.ContentPaste, contentDescription = tr("لصق"), tint = MaterialTheme.colorScheme.primary)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
        NOButton(
            text = tr("تحقّق وأضف الرصيد"),
            onClick = vm::submitDeposit,
            enabled = !vm.loading && selected != null && isValidTxId(txValue)
        )
    }

    NOCard {
        SectionTitle(tr("طلبات الإيداع"), tr("اضغط على طلب مفتوح لاختياره"))
        if (vm.deposits.isEmpty()) EmptyState(tr("لا توجد طلبات إيداع بعد"))
        vm.deposits.forEach { d ->
            DepositRow(
                deposit = d,
                selected = d.id == vm.selectedDeposit,
                onSelect = { vm.selectDeposit(d.id) }
            )
        }
    }
}

@Composable
private fun DepositRow(deposit: WalletTopUp, selected: Boolean, onSelect: () -> Unit) {
    val open = isOpenStatus(deposit.status)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected && open) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        onClick = { if (open) onSelect() }
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(ltr(formatAmount(deposit.requestedAmount) + " USDT"), fontWeight = FontWeight.Bold)
                    Text(
                        formatDateTime(deposit.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusChip(deposit.status)
            }
            if (statusTone(deposit.status) == StatusTone.ERROR && !deposit.adminNote.isNullOrBlank()) {
                Text(trf("سبب الرفض: {0}", deposit.adminNote), color = dangerColor(), style = MaterialTheme.typography.bodySmall)
            }
            if (open) {
                Text(
                    text = if (selected) tr("✓ هذا هو الطلب المحدد") else tr("اضغط لاختيار هذا الطلب"),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Sham Cash flow
// ---------------------------------------------------------------------------

@Composable
private fun ShamSection(
    vm: AppViewModel,
    pendingReceipt: ReceiptFile?,
    receiptTargetId: String?,
    onPickReceipt: (String) -> Unit,
    onConfirmReceipt: () -> Unit,
    onRemoveReceipt: () -> Unit
) {
    val awaitingReceipt = vm.shamTopUps.filter { isOpenStatus(it.status) && it.receiptPath == null }
    val active = awaitingReceipt.firstOrNull { it.id == vm.shamSelected } ?: awaitingReceipt.firstOrNull()

    NOCard {
        PaymentStepHeader(1, tr("أنشئ طلب شحن"), tr("اختر العملة التي ستدفع بها وأدخل المبلغ"), done = active != null)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ChoicePill(
                text = tr("ليرة سورية SYP"),
                selected = vm.shamMethod == "SHAM_CASH_SYP",
                onClick = { vm.setShamMethod("SHAM_CASH_SYP") },
                modifier = Modifier.weight(1f)
            )
            ChoicePill(
                text = tr("دولار USD"),
                selected = vm.shamMethod == "SHAM_CASH_USD",
                onClick = { vm.setShamMethod("SHAM_CASH_USD") },
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = vm.shamAmount,
            onValueChange = vm::setShamAmount,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(tr("المبلغ الذي ستدفعه")) },
            suffix = { Text(if (vm.shamMethod == "SHAM_CASH_USD") "USD" else "SYP", fontWeight = FontWeight.SemiBold) },
            textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(16.dp)
        )
        NOButton(
            text = tr("إنشاء الطلب"),
            onClick = vm::createShamTopUp,
            enabled = !vm.loading && vm.shamAmount.isNotBlank()
        )
    }

    if (active == null) {
        NOCard {
            PaymentStepHeader(2, tr("حوّل ثم ارفع الإيصال"), tr("ستظهر هنا تفاصيل التحويل ومنطقة رفع الإيصال بعد إنشاء الطلب."))
        }
    } else {
        ActiveShamRequest(
            topUp = active,
            loading = vm.loading,
            error = vm.error,
            pendingReceipt = if (receiptTargetId == active.id) pendingReceipt else null,
            onPick = { onPickReceipt(active.id) },
            onConfirm = onConfirmReceipt,
            onRemove = onRemoveReceipt
        )
    }

    NOCard {
        SectionTitle(tr("طلبات شام كاش"), tr("تابع حالة كل طلب من هنا"))
        if (vm.shamTopUps.isEmpty()) EmptyState(tr("لا توجد طلبات شام كاش بعد"))
        vm.shamTopUps.forEach { t ->
            ShamRow(
                topUp = t,
                isActive = t.id == active?.id,
                canSwitch = active != null && t.id != active.id && isOpenStatus(t.status) && t.receiptPath == null,
                onSwitch = { vm.selectSham(t.id) }
            )
        }
    }
}

/** The request the customer is currently paying: transfer details to copy, then the receipt step. */
@Composable
private fun ActiveShamRequest(
    topUp: ShamCashTopUp,
    loading: Boolean,
    error: String?,
    pendingReceipt: ReceiptFile?,
    onPick: () -> Unit,
    onConfirm: () -> Unit,
    onRemove: () -> Unit
) {
    val currency = topUp.paidCurrency.orEmpty()
    ShamCashBrandHeader()
    NOCard {
        PaymentStepHeader(2, tr("حوّل المبلغ عبر شام كاش"), tr("انسخ التفاصيل ثم نفّذ التحويل من تطبيق شام كاش"))
        CopyRow(
            label = tr("المبلغ الذي ستحوّله"),
            value = ltr("${formatAmount(topUp.paidAmount)} $currency".trim()),
            copyValue = formatAmount(topUp.paidAmount),
            emphasized = true
        )
        if (PaymentConfig.SHAM_CASH_ACCOUNT_NAME.isNotBlank()) {
            CopyRow(label = tr("اسم صاحب الحساب"), value = PaymentConfig.SHAM_CASH_ACCOUNT_NAME)
        }
        if (PaymentConfig.SHAM_CASH_ACCOUNT.isNotBlank()) {
            CopyRow(label = tr("حساب شام كاش"), value = PaymentConfig.SHAM_CASH_ACCOUNT, monospace = true)
        }
        CopyRow(
            label = tr("رقم الطلب"),
            value = "#" + topUp.id.take(8),
            copyValue = topUp.id,
            monospace = true
        )
    }

    NOCard {
        PaymentStepHeader(3, tr("ارفع إيصال التحويل"), tr("تُضاف قيمة USDT إلى محفظتك بعد مراجعة الإدارة"), done = false)
        if (pendingReceipt == null) {
            ReceiptDropZone(onPick = onPick, enabled = !loading)
        } else {
            ReceiptPreviewCard(
                file = pendingReceipt,
                loading = loading,
                onConfirm = onConfirm,
                onReplace = onPick,
                onRemove = onRemove,
                confirmLabel = tr("إرسال الإيصال للمراجعة")
            )
            error?.let { NoticeBanner(text = it, isError = true) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                tr("يُحفظ الإيصال بشكل خاص ولا يراه إلا فريق المراجعة."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ShamRow(topUp: ShamCashTopUp, isActive: Boolean, canSwitch: Boolean, onSwitch: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        ltr("${formatAmount(topUp.paidAmount)} ${topUp.paidCurrency.orEmpty()}"),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        shamMethodLabel(topUp.paymentMethod),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusChip(topUp.status)
            }
            topUp.creditedUsdt?.let {
                Text(
                    trf("أُضيف إلى محفظتك: {0}", ltr(formatAmount(it) + " USDT")),
                    color = successColor(),
                    fontWeight = FontWeight.SemiBold
                )
            }
            topUp.rejectionReason?.let {
                Text(trf("سبب الرفض: {0}", it), color = dangerColor(), style = MaterialTheme.typography.bodySmall)
            }
            if (topUp.receiptPath != null && isOpenStatus(topUp.status)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
                    Text(tr("تم رفع الإيصال، بانتظار المراجعة"), style = MaterialTheme.typography.bodySmall)
                }
            } else if (isActive) {
                Text(
                    tr("هذا هو الطلب النشط — ارفع إيصاله من الأعلى"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (canSwitch) {
                TextButton(onClick = onSwitch) {
                    Text(tr("رفع إيصال لهذا الطلب"), fontWeight = FontWeight.SemiBold)
                }
            }
            Text(
                formatDateTime(topUp.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Transactions
// ---------------------------------------------------------------------------

@Composable
private fun TransactionRow(tx: WalletTransaction) {
    val incoming = tx.direction.trim().lowercase() in setOf("credit", "in", "incoming")
    val tint = if (incoming) successColor() else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconBadge(
            icon = if (incoming) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
            tint = tint,
            size = 38.dp
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(transactionLabel(tx), fontWeight = FontWeight.SemiBold)
            Text(
                formatDateTime(tx.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = ltr((if (incoming) "+" else "−") + formatAmount(tx.amount) + " " + tx.currency),
                fontWeight = FontWeight.Bold,
                color = if (incoming) successColor() else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = tr("الرصيد: ") + ltr(formatAmount(tx.balanceAfter)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End
            )
        }
    }
}

private fun transactionLabel(tx: WalletTransaction): String = when (tx.transactionType.trim().lowercase()) {
    "purchase" -> tr("شراء")
    "refund" -> tr("استرداد")
    "deposit", "topup", "top_up" -> tr("شحن رصيد")
    "sham_cash_topup", "sham_cash" -> tr("شحن عبر شام كاش")
    "admin_adjustment", "adjustment" -> tr("تعديل من الإدارة")
    "" -> if (tx.direction.trim().lowercase() in setOf("credit", "in", "incoming")) tr("إيداع") else tr("سحب")
    else -> tx.transactionType.replace('_', ' ')
}

private fun shamMethodLabel(method: String): String = when (method) {
    "SHAM_CASH_SYP" -> tr("شام كاش • ليرة سورية")
    "SHAM_CASH_USD" -> tr("شام كاش • دولار أمريكي")
    else -> tr("شام كاش")
}
