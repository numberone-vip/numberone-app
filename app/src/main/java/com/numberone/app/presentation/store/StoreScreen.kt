package com.numberone.app.presentation.store

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.numberone.app.core.localization.AppLanguage
import com.numberone.app.core.localization.Strings
import com.numberone.app.domain.model.VirtualService
import java.util.Locale

private val StoreEmerald = Color(0xFF20C997)
private val GlassBorder = listOf(Color(0xFF55C7FF), Color(0xFF9B8CFF), Color(0xFF55C7FF))

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
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var search by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf<String?>(null) }
    var selectedApp by remember { mutableStateOf<String?>(null) }

    val countries = remember(services, language) {
        services.map { if (language == AppLanguage.ARABIC) it.countryNameAr else it.countryNameEn }.distinct().sorted()
    }
    val apps = remember(services) { services.map { it.appName }.distinct().sorted() }
    val filtered = remember(services, language, search, selectedCountry, selectedApp) {
        services.filter { service ->
            val countryName = if (language == AppLanguage.ARABIC) service.countryNameAr else service.countryNameEn
            val query = search.trim()
            (selectedCountry == null || countryName == selectedCountry) &&
                (selectedApp == null || service.appName == selectedApp) &&
                (query.isBlank() || service.appName.contains(query, true) ||
                    service.countryNameAr.contains(query, true) || service.countryNameEn.contains(query, true))
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        item { StoreHero(language, services.size) }
        item { WalletBalanceCard(walletBalance, language, strings.topUp, onTopUpClick) }
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(strings.searchHint) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (search.isNotBlank()) {
                        Text(
                            if (language == AppLanguage.ARABIC) "مسح" else "Clear",
                            modifier = Modifier.clickable { search = "" }.padding(horizontal = 8.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp)
            )
        }
        if (apps.isNotEmpty()) {
            item {
                FilterTitle(if (language == AppLanguage.ARABIC) "اختر التطبيق" else "Choose an app", Icons.Default.Tune)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selectedApp == null, onClick = { selectedApp = null }, label = { Text(strings.filterAll) })
                    apps.forEach { app ->
                        FilterChip(selected = selectedApp == app, onClick = { selectedApp = if (selectedApp == app) null else app }, label = { Text(app) })
                    }
                }
            }
        }
        if (countries.size > 1) {
            item {
                FilterTitle(if (language == AppLanguage.ARABIC) "الدول" else "Countries", Icons.Default.Language)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selectedCountry == null, onClick = { selectedCountry = null }, label = { Text(strings.filterAll) })
                    countries.forEach { country ->
                        FilterChip(selected = selectedCountry == country, onClick = { selectedCountry = if (selectedCountry == country) null else country }, label = { Text(country) })
                    }
                }
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.20f))
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Text(strings.guaranteeBadge, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(if (language == AppLanguage.ARABIC) "الأرقام المتاحة" else "Available numbers", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(if (language == AppLanguage.ARABIC) "اختر الخدمة المناسبة لك" else "Find the right service for you", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                    Text(filtered.size.toString(), Modifier.padding(horizontal = 13.dp, vertical = 7.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (isLoading && services.isEmpty()) {
            items(4) { ServiceSkeleton() }
        } else if (isLoading) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(if (language == AppLanguage.ARABIC) "يتم تحديث بيانات المتجر…" else "Updating store data…", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (dataError != null) {
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (language == AppLanguage.ARABIC) "تعذر تحديث المتجر" else "Store update failed", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text(dataError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        Button(onClick = onRetry, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Text(if (language == AppLanguage.ARABIC) "إعادة" else "Retry")
                        }
                    }
                }
            }
        }
        if (filtered.isEmpty() && !isLoading) {
            item {
                EmptyStoreState(
                    language = language,
                    noServices = services.isEmpty(),
                    onClear = { search = ""; selectedApp = null; selectedCountry = null }
                )
            }
        }
        items(filtered, key = { it.id }) { service ->
            ServiceCard(service, language) { onSelectService(service) }
        }
    }
}

@Composable
private fun StoreHero(language: AppLanguage, count: Int) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(
            Brush.linearGradient(listOf(Color(0xFF111D35), Color(0xFF172B50), Color(0xFF28234F)))
        )
    ) {
        Box(Modifier.align(Alignment.TopEnd).padding(16.dp).size(112.dp).clip(CircleShape).background(Color(0xFF8B7CFF).copy(alpha = 0.17f)))
        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("NUMBER ONE  /  GLOBAL CONNECTIVITY", color = Color(0xFF83E7DB), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(
                if (language == AppLanguage.ARABIC) "عالمك الرقمي،\nبرقمك الخاص." else "Your digital world.\nYour number.",
                color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold
            )
            Text(
                if (language == AppLanguage.ARABIC) "أرقام افتراضية لخدماتك المفضلة، بخطوات شراء واضحة وآمنة." else "Virtual numbers for your favorite services, with a clear and secure checkout.",
                color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodyMedium
            )
            Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.10f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Icon(Icons.Default.CheckCircle, null, tint = StoreEmerald, modifier = Modifier.size(15.dp))
                    Text(if (language == AppLanguage.ARABIC) "$count خدمة في المتجر" else "$count services in store", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun WalletBalanceCard(balance: Double?, language: AppLanguage, topUp: String, onTopUp: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Brush.linearGradient(GlassBorder.map { it.copy(alpha = 0.42f) })),
        shadowElevation = 4.dp
    ) {
        Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (language == AppLanguage.ARABIC) "رصيد محفظتك" else "Your wallet balance", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                Text(balance?.let { String.format(Locale.US, "%.2f USDT", it) } ?: "— USDT", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text(if (language == AppLanguage.ARABIC) "الرصيد الفعلي المتاح للشراء" else "Available balance for purchases", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
            Button(onClick = onTopUp, shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 15.dp, vertical = 12.dp)) {
                Text("+  $topUp", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FilterTitle(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ServiceCard(service: VirtualService, language: AppLanguage, onClick: () -> Unit) {
    val available = service.availableCount > 0
    val mark = when {
        service.appName.contains("WhatsApp", true) -> "WA"
        service.appName.contains("Telegram", true) -> "TG"
        service.appName.contains("Facebook", true) -> "f"
        service.appName.contains("Google", true) -> "G"
        service.appName.contains("Instagram", true) -> "IG"
        else -> service.appName.take(2).uppercase()
    }
    Surface(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).clickable(enabled = available, onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Brush.linearGradient(GlassBorder.map { it.copy(alpha = 0.34f) })),
        shadowElevation = 2.dp
    ) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(54.dp).clip(RoundedCornerShape(17.dp)).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)))), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(service.countryFlag, style = MaterialTheme.typography.titleMedium)
                    Text(mark, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(service.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(if (language == AppLanguage.ARABIC) service.countryNameAr else service.countryNameEn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(shape = RoundedCornerShape(50), color = if (available) StoreEmerald.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer) {
                    Text(
                        if (available) (if (language == AppLanguage.ARABIC) "متاح • ${service.availableCount}" else "Available • ${service.availableCount}") else (if (language == AppLanguage.ARABIC) "نفد المخزون" else "Out of stock"),
                        Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        color = if (available) StoreEmerald else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(String.format(Locale.US, "%.2f", service.priceUsd), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = StoreEmerald)
                Text("USDT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (language == AppLanguage.ARABIC) "للطلب ←" else "Order →", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ServiceSkeleton() {
    val transition = rememberInfiniteTransition(label = "store-shimmer")
    val alpha by transition.animateFloat(
        0.25f, 0.65f,
        infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "shimmer-alpha"
    )
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = alpha)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.fillMaxWidth(0.58f).height(13.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.25f)))
            Box(Modifier.fillMaxWidth(0.38f).height(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.18f)))
            Box(Modifier.fillMaxWidth(0.46f).height(9.dp).clip(CircleShape).background(StoreEmerald.copy(alpha = alpha * 0.25f)))
        }
        Box(Modifier.width(48.dp).height(18.dp).clip(CircleShape).background(StoreEmerald.copy(alpha = alpha * 0.28f)))
    }
}

@Composable
private fun EmptyStoreState(language: AppLanguage, noServices: Boolean, onClear: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(25.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("⌕", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                if (noServices) (if (language == AppLanguage.ARABIC) "المتجر يستعد لاستقبالك" else "The store is getting ready")
                else (if (language == AppLanguage.ARABIC) "لم نعثر على نتائج" else "No matching results"),
                fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium
            )
            Text(
                if (noServices) (if (language == AppLanguage.ARABIC) "ستظهر الخدمات هنا بمجرد إضافتها من الإدارة." else "Services will appear here when added by the team.")
                else (if (language == AppLanguage.ARABIC) "جرّب تغيير البحث أو الفلاتر." else "Try changing your search or filters."),
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall
            )
            if (!noServices) Button(onClick = onClear) { Text(if (language == AppLanguage.ARABIC) "مسح الفلاتر" else "Clear filters") }
        }
    }
}
