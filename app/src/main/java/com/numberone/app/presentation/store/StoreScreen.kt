package com.numberone.app.presentation.store

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.numberone.app.core.localization.AppLanguage
import com.numberone.app.core.localization.Strings
import com.numberone.app.core.theme.StatusApproved
import com.numberone.app.core.theme.TealAccent
import com.numberone.app.domain.model.VirtualService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(
    strings: Strings,
    language: AppLanguage,
    walletBalance: Double?,
    services: List<VirtualService>,
    onSelectService: (VirtualService) -> Unit,
    onTopUpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedApp by remember { mutableStateOf<String?>(null) }

    val apps = listOf("WhatsApp", "Telegram", "OpenAI", "Google", "TikTok")
    val filteredServices = services.filter { service ->
        val matchesApp = selectedApp == null || service.appName.equals(selectedApp, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() ||
            service.appName.contains(searchQuery, ignoreCase = true) ||
            service.countryNameAr.contains(searchQuery, ignoreCase = true) ||
            service.countryNameEn.contains(searchQuery, ignoreCase = true)
        matchesApp && matchesSearch
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(StatusApproved.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(StatusApproved)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.adminOnlineStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    color = StatusApproved,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = strings.walletBalance,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                        Text(
                            text = walletBalance?.let { "${String.format("%.2f", it)} USDT" } ?: "—",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = onTopUpClick,
                        colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = strings.topUp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(strings.searchHint) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedApp == null,
                        onClick = { selectedApp = null },
                        label = { Text(strings.filterAll) }
                    )
                }
                items(apps) { app ->
                    FilterChip(
                        selected = selectedApp == app,
                        onClick = { selectedApp = if (selectedApp == app) null else app },
                        label = { Text(app) }
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(TealAccent.copy(alpha = 0.1f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = TealAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.guaranteeBadge,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        if (filteredServices.isEmpty()) {
            item {
                Text(
                    text = if (services.isEmpty()) "لا توجد خدمات متاحة حالياً أو تعذر تحميلها." else "لا توجد نتائج مطابقة للبحث.",
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(filteredServices, key = { it.id }) { service ->
            val countryName = if (language == AppLanguage.ARABIC) service.countryNameAr else service.countryNameEn
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectService(service) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = service.countryFlag, style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "${service.appName} • $countryName",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${service.successRate}% Success",
                                style = MaterialTheme.typography.bodyMedium,
                                color = StatusApproved
                            )
                        }
                    }
                    Text(
                        text = "$${String.format("%.2f", service.priceUsd)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = TealAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
