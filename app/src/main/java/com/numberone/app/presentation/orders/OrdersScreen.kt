package com.numberone.app.presentation.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.numberone.app.core.components.OrderStepper
import com.numberone.app.core.components.OtpCopyCard
import com.numberone.app.core.components.StatusBadge
import com.numberone.app.core.localization.Strings
import com.numberone.app.core.theme.StatusRejected
import com.numberone.app.domain.model.OrderStatus
import com.numberone.app.domain.model.PurchaseRequest

private val OrderGold = Color(0xFFE7BD72)

@Composable
fun OrdersScreen(orders: List<PurchaseRequest>, strings: Strings, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("طلباتي", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text("تابع حالة طلباتك وتفاصيل التفعيل", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("كل الطلبات", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        Text(orders.size.toString(), color = OrderGold, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
                    }
                    Icon(Icons.Default.ReceiptLong, null, tint = OrderGold, modifier = Modifier.size(34.dp))
                }
            }
        }
        if (orders.isEmpty()) {
            item {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.fillMaxWidth().padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.ReceiptLong, null, tint = OrderGold, modifier = Modifier.size(38.dp))
                        Text("لا توجد طلبات حتى الآن", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("عندما تطلب رقماً من المتجر سيظهر هنا رقم الطلب وحالته.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            items(orders, key = { it.id }) { order ->
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(order.serviceName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(order.countryName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            StatusBadge(status = order.status, strings = strings)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        OrderStepper(status = order.status)
                        if (order.assignedNumber != null) OtpCopyCard(label = order.serviceName, value = order.assignedNumber, strings = strings)
                        if (order.otpCode != null) OtpCopyCard(label = strings.otpCodeLabel, value = order.otpCode, strings = strings)
                        if (order.status == OrderStatus.REJECTED && !order.rejectionReason.isNullOrBlank()) {
                            Text(order.rejectionReason, style = MaterialTheme.typography.bodyMedium, color = StatusRejected)
                        }
                    }
                }
            }
        }
    }
}
