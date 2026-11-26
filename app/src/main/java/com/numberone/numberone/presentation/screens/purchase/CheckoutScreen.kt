package com.numberone.numberone.presentation.screens.purchase

import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.core.localization.trf
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
    onWalletPurchase: (String) -> Unit,
    onManualPayment: (String) -> Unit,
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
    val price = selected.price
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
            IconBadge(Icons.Filled.PhoneAndroid, size = 48.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(service.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                val flag = flagEmoji(service.countryCode)
                val duration = serviceDurationLabel(service.name)
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
                        service.country,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (duration != null) {
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

        if (options.size > 1) {
            Text(tr("اختر السعر المناسب"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    val optionLabel = "${formatAmount(option.price)} USDT"
                    FilterChip(
                        selected = option.id == selected.id,
                        onClick = { selectedId = option.id },
                        label = { Text(optionLabel) },
                        enabled = true
                    )
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

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { confirmOpen = true },
                enabled = walletPurchaseEnabled,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(tr("اشترِ الآن من رصيدك"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            }
            if (walletBalance != null && inStock && !hasFunds) {
                    Text(
                        text = trf(
                            "رصيدك {0} USDT أقل من السعر — اشحن محفظتك أو ادفع بتحويل USDT.",
                            ltr(formatAmount(walletBalance))
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = warningColor()
                    )
                    if (onTopUp != null) {
                        Button(
                            onClick = onTopUp,
                            enabled = enabled,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text(tr("اشحن المحفظة"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = { onManualPayment(selected.id) },
                enabled = manualPaymentEnabled,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.CurrencyBitcoin, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(tr("الدفع بتحويل USDT (BEP20)"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
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
                    onWalletPurchase(selected.id)
                }) {
                    Text(tr("تأكيد الخصم"), color = successColor(), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmOpen = false }) { Text(tr("إلغاء")) } }
        )
    }
}

@Composable
private fun ConfirmRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
