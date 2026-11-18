package com.numberone.app.presentation.wallet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.numberone.app.core.localization.AppLanguage
import com.numberone.app.data.repository.WalletTransactionDto
import java.util.Locale

private val Gold = Color(0xFFE7BD72)
private val Mint = Color(0xFF49D6B0)

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
    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(if (ar) "المحفظة" else "Your wallet", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text(if (ar) "رصيدك وحركتك المالية في مكان واحد" else "Your balance and activity, all in one place", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, Gold.copy(alpha = .55f))
            ) {
                Box(Modifier.background(Brush.linearGradient(listOf(Color(0xFF172A48), Color(0xFF101827), Color(0xFF24375A)))).padding(22.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            Icon(Icons.Default.AccountBalanceWallet, null, tint = Gold)
                            Text(if (ar) "الرصيد المتاح" else "AVAILABLE BALANCE", color = Color.White.copy(alpha = .72f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Surface(color = Gold.copy(alpha = .14f), shape = RoundedCornerShape(50)) {
                                Text("USDT", Modifier.padding(horizontal = 11.dp, vertical = 6.dp), color = Gold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(balance?.let { String.format(Locale.US, "%.4f", it) } ?: "— — —", color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
                        Text(if (ar) "رصيد آمن • تحديث من الخادم" else "Secure balance • Server synced", color = Color.White.copy(alpha = .62f), style = MaterialTheme.typography.bodySmall)
                        Button(onClick = onTopUpClick, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF172033))) {
                            Text(if (ar) "＋  شحن المحفظة" else "＋  Add funds", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (ar) "النشاط المالي" else "Activity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(if (ar) "آخر عمليات المحفظة" else "Recent wallet transactions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = onRefresh, enabled = !loading, shape = RoundedCornerShape(14.dp)) {
                    if (loading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.Refresh, null, Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (ar) "تحديث" else "Refresh")
                }
            }
        }
        if (transactions.isEmpty()) {
            item {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.fillMaxWidth().padding(25.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AccountBalanceWallet, null, tint = Gold, modifier = Modifier.size(34.dp))
                        Text(if (ar) "لا توجد حركات بعد" else "No activity yet", fontWeight = FontWeight.Bold)
                        Text(if (ar) "ستظهر عمليات الشحن والشراء هنا." else "Deposits and purchases will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            items(transactions) { item ->
                val credit = item.direction.equals("credit", true)
                val label = when (item.transactionType.lowercase()) {
                    "purchase" -> if (ar) "شراء رقم افتراضي" else "Virtual number purchase"
                    "deposit" -> if (ar) "إيداع في المحفظة" else "Wallet deposit"
                    else -> item.transactionType.replaceFirstChar { it.uppercase() }
                }
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                        Surface(shape = RoundedCornerShape(14.dp), color = (if (credit) Mint else Color(0xFFE6A36C)).copy(alpha = .13f)) {
                            Icon(if (credit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, null, Modifier.padding(11.dp), tint = if (credit) Mint else Color(0xFFE6A36C))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(label, fontWeight = FontWeight.Bold)
                            Text(item.description ?: item.transactionType, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            Text(item.createdAt.replace("T", " ").take(16), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text((if (credit) "+" else "−") + String.format(Locale.US, "%.4f", item.amount), color = if (credit) Mint else MaterialTheme.colorScheme.error, fontWeight = FontWeight.ExtraBold)
                            Text(item.currency, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                            Text(String.format(Locale.US, "%.4f", item.balanceAfter), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
