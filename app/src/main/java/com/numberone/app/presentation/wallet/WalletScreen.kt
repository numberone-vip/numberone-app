package com.numberone.app.presentation.wallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.numberone.app.data.repository.WalletTransactionDto
import com.numberone.app.core.localization.AppLanguage

@Composable
fun WalletScreen(
    balance: Double?,
    language: AppLanguage,
    transactions: List<WalletTransactionDto>,
    loading: Boolean,
    onTopUpClick: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ar = language == AppLanguage.ARABIC
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(if (ar) "محفظتي" else "My wallet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("AVAILABLE BALANCE", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .75f), style = MaterialTheme.typography.labelSmall)
                Text(balance?.let { String.format("%.4f USDT", it) } ?: "—", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
                Button(onClick = onTopUpClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text(if (ar) "شحن الرصيد" else "Top up", fontWeight = FontWeight.Bold)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (ar) "آخر الحركات" else "Recent activity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onRefresh, enabled = !loading) { Text(if (loading) (if (ar) "تحديث…" else "Refreshing…") else (if (ar) "تحديث" else "Refresh")) }
        }
        if (transactions.isEmpty()) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (ar) "لا توجد حركات مالية بعد" else "No wallet activity yet", fontWeight = FontWeight.Bold)
                    Text(if (ar) "ستظهر هنا عمليات الشحن والشراء وتفاصيل الرصيد بعد تنفيذها." else "Deposits, purchases and balance changes will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            transactions.forEach { item ->
                val credit = item.direction.equals("credit", true)
                val transactionLabel = when (item.transactionType.lowercase()) {
                    "purchase" -> if (ar) "شراء رقم" else "Number purchase"
                    "deposit" -> if (ar) "شحن محفظة" else "Wallet deposit"
                    else -> item.transactionType.replaceFirstChar { it.uppercase() }
                }
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(transactionLabel, fontWeight = FontWeight.Bold)
                            Text((if (credit) "+" else "−") + String.format("%.4f %s", item.amount, item.currency), color = if (credit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                        Text(item.description ?: item.transactionType, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.createdAt.replace("T", " ").take(16), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                            Text((if (ar) "الرصيد بعد العملية: " else "Balance after: ") + String.format("%.4f", item.balanceAfter), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}
