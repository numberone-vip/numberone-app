package com.numberone.app.presentation.navigation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.numberone.app.core.localization.AppLanguage
import com.numberone.app.core.localization.AppStrings
import com.numberone.app.core.theme.NumberOneTheme
import com.numberone.app.data.auth.AuthRepository
import com.numberone.app.data.repository.OrdersRepository
import com.numberone.app.data.repository.ServicesRepository
import com.numberone.app.data.repository.SubscriptionsRepository
import com.numberone.app.data.repository.WalletRepository
import com.numberone.app.domain.model.PurchaseRequest
import com.numberone.app.domain.model.VirtualService
import com.numberone.app.presentation.auth.CustomerAuthScreen
import com.numberone.app.presentation.checkout.CheckoutScreen
import com.numberone.app.presentation.navigation.components.FloatingCustomerNavigationBar
import com.numberone.app.presentation.orders.OrdersScreen
import com.numberone.app.presentation.store.StoreScreen
import com.numberone.app.presentation.subscriptions.MyNumbersScreen
import com.numberone.app.presentation.support.SupportScreen
import com.numberone.app.presentation.wallet.WalletScreen
import com.numberone.app.presentation.wallet.WalletTopUpDialog
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerNavigation() {
    var language by remember { mutableStateOf(AppLanguage.ARABIC) }
    val strings = AppStrings.get(language)
    val direction = if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { AuthRepository() }
    val servicesRepo = remember { ServicesRepository() }
    val walletRepo = remember { WalletRepository() }
    val ordersRepo = remember { OrdersRepository() }
    val subscriptionsRepo = remember { SubscriptionsRepository() }

    var userId by remember { mutableStateOf(auth.currentUserId()) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var signUp by remember { mutableStateOf(false) }
    var authBusy by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var authMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) }
    var showTopUp by remember { mutableStateOf(false) }
    var checkoutService by remember { mutableStateOf<VirtualService?>(null) }
    var requestId by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var purchaseBusy by remember { mutableStateOf(false) }
    var purchaseError by remember { mutableStateOf<String?>(null) }
    var liveServices by remember { mutableStateOf(emptyList<VirtualService>()) }
    var walletBalance by remember { mutableStateOf<Double?>(null) }
    var walletTransactions by remember { mutableStateOf(emptyList<com.numberone.app.data.repository.WalletTransactionDto>()) }
    var orders by remember { mutableStateOf(emptyList<PurchaseRequest>()) }
    var subscriptions by remember { mutableStateOf(emptyList<com.numberone.app.data.repository.CustomerSubscription>()) }
    var loading by remember { mutableStateOf(false) }
    var dataError by remember { mutableStateOf<String?>(null) }

    suspend fun refreshCustomerData() {
        if (userId == null) return
        loading = true
        dataError = null
        servicesRepo.getAvailableServices()
            .onSuccess { liveServices = it }
            .onFailure { dataError = it.localizedMessage ?: if (language == AppLanguage.ARABIC) "تعذر تحميل الخدمات." else "Unable to load services." }
        walletRepo.getMyWalletSummary()
            .onSuccess { walletBalance = it.balance }
            .onFailure { error -> dataError = listOfNotNull(dataError, error.localizedMessage).joinToString("\n") }
        walletRepo.getMyTransactions()
            .onSuccess { walletTransactions = it }
            .onFailure { error -> dataError = listOfNotNull(dataError, error.localizedMessage).joinToString("\n") }
        ordersRepo.getMyOrders()
            .onSuccess { orders = it }
            .onFailure { error -> dataError = listOfNotNull(dataError, error.localizedMessage).joinToString("\n") }
        subscriptionsRepo.getMySubscriptions()
            .onSuccess { subscriptions = it }
            .onFailure { error -> dataError = listOfNotNull(dataError, error.localizedMessage).joinToString("\n") }
        loading = false
    }

    LaunchedEffect(userId) {
        if (userId != null) refreshCustomerData()
        else {
            liveServices = emptyList()
            walletBalance = null
            walletTransactions = emptyList()
            orders = emptyList()
            subscriptions = emptyList()
        }
    }

    NumberOneTheme {
        CompositionLocalProvider(LocalLayoutDirection provides direction) {
            if (userId == null) {
                CustomerAuthScreen(
                    email = email,
                    password = password,
                    isSignUp = signUp,
                    loading = authBusy,
                    error = authError,
                    message = authMessage,
                    onEmailChange = { email = it; authError = null },
                    onPasswordChange = { password = it; authError = null },
                    onToggleMode = { signUp = !signUp; authError = null; authMessage = null },
                    onSubmit = {
                        scope.launch {
                            authBusy = true
                            authError = null
                            authMessage = null
                            if (signUp) {
                                auth.signUpForCustomer(email, password)
                                    .onSuccess { hasSession ->
                                        if (hasSession) userId = auth.currentUserId()
                                        else {
                                            authMessage = if (language == AppLanguage.ARABIC) "تم إنشاء الحساب. أكّد بريدك الإلكتروني ثم سجّل الدخول." else "Account created. Verify your email, then sign in."
                                            signUp = false
                                        }
                                        password = ""
                                    }
                                    .onFailure { authError = it.localizedMessage ?: "تعذر إنشاء الحساب." }
                            } else {
                                auth.signInForCustomer(email, password)
                                    .onSuccess { userId = auth.currentUserId(); password = "" }
                                    .onFailure { authError = it.localizedMessage ?: "تعذر تسجيل الدخول." }
                            }
                            authBusy = false
                        }
                    }
                )
            } else {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(strings.brandName, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                                    Text(strings.tagline, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                                }
                            },
                            actions = {
                                TextButton(onClick = {
                                    scope.launch {
                                        auth.signOut()
                                        userId = null
                                        selectedTab = 0
                                    }
                                }) {
                                    Icon(Icons.Default.Logout, contentDescription = null)
                                    Text(if (language == AppLanguage.ARABIC) "خروج" else "Sign out")
                                }
                            }
                        )
                    },
                    bottomBar = {
                        if (checkoutService == null) {
                            FloatingCustomerNavigationBar(
                                selectedTab = selectedTab,
                                language = language,
                                onSelectTab = { tab ->
                                    selectedTab = tab
                                    when (tab) {
                                        1 -> scope.launch { walletRepo.getMyTransactions().onSuccess { walletTransactions = it } }
                                        2 -> scope.launch { subscriptionsRepo.getMySubscriptions().onSuccess { subscriptions = it } }
                                        3 -> scope.launch { ordersRepo.getMyOrders().onSuccess { orders = it } }
                                    }
                                }
                            )
                        }
                    }
                ) { padding ->
                    val contentModifier = Modifier.padding(padding)
                    val selectedService = checkoutService
                    if (selectedService != null) {
                        CheckoutScreen(
                            service = selectedService,
                            strings = strings,
                            language = language,
                            loading = purchaseBusy,
                            error = purchaseError,
                            onConfirmWalletPurchase = {
                                scope.launch {
                                    purchaseBusy = true
                                    purchaseError = null
                                    walletRepo.purchaseFromWallet(selectedService.id, requestId)
                                        .onSuccess { result ->
                                            walletBalance = result.balanceAfter
                                            checkoutService = null
                                            selectedTab = 3
                                            requestId = UUID.randomUUID().toString()
                                            refreshCustomerData()
                                            Toast.makeText(context, if (language == AppLanguage.ARABIC) "تم إرسال طلبك للمراجعة" else "Your order was submitted for review", Toast.LENGTH_LONG).show()
                                        }
                                        .onFailure { purchaseError = it.localizedMessage ?: "تعذر تنفيذ الشراء." }
                                    purchaseBusy = false
                                }
                            },
                            onCancel = {
                                checkoutService = null
                                purchaseError = null
                                requestId = UUID.randomUUID().toString()
                            },
                            modifier = contentModifier
                        )
                    } else {
                        when (selectedTab) {
                            0 -> StoreScreen(
                                strings = strings,
                                language = language,
                                walletBalance = walletBalance,
                                services = liveServices,
                                isLoading = loading,
                                dataError = dataError,
                                onSelectService = { service ->
                                    purchaseError = null
                                    requestId = UUID.randomUUID().toString()
                                    checkoutService = service
                                },
                                onTopUpClick = { showTopUp = true },
                                onRetry = { scope.launch { refreshCustomerData() } },
                                modifier = contentModifier
                            )
                            1 -> WalletScreen(
                                balance = walletBalance,
                                language = language,
                                transactions = walletTransactions,
                                loading = loading,
                                onTopUpClick = { showTopUp = true },
                                onRefresh = {
                                    scope.launch {
                                        walletRepo.getMyWalletSummary().onSuccess { walletBalance = it.balance }
                                        walletRepo.getMyTransactions().onSuccess { walletTransactions = it }
                                    }
                                },
                                modifier = contentModifier
                            )
                            2 -> MyNumbersScreen(items = subscriptions, language = language, modifier = contentModifier)
                            3 -> OrdersScreen(orders = orders, strings = strings, modifier = contentModifier)
                            else -> SupportScreen(
                                strings = strings,
                                language = language,
                                onToggleLanguage = {
                                    language = if (language == AppLanguage.ARABIC) AppLanguage.ENGLISH else AppLanguage.ARABIC
                                },
                                onContactClick = {
                                    val supportIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/FastNumbersSy_bot"))
                                    try {
                                        context.startActivity(supportIntent)
                                    } catch (_: ActivityNotFoundException) {
                                        Toast.makeText(context, "https://t.me/FastNumbersSy_bot", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = contentModifier
                            )
                        }
                    }
                }
                if (showTopUp) {
                    WalletTopUpDialog(
                        repository = walletRepo,
                        onDismiss = { showTopUp = false },
                        onCredited = {
                            scope.launch {
                                walletRepo.getMyWalletSummary().onSuccess { walletBalance = it.balance }
                                walletRepo.getMyTransactions().onSuccess { walletTransactions = it }
                            }
                        }
                    )
                }
            }
        }
    }
}
