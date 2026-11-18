package com.numberone.app.presentation.store

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.numberone.app.core.localization.AppLanguage
import com.numberone.app.core.localization.Strings
import com.numberone.app.domain.model.VirtualService

@Composable
fun StoreScreen(
    strings: Strings,
    language: AppLanguage,
    walletBalance: Double?,
    services: List<VirtualService>,
    isLoading: Boolean = false,
    dataError: String? = null,
    onSelectService: (VirtualService) -> Unit,
    onTopUpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var search by remember { mutableStateOf("") }
    var country by remember { mutableStateOf<String?>(null) }
    val countries = services.map { if (language == AppLanguage.ARABIC) it.countryNameAr else it.countryNameEn }.distinct().sorted()
    val filtered = services.filter { service ->
        val name = if (language == AppLanguage.ARABIC) service.countryNameAr else service.countryNameEn
        (country == null || name == country) &&
            (search.isBlank() || service.appName.contains(search, true) || name.contains(search, true))
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("NUMBER ONE  /  GLOBAL CONNECTIVITY", color = Color(0xFF8FE6DC), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(
                        if (language == AppLanguage.ARABIC) "أرقامك. تحكّمك." else "Your numbers.\nYour control.",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        if (language == AppLanguage.ARABIC) "اختر الخدمة والدولة واطلب رقمك الافتراضي بخطوات واضحة." else "Choose a service and country, then order your virtual number in a few clear steps.",
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(strings.walletBalance, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                        Text(walletBalance?.let { String.format("%.2f USDT", it) } ?: "—", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = onTopUpClick, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                        Text(strings.topUp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text(strings.searchHint) }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }, singleLine = true, shape = RoundedCornerShape(14.dp))
        }
        if (countries.size > 1) {
            item {
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = country == null, onClick = { country = null }, label = { Text(strings.filterAll) }) }
                    items(countries) { item -> FilterChip(selected = country == item, onClick = { country = if (country == item) null else item }, label = { Text(item) }) }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(9.dp))
                Text(strings.guaranteeBadge, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (language == AppLanguage.ARABIC) "الخدمات المتاحة" else "Available services", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(filtered.size.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            }
        }
        if (isLoading) item { Text(if (language == AppLanguage.ARABIC) "جارٍ تحديث المتجر…" else "Refreshing store…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (dataError != null) item { Text(dataError, color = MaterialTheme.colorScheme.error) }
        if (filtered.isEmpty() && !isLoading) {
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("⌕", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (services.isEmpty()) (if (language == AppLanguage.ARABIC) "لا توجد خدمات مضافة حالياً." else "No services have been added yet.") else (if (language == AppLanguage.ARABIC) "لا توجد نتائج مطابقة." else "No matching services."), fontWeight = FontWeight.SemiBold)
                        Text(if (language == AppLanguage.ARABIC) "سيظهر هنا كل ما تضيفه الإدارة إلى المتجر." else "Services added by the team will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        items(filtered, key = { it.id }) { service ->
            val countryName = if (language == AppLanguage.ARABIC) service.countryNameAr else service.countryNameEn
            val available = service.availableCount > 0
            Card(
                modifier = Modifier.fillMaxWidth().clickable(enabled = available) { onSelectService(service) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                        Text(service.countryFlag, style = MaterialTheme.typography.headlineSmall)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(service.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(countryName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (available) (if (language == AppLanguage.ARABIC) "متوفر: " + service.availableCount else "Available: " + service.availableCount) else (if (language == AppLanguage.ARABIC) "غير متوفر حالياً" else "Out of stock"),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (available) Color(0xFF13865F) else MaterialTheme.colorScheme.error
                        )
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(String.format("%.2f USDT", service.priceUsd), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(if (language == AppLanguage.ARABIC) "للطلب" else "Order", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
