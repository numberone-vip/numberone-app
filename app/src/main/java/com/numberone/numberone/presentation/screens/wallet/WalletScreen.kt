package com.numberone.numberone.presentation.screens.wallet

import com.numberone.numberone.core.theme.NumberOneColors
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.HorizontalDivider
import com.numberone.numberone.core.components.CopyIconButton
import com.numberone.numberone.core.components.ExpiryCountdown
import com.numberone.numberone.core.util.parseInstantOrNull
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
import androidx.compose.ui.text.font.FontFamily
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import com.numberone.numberone.core.components.rememberReceiptBitmap
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.CircularProgressIndicator

private const val TX_PREVIEW_COUNT = 8
private val BinanceGold = BinanceYellow

@Composable
fun WalletScreen(vm: AppViewModel, onBack: () -> Unit) {
    LaunchedEffect(Unit) { vm.loadWallet() }

    var method by rememberSaveable { mutableIntStateOf(0) }
    var receiptTargetId by remember { mutableStateOf<String?>(null) }
    var pendingReceipt by remember { mutableStateOf<ReceiptFile?>(null) }
    var showAllTx by remember { mutableStateOf(false) }
    var txFilter by rememberSaveable { mutableIntStateOf(TX_ALL) }

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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ChoicePill(text = "USDT • BEP20", selected = method == 0, onClick = { method = 0 }, modifier = Modifier.weight(1f))
            ChoicePill(text = tr("شام كاش"), selected = method == 1, onClick = { method = 1 }, modifier = Modifier.weight(1f))
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
                // Filter chips: all, incoming, outgoing.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ChoicePill(text = tr("الكل"), selected = txFilter == TX_ALL, onClick = { txFilter = TX_ALL; showAllTx = false }, modifier = Modifier.weight(1f))
                    ChoicePill(text = tr("واردة"), selected = txFilter == TX_IN, onClick = { txFilter = TX_IN; showAllTx = false }, modifier = Modifier.weight(1f))
                    ChoicePill(text = tr("صادرة"), selected = txFilter == TX_OUT, onClick = { txFilter = TX_OUT; showAllTx = false }, modifier = Modifier.weight(1f))
                }
                val filtered = vm.transactions.filter { tx ->
                    val incoming = isIncoming(tx)
                    when (txFilter) {
                        TX_IN -> incoming
                        TX_OUT -> !incoming
                        else -> true
                    }
                }
                if (filtered.isEmpty()) {
                    EmptyState(tr("لا توجد حركات في هذا التصنيف."))
                } else {
                    val visible = if (showAllTx) filtered else filtered.take(TX_PREVIEW_COUNT)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        visible.forEachIndexed { index, tx ->
                            TransactionRow(tx)
                            if (index < visible.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                    if (filtered.size > TX_PREVIEW_COUNT) {
                        TextButton(onClick = { showAllTx = !showAllTx }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (showAllTx) tr("إخفاء") else trf("عرض الكل ({0})", filtered.size), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Binance (USDT) flow
// ---------------------------------------------------------------------------


/** Unique deposit amounts carry 4 decimals (e.g. 15.0437); formatAmount would round them. */
private fun exactUsdt(value: Double): String =
    String.format(java.util.Locale.US, "%.4f", value).trimEnd('0').trimEnd('.')

@Composable
private fun BinanceSection(vm: AppViewModel) {
    fun isFreshOpen(d: WalletTopUp): Boolean {
        if (!isOpenStatus(d.status)) return false
        if (d.status.equals("pending", true)) {
            val exp = parseInstantOrNull(d.createdAt)?.plusSeconds(24L * 3600)?.toEpochMilli()
            if (exp != null && exp <= System.currentTimeMillis()) return false
        }
        return true
    }
    val selected = vm.deposits.firstOrNull { it.id == vm.selectedDeposit && isFreshOpen(it) }
    var liveCfg by remember { mutableStateOf(com.numberone.numberone.data.remote.SupabaseWalletDataSource.LiveDepositConfig()) }
    LaunchedEffect(Unit) { liveCfg = com.numberone.numberone.data.remote.SupabaseWalletDataSource().getLiveDepositConfig() }
    val address = liveCfg.usdtAddress ?: vm.pendingOrder?.walletAddress?.takeIf { it.isNotBlank() }

    NOCard {
        DepositProgress(current = if (selected == null) 1 else 2)
        SectionTitle(tr("كم تريد إضافته؟"), tr("اختر مبلغاً أو أدخل مبلغاً آخر"))
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
            label = { Text(tr("مبلغ آخر")) },
            suffix = { Text("USDT", fontWeight = FontWeight.SemiBold) },
            textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(16.dp)
        )
        NOButton(
            text = tr("إنشاء طلب"),
            onClick = vm::topUp,
            enabled = !vm.loading && vm.amount.isNotBlank()
        )
    }

    if (selected != null) {
        PaymentTicket(
            amount = exactUsdt(selected.requestedAmount),
            status = selected.status,
            address = address,
            loading = vm.loading,
            onCheck = vm::checkDeposit,
            expiresAt = depositExpiry(selected.createdAt)
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

/** Three-segment progress bar: amount, transfer, verification. */
@Composable
private fun DepositProgress(current: Int) {
    val labels = listOf(tr("المبلغ"), tr("التحويل"), tr("التحقق"))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            val active = index + 1 <= current
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** The transfer ticket: amount as the hero figure, then a ruled table of details. */
/** The unique amount is valid for 24 hours from the moment the request was created (backend rule). */
private fun depositExpiry(createdAt: String?): String? =
    parseInstantOrNull(createdAt)?.plusSeconds(24L * 3600)?.toString()

@Composable
private fun PaymentTicket(
    amount: String,
    status: String,
    address: String?,
    loading: Boolean,
    onCheck: () -> Unit,
    expiresAt: String? = null
) {
    NOCard {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tr("تفاصيل التحويل"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    tr("أرسل المبلغ الظاهر بالضبط، فهو ما يربط تحويلك بطلبك"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusChip(status)
        }

        // Time left to send the exact amount; the amount stops matching after this.
        ExpiryCountdown(expiresAt = expiresAt)

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(vertical = 18.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(tr("المبلغ المطلوب"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    CopyIconButton(value = amount, contentDescription = tr("نسخ المبلغ"))
                }
                Text(
                    ltr("$amount USDT"),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Ruled table: label on the start side, value and copy action on the end side.
        Column {
            TicketRow(label = tr("المبلغ"), value = ltr("$amount USDT"), copyValue = amount)
            TicketRow(label = tr("الشبكة"), value = "BEP20")
            if (address != null) {
                TicketRow(label = tr("العنوان"), value = address, copyValue = address, monospace = true)
            }
        }

        WarningStrip(tr("حوّل عبر شبكة BEP20 فقط. أي شبكة أخرى قد تضيع أموالك."))

        NOButton(
            text = tr("تحقق من الإيداع"),
            onClick = onCheck,
            enabled = !loading
        )
    }
}

@Composable
private fun TicketRow(label: String, value: String, copyValue: String? = null, monospace: Boolean = false) {
    val clipboard = LocalClipboardManager.current
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.35f)
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDirection = TextDirection.Ltr,
                    fontFamily = if (monospace) FontFamily.Monospace else null
                ),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(0.5f)
            )
            if (copyValue != null) {
                IconButton(onClick = { clipboard.setText(AnnotatedString(copyValue)) }) {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = tr("نسخ"),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
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
                    Text(ltr(exactUsdt(deposit.requestedAmount) + " USDT"), fontWeight = FontWeight.Bold)
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
        DepositProgress(current = if (active == null) 1 else 2)
        SectionTitle(tr("أنشئ طلب شحن"), tr("اختر العملة التي ستدفع بها وأدخل المبلغ"))
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
            SectionTitle(tr("حوّل ثم ارفع الإيصال"), tr("ستظهر هنا تفاصيل التحويل ومنطقة رفع الإيصال بعد إنشاء الطلب."))
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
    NOCard {
        SectionTitle(tr("حوّل المبلغ عبر شام كاش"), tr("انسخ التفاصيل ثم نفّذ التحويل من تطبيق شام كاش"))
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(vertical = 18.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(tr("المبلغ الذي ستحوّله"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    ltr("${formatAmount(topUp.paidAmount)} $currency".trim()),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Column {
            if ((liveCfg.shamName ?: (liveCfg.shamAccount ?: PaymentConfig.SHAM_CASH_ACCOUNT)_NAME).isNotBlank()) {
                TicketRow(label = tr("اسم صاحب الحساب"), value = (liveCfg.shamName ?: (liveCfg.shamAccount ?: PaymentConfig.SHAM_CASH_ACCOUNT)_NAME), copyValue = (liveCfg.shamName ?: (liveCfg.shamAccount ?: PaymentConfig.SHAM_CASH_ACCOUNT)_NAME))
            }
            if ((liveCfg.shamAccount ?: PaymentConfig.SHAM_CASH_ACCOUNT).isNotBlank()) {
                TicketRow(label = tr("حساب شام كاش"), value = (liveCfg.shamAccount ?: PaymentConfig.SHAM_CASH_ACCOUNT), copyValue = (liveCfg.shamAccount ?: PaymentConfig.SHAM_CASH_ACCOUNT), monospace = true)
            }
            TicketRow(label = tr("رقم الطلب"), value = "#" + topUp.id.take(8), copyValue = topUp.id, monospace = true)
        }
    }

    NOCard {
        SectionTitle(tr("ارفع إيصال التحويل"), tr("تُضاف قيمة USDT إلى محفظتك بعد مراجعة الإدارة"))
        ApprovalTimeline(hasReceipt = pendingReceipt != null, status = "pending")
        if (pendingReceipt == null) {
            ReceiptUploadPanel(onPick = onPick, enabled = !loading)
        } else {
            ReceiptReviewPanel(
                file = pendingReceipt,
                loading = loading,
                onConfirm = onConfirm,
                onReplace = onPick,
                onRemove = onRemove
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


/** Upload step, empty state: one clear card with the photo icon and a single action. */
@Composable
private fun ReceiptUploadPanel(onPick: () -> Unit, enabled: Boolean) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = accent.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = accent, modifier = Modifier.size(40.dp))
            Text(tr("ارفع صورة الإيصال"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                tr("لقطة شاشة من شام كاش تُظهر المبلغ والتاريخ ورقم الحوالة"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            NOButton(text = tr("اختيار من المعرض"), onClick = onPick, enabled = enabled)
        }
    }
}

/** Review step: framed preview, a ruled table of file details, then send / replace / remove. */
@Composable
private fun ReceiptReviewPanel(
    file: ReceiptFile,
    loading: Boolean,
    onConfirm: () -> Unit,
    onReplace: () -> Unit,
    onRemove: () -> Unit
) {
    val preview by rememberReceiptBitmap(file, 1280)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
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
            } else {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
            }
        }
        Column {
            TicketRow(label = tr("نوع الملف"), value = file.extension.uppercase())
            TicketRow(label = tr("الحجم"), value = "${file.bytes.size / 1024} KB")
            TicketRow(label = tr("الحالة"), value = tr("جاهز للإرسال"))
        }
        NOButton(
            text = tr("إرسال الإيصال للمراجعة"),
            onClick = onConfirm,
            enabled = !loading
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onReplace, enabled = !loading, modifier = Modifier.weight(1f)) {
                Text(tr("استبدال"))
            }
            OutlinedButton(onClick = onRemove, enabled = !loading, modifier = Modifier.weight(1f)) {
                Text(tr("حذف"))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                tr("يُحفظ الإيصال بشكل خاص ولا يراه إلا فريق المراجعة."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Three-stage approval track: created, receipt uploaded, admin approved (or rejected). */
@Composable
private fun ApprovalTimeline(hasReceipt: Boolean, status: String) {
    val normalized = status.trim().lowercase()
    val rejected = normalized == "rejected"
    val approved = normalized == "credited"
    val stages = listOf(
        tr("تم إنشاء الطلب") to true,
        tr("تم رفع الإيصال") to hasReceipt,
        when {
            rejected -> tr("مرفوض")
            approved -> tr("تمت الموافقة")
            else -> tr("بانتظار الموافقة")
        } to (approved || rejected)
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        stages.forEach { (label, done) ->
            val tone = when {
                rejected && label == stages.last().first -> dangerColor()
                done -> successColor()
                else -> MaterialTheme.colorScheme.outlineVariant
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(tone))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (done) FontWeight.Bold else FontWeight.Medium,
                    color = if (done) tone else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
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
            ApprovalTimeline(hasReceipt = topUp.receiptPath != null, status = topUp.status)
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
    val incoming = isIncoming(tx)
    val tint = if (incoming) successColor() else NumberOneColors.GoldDeep
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconBadge(
            icon = if (incoming) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
            tint = tint,
            size = 40.dp
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(transactionLabel(tx), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                formatDateTime(tx.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = ltr((if (incoming) "+" else "−") + formatAmount(tx.amount) + " " + tx.currency),
                fontWeight = FontWeight.Black,
                color = if (incoming) successColor() else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = tr("الرصيد بعد: ") + ltr(formatAmount(tx.balanceAfter)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End
            )
        }
    }
}

private const val TX_ALL = 0
private const val TX_IN = 1
private const val TX_OUT = 2

private fun isIncoming(tx: WalletTransaction): Boolean =
    tx.direction.trim().lowercase() in setOf("credit", "in", "incoming")

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
