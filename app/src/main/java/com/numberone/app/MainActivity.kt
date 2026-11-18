package com.numberone.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import com.numberone.app.core.localization.AppLanguage
import com.numberone.app.core.localization.AppStrings
import com.numberone.app.core.theme.NumberOneTheme
import com.numberone.app.domain.model.OrderStatus
import com.numberone.app.domain.model.PurchaseRequest
import com.numberone.app.domain.model.VirtualService
import com.numberone.app.presentation.checkout.CheckoutScreen
import com.numberone.app.presentation.orders.OrdersScreen
import com.numberone.app.presentation.store.StoreScreen
import com.numberone.app.presentation.support.SupportScreen

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var language by remember { mutableStateOf(AppLanguage.ARABIC) }
            val strings = AppStrings.get(language)
            val layoutDirection = if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            val context = LocalContext.current

            var selectedTab by remember { mutableStateOf(0) }
            var selectedServiceForCheckout by remember { mutableStateOf<VirtualService?>(null) }

            val sampleServices = remember {
                listOf(
                    VirtualService("1", "WhatsApp", "الولايات المتحدة", "United States", "🇺🇸", 1.50, 98, true),
                    VirtualService("2", "Telegram", "بريطانيا", "United Kingdom", "🇬🇧", 1.20, 96, true),
                    VirtualService("3", "OpenAI", "كندا", "Canada", "🇨🇦", 2.00, 99, false),
                    VirtualService("4", "Google", "ألمانيا", "Germany", "🇩🇪", 1.80, 95, false)
                )
            }

            val sampleOrders = remember {
                listOf(
                    PurchaseRequest(
                        id = "ord_1",
                        userId = "usr_1",
                        serviceName = "WhatsApp",
                        countryName = "🇺🇸 USA",
                        amountUsd = 1.50,
                        status = OrderStatus.SMS_READY,
                        assignedNumber = "+1 555 019 2834",
                        otpCode = "482-910",
                        createdAt = "2026-11-14"
                    ),
                    PurchaseRequest(
                        id = "ord_2",
                        userId = "usr_1",
                        serviceName = "Telegram",
                        countryName = "🇬🇧 UK",
                        amountUsd = 1.20,
                        status = OrderStatus.PENDING,
                        createdAt = "2026-11-14"
                    )
                )
            }

            NumberOneTheme {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Column {
                                        Text(
                                            text = strings.brandName,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = strings.tagline,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            )
                        },
                        bottomBar = {
                            if (selectedServiceForCheckout == null) {
                                NavigationBar {
                                    NavigationBarItem(
                                        selected = selectedTab == 0,
                                        onClick = { selectedTab = 0 },
                                        icon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                                        label = { Text(strings.navStore) }
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == 1,
                                        onClick = { selectedTab = 1 },
                                        icon = { Icon(Icons.Default.ListAlt, contentDescription = null) },
                                        label = { Text(strings.navOrders) }
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == 2,
                                        onClick = { selectedTab = 2 },
                                        icon = { Icon(Icons.Default.SupportAgent, contentDescription = null) },
                                        label = { Text(strings.navSupport) }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        val checkoutService = selectedServiceForCheckout
                        if (checkoutService != null) {
                            CheckoutScreen(
                                service = checkoutService,
                                strings = strings,
                                language = language,
                                onSubmitOrder = { _, _ ->
                                    Toast.makeText(context, strings.statusPending, Toast.LENGTH_SHORT).show()
                                    selectedServiceForCheckout = null
                                    selectedTab = 1
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        } else {
                            when (selectedTab) {
                                0 -> StoreScreen(
                                    strings = strings,
                                    language = language,
                                    walletBalance = 15.00,
                                    services = sampleServices,
                                    onSelectService = { selectedServiceForCheckout = it },
                                    onTopUpClick = {
                                        Toast.makeText(context, strings.topUp, Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.padding(innerPadding)
                                )
                                1 -> OrdersScreen(
                                    orders = sampleOrders,
                                    strings = strings,
                                    modifier = Modifier.padding(innerPadding)
                                )
                                2 -> SupportScreen(
                                    strings = strings,
                                    language = language,
                                    onToggleLanguage = {
                                        language = if (language == AppLanguage.ARABIC) {
                                            AppLanguage.ENGLISH
                                        } else {
                                            AppLanguage.ARABIC
                                        }
                                    },
                                    onContactClick = {
                                        Toast.makeText(context, strings.contactSupport, Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
