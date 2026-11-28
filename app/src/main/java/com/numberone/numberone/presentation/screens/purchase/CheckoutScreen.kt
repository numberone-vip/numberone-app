package com.numberone.numberone.presentation.screens.purchase

import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.core.localization.trf
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.numberone.numberone.core.components.IconBadge
import com.numberone.numberone.core.components.formatAmount
import com.numberone.numberone.core.components.NOCard
import com.numberone.numberone.core.components.dangerColor
import com.numberone.numberone.core.components.ltr
import com.numberone.numberone.core.components.successColor
import com.numberone.numberone.core.components.warningColor
import com.numberone.numberone.core.util.flagEmoji
import com.numberone.numberone.core.util.serviceDurationLabel
import com.numberone.numberone.domain.model.Service

/**
 * One service on the store page: what it is, what it costs, whether it is in stock, and the two
 * ways to pay (instantly from the wallet, or by a BEP20 USDT transfer with a receipt).
 *
 * Guards in the UI:
 * - Out of stock: both purchase actions are disabled.
 * - Balance below the price: the wallet purchase is disabled and "Top up wallet" is offered.
 * - Wallet purchase asks for confirmation first, showing the amount and the remaining balance.
 *
 * [walletBalance] is optional. When it is unknown the wallet button stays enabled and the server
 * remains the authority: a purchase only goes through when the server accepts it.
 * [onTopUp] is optional; when provided, the "Top up wallet" button is shown for insufficient balance.
 */
@Composable
fun CheckoutScreen(
    service: Service,
    enabled: Boolean,
    onWalletPurchase: (String, Int) -> Unit,
    onManualPayment: (String, Int) -> Unit,
    walletBalance: Double? = null,
    onTopUp: (() -> Unit)? = null
) {
    val options = service.options.ifEmpty { listOf(service) }
    var selectedId by remember(service.id, options) {
        mutableStateOf((options.firstOrNull { (it.availableCount ?: 0) > 0 } ?: options.first()).id)
    }
    val selected = options.firstOrNull { it.id == selectedId } ?: options.first()
    val available = selected.availableCount ?: 0
    val inStock = available > 0
    val stockColor = if (inStock) successColor() else dangerColor()
    var fallbackMonths by remember(service.id) { mutableStateOf(1) }
    val activeMonths = if (options.size > 1) 1 else fallbackMonths
    val rawAnnual = selected.price * activeMonths
    val price = if (activeMonths >= 12) kotlin.math.round(rawAnnual * 0.75 * 100.0) / 100.0 else rawAnnual
    val priceText = formatAmount(price)
    val hasFunds = walletBalance == null || walletBalance >= price
    val walletPurchaseEnabled = enabled && inStock && hasFunds
    val manualPaymentEnabled = enabled && inStock
    var confirmOpen by remember { mutableStateOf(false) }

    NOCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ServiceBrandBadge(service.name)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(service.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                val flag = flagEmoji(service.countryCode)
                val duration = serviceDurationLabel(service.name) ?: if (activeMonths >= 12) tr("اشتراك سنوي • 365 يوم") else tr("اشتراك شهري • 30 يوم")
                val cc = selected.countryCode.trim().let { if (it.startsWith("+") || it.isBlank()) it else "+$it" }.ifBlank { "+1" }
        val formatSample = when {
            cc.startsWith("+1") -> "+1 (•••) •••-••••"
            cc.startsWith("+44") -> "+44 •••• ••••••"
            cc.startsWith("+60") -> "+60 •• •••• ••••"
            cc.startsWith("+963") -> "+963 ••• ••••••"
            cc.startsWith("+971") -> "+971 •• ••• ••••"
            cc.startsWith("+966") -> "+966 •• ••• ••••"
            else -> "$cc ••• ••• ••••"
        }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = tr("صيغة الرقم (تُخصَّص تلقائياً):"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = ltr(formatSample),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (flag != null) {
                        Text(flag, fontSize = 18.sp)
                    } else {
                        Icon(
                            Icons.Filled.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        if (service.country.trim() in listOf("1+", "+1", "1")) "🇺🇸 " + tr("الولايات المتحدة (+1)") else service.country,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(tr("رقم خاص مخصص لك • يدعم فيسبوك، واتساب، وتيليجرام"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    run {
                        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)) {
                            Text(
                                duration,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        ltr(priceText),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "USDT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Text(tr("اختر مدة الاشتراك:"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(1 to tr("اشتراك شهري"), 12 to tr("اشتراك سنوي")).forEach { (m, lbl) ->
                val rawP = selected.price * m
                val discP = if (m >= 12) kotlin.math.round(rawP * 0.75 * 100.0) / 100.0 else rawP
                val chipPrice = formatAmount(discP)
                val isSel = fallbackMonths == m
                Surface(
                    onClick = { fallbackMonths = m },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSel) 1.5.dp else 1.dp,
                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(lbl, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        Text(ltr("$chipPrice USDT"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                        if (m >= 12) {
                            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {
                                Text(tr("خصم 25% 🔥"), modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        } else {
                            Text(tr("30 يوم"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                imageVector = if (inStock) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                contentDescription = null,
                tint = stockColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = if (inStock) trf("متوفر الآن • {0}", available) else tr("نفدت الكمية حالياً"),
                style = MaterialTheme.typography.bodySmall,
                color = stockColor,
                fontWeight = FontWeight.SemiBold
            )
        }

        var promoOpen by remember(service.id) { mutableStateOf(false) }
        var promoCodeInput by remember(service.id) { mutableStateOf("") }
        var promoChecking by remember(service.id) { mutableStateOf(false) }
        var promoPreview by remember(service.id) { mutableStateOf<com.numberone.numberone.domain.model.PromoValidationResult?>(null) }
        var promoError by remember(service.id) { mutableStateOf<String?>(null) }
        val promoScope = rememberCoroutineScope()
        androidx.compose.material3.TextButton(onClick = { promoOpen = !promoOpen }, modifier = Modifier.fillMaxWidth()) { Text(if (promoOpen) tr("إخفاء كوبون الخصم") else tr("لديك كوبون خصم؟ (معاينة الخصم)"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
        if (promoOpen) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.OutlinedTextField(value = promoCodeInput, onValueChange = { promoCodeInput = it.uppercase(); promoPreview = null; promoError = null }, label = { Text(tr("كود الكوبون")) }, singleLine = true, modifier = Modifier.weight(1f))
                    Button(onClick = { promoChecking = true; promoError = null; promoPreview = null; promoScope.launch { com.numberone.numberone.di.AppContainer.engagementRepository.validatePromoCode(promoCodeInput, price).onSuccess { promoPreview = it }.onFailure { promoError = com.numberone.numberone.core.error.ErrorMapper.toUserMessage(it) }; promoChecking = false } }, enabled = promoCodeInput.trim().length >= 2 && !promoChecking, shape = RoundedCornerShape(12.dp)) { Text(if (promoChecking) "..." else tr("تحقق"), fontWeight = FontWeight.Bold) }
                }
                promoPreview?.let { res -> Text(trf("الكوبون صالح ({0}) • الخصم: {1} {3} • المبلغ بعد الخصم: {2} {3}", res.code, formatAmount(res.discountAmount), formatAmount(res.finalAmount), res.currency), style = MaterialTheme.typography.bodySmall, color = successColor(), fontWeight = FontWeight.Bold) }
                promoError?.let { err -> Text(err, style = MaterialTheme.typography.bodySmall, color = dangerColor()) }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (walletBalance != null && inStock && !hasFunds && onTopUp != null) {
                Button(onClick = onTopUp, enabled = enabled, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(trf("اشحن رصيدك للشراء ({0} USDT)", ltr(priceText)), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Button(onClick = { confirmOpen = true }, enabled = walletPurchaseEnabled, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(trf("اشترِ الآن من رصيدك • {0} USDT", ltr(priceText)), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
            }
            OutlinedButton(onClick = { onManualPayment(selected.id, activeMonths) }, enabled = manualPaymentEnabled, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Filled.CurrencyBitcoin, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(tr("أو اطلب بتحويل USDT مباشر"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (confirmOpen) {
        AlertDialog(
            onDismissRequest = { confirmOpen = false },
            title = { Text(tr("تأكيد الشراء من الرصيد"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(service.name, fontWeight = FontWeight.SemiBold)
                    ConfirmRow(label = tr("المبلغ"), value = ltr("$priceText USDT"))
                    if (walletBalance != null) {
                        ConfirmRow(label = tr("الرصيد الحالي"), value = ltr("${formatAmount(walletBalance)} USDT"))
                        ConfirmRow(
                            label = tr("الرصيد بعد الشراء"),
                            value = ltr("${formatAmount(walletBalance - price)} USDT")
                        )
                    }
                    Text(
                        tr("سيُخصم المبلغ من رصيدك فوراً وينشأ الطلب."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmOpen = false
                    onWalletPurchase(selected.id, activeMonths)
                }) {
                    Text(tr("تأكيد الخصم"), color = successColor(), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmOpen = false }) { Text(tr("إلغاء")) } }
        )
    }
}

@Composable
private fun ServiceBrandBadge(name: String) {
    val n = name.lowercase()
    val gold = androidx.compose.ui.graphics.Color(0xFFD4AF37)
    val isMultiOrVip = "/" in n || "vip" in n || "multi" in n || "all" in n
    val tag = when {
        isMultiOrVip -> "VIP"
        "whatsapp" in n -> "WA"
        "telegram" in n -> "TG"
        "facebook" in n -> "FB"
        "google" in n -> "G"
        "tiktok" in n -> "TT"
        else -> "VIP"
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = androidx.compose.ui.graphics.Color(0xFF171510),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, gold.copy(alpha = if (isMultiOrVip) 0.85f else 0.45f)),
        modifier = Modifier.size(52.dp)
    ) {
        androidx.compose.foundation.layout.Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(gold.copy(alpha = 0.24f), androidx.compose.ui.graphics.Color.Transparent)
                )
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("👑", fontSize = 14.sp)
                Text(tag, color = gold, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ConfirmRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
