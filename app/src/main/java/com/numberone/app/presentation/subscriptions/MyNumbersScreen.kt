package com.numberone.app.presentation.subscriptions

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.numberone.app.core.util.ClipboardHelper
import com.numberone.app.data.repository.CustomerSubscription

@Composable
fun MyNumbersScreen(items: List<CustomerSubscription>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("أرقامي الافتراضية", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("أرقامك المفعّلة وأكواد الرسائل الواردة.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        if (items.isEmpty()) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("لا توجد أرقام مفعّلة بعد.", fontWeight = FontWeight.Bold)
                    Text("بعد الموافقة على طلبك سيظهر رقمك هنا.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items.forEach { item ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.serviceName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(item.status, color = if (item.status == "active") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(item.phoneNumber, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Text("تاريخ الانتهاء: " + (item.expiryDate ?: "غير محدد"), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        if (!item.latestOtp.isNullOrBlank()) {
                            Spacer(Modifier.height(3.dp))
                            Text("آخر كود وصل", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(item.latestOtp, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                                OutlinedButton(onClick = { copy(context, item.latestOtp) }) { Text("نسخ الكود") }
                            }
                        }
                        if (!item.adminNote.isNullOrBlank()) Text(item.adminNote, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private fun copy(context: Context, value: String) {
    ClipboardHelper.copyToClipboard(context, "Activation code", value, "تم نسخ الكود")
}
