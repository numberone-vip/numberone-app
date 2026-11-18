package com.numberone.app.presentation.navigation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.numberone.app.core.localization.AppLanguage

@Composable
fun FloatingCustomerNavigationBar(
    selectedTab: Int,
    language: AppLanguage,
    onSelectTab: (Int) -> Unit
) {
    val labels = if (language == AppLanguage.ARABIC)
        listOf("المتجر", "المحفظة", "أرقامي", "طلباتي", "الدعم")
    else
        listOf("Store", "Wallet", "My numbers", "Orders", "Support")

    val icons = listOf(
        Icons.Default.Storefront,
        Icons.Default.AccountBalanceWallet,
        Icons.Default.Storefront,
        Icons.Default.ListAlt,
        Icons.Default.SupportAgent
    )

    Surface(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.48f),
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.30f),
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.50f)
                )
            )
        ),
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            labels.forEachIndexed { index, label ->
                NavigationBarItem(
                    selected = selectedTab == index,
                    onClick = { onSelectTab(index) },
                    icon = { Icon(icons[index], contentDescription = label) },
                    label = { Text(label, maxLines = 1) },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
