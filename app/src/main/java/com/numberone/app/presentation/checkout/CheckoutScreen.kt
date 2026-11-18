package com.numberone.app.presentation.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.numberone.app.core.localization.AppLanguage
import com.numberone.app.core.localization.Strings
import com.numberone.app.domain.model.VirtualService
import java.util.Locale

@Composable
fun CheckoutScreen(
    service: VirtualService, strings: Strings, language: AppLanguage, loading: Boolean, error: String?,
    onConfirmWalletPurchase: () -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier
) {
    val ar = language == AppLanguage.ARABIC
    val country = if (ar) service.countryNameAr else service.countryNameEn
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(if (ar) "تأكيد الشراء" else "Review purchase", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text(if (ar) "خطوة أخيرة قبل تفعيل رقمك" else "One final step before activation", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
            Box(Modifier.background(Brush.linearGradient(listOf(Color(0xFF172A48), Color(0xFF101827), Color(0xFF26395B)))).padding(22.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Text(if (ar) "ملخص الطلب" else "ORDER SUMMARY", color = Color(0xFFE7BD72), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(service.countryFlag, style = MaterialTheme.typography.headlineLarge)
                        Column {
                            Text(service.appName, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(country, color = Color.White.copy(alpha = .7f))
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = .16f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (ar) "الأرقام المتاحة" else "Available", color = Color.White.copy(alpha = .7f))
                        Text(service.availableCount.toString(), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Text(if (ar) "الإجمالي" else "TOTAL", color = Color.White.copy(alpha = .7f))
                        Text(String.format(Locale.US, "%.2f USDT", service.priceUsd), color = Color(0xFFE7BD72), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.VerifiedUser, null, tint = Color(0xFF49D6B0))
                Text(if (ar) "الدفع يتم من رصيد محفظتك الآمن." else "Payment is processed from your secure wallet balance.", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.weight(1f))
        Button(onClick = onConfirmWalletPurchase, enabled = !loading && service.availableCount > 0, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(17.dp)) {
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(if (ar) "تأكيد الشراء من المحفظة" else "Confirm with wallet", fontWeight = FontWeight.Bold)
        }
        OutlinedButton(onClick = onCancel, enabled = !loading, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(16.dp)) {
            Text(if (ar) "رجوع إلى المتجر" else "Back to store")
        }
    }
}
