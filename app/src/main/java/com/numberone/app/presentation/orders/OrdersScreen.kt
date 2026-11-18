package com.numberone.app.presentation.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.numberone.app.core.components.OrderStepper
import com.numberone.app.core.components.OtpCopyCard
import com.numberone.app.core.components.StatusBadge
import com.numberone.app.core.localization.Strings
import com.numberone.app.core.theme.StatusRejected
import com.numberone.app.domain.model.OrderStatus
import com.numberone.app.domain.model.PurchaseRequest

@Composable
fun OrdersScreen(
    orders: List<PurchaseRequest>,
    strings: Strings,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(orders, key = { it.id }) { order ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${order.serviceName} • ${order.countryName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        StatusBadge(status = order.status, strings = strings)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    OrderStepper(status = order.status)
                    Spacer(modifier = Modifier.height(14.dp))

                    if (order.assignedNumber != null) {
                        OtpCopyCard(
                            label = order.serviceName,
                            value = order.assignedNumber,
                            strings = strings
                        )
                    }

                    if (order.otpCode != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OtpCopyCard(
                            label = strings.otpCodeLabel,
                            value = order.otpCode,
                            strings = strings
                        )
                    }

                    if (order.status == OrderStatus.REJECTED && !order.rejectionReason.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = order.rejectionReason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = StatusRejected
                        )
                    }
                }
            }
        }
    }
}
