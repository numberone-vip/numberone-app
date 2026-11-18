package com.numberone.app.presentation.subscriptions

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.numberone.app.core.localization.AppLanguage
import com.numberone.app.core.util.ClipboardHelper
import com.numberone.app.data.repository.CustomerSubscription

private val NumberGold = Color(0xFFE7BD72)
private val NumberMint = Color(0xFF49D6B0)

@Composable
fun MyNumbersScreen(items: List<CustomerSubscription>, language: AppLanguage, modifier: Modifier = Modifier) {
    val ar = language == AppLanguage.ARABIC
    val context = LocalContext.current
    LazyColumn(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(if (ar) "أرقامي" else "My numbers", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text(if (ar) "إدارة أرقامك ورسائل التفعيل في مكان واحد" else "Your virtual numbers and activation messages", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile(if (ar) "إجمالي الأرقام" else "TOTAL NUMBERS", items.size.toString(), Modifier.weight(1f))
                MetricTile(if (ar) "نشطة" else "ACTIVE", items.count { it.status.equals("active", true) }.toString(), Modifier.weight(1f), NumberMint)
            }
        }
        if (items.isEmpty()) {
            item {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Icon(Icons.Default.SimCard, null, tint = NumberGold, modifier = Modifier.size(42.dp))
                        Text(if (ar) "لا توجد أرقام مفعّلة بعد" else "No active numbers yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(if (ar) "بعد الموافقة على طلبك وتخصيص الرقم سيظهر هنا." else "Your number will appear here after your order is approved and fulfilled.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            items(items) { item ->
                val active = item.status.equals("active", true)
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(13.dp), color = NumberGold.copy(alpha = .14f)) {
                                Icon(Icons.Default.SimCard, null, Modifier.padding(11.dp), tint = NumberGold)
                            }
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.serviceName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(if (ar) "رقم افتراضي" else "Virtual number", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                            }
                            Surface(shape = RoundedCornerShape(50), color = (if (active) NumberMint else Color(0xFFE6A36C)).copy(alpha = .13f)) {
                                Text(if (active) (if (ar) "نشط" else "ACTIVE") else item.status.uppercase(), Modifier.padding(horizontal = 11.dp, vertical = 6.dp), color = if (active) NumberMint else Color(0xFFE6A36C), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF182A46), Color(0xFF101827))), RoundedCornerShape(18.dp)).padding(16.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text(if (ar) "رقم الهاتف" else "PHONE NUMBER", color = Color.White.copy(alpha = .65f), style = MaterialTheme.typography.labelSmall)
                                Text(item.phoneNumber, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (ar) "تاريخ الانتهاء" else "EXPIRES", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                            Text(item.expiryDate ?: if (ar) "غير محدد" else "Not set", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                        }
                        if (!item.latestOtp.isNullOrBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(if (ar) "آخر رسالة / كود تفعيل" else "LATEST SMS / OTP", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                                    Text(item.latestOtp, color = NumberMint, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                                }
                                OutlinedButton(onClick = { copy(context, item.latestOtp) }, shape = RoundedCornerShape(13.dp)) {
                                    Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text(if (ar) "نسخ" else "Copy")
                                }
                            }
                        }
                        if (!item.adminNote.isNullOrBlank()) Text(item.adminNote, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier = Modifier, accent: Color = NumberGold) {
    Surface(modifier, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            Text(value, color = accent, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        }
    }
}

private fun copy(context: Context, value: String) {
    ClipboardHelper.copyToClipboard(context, "Activation code", value, "تم نسخ الكود")
}
