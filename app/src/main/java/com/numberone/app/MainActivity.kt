package com.numberone.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.numberone.app.core.localization.AppLanguage
import com.numberone.app.core.localization.AppStrings
import com.numberone.app.core.theme.NumberOneTheme
import com.numberone.app.data.auth.AuthRepository
import com.numberone.app.data.auth.StaffAccessRole
import com.numberone.app.data.repository.ServicesRepository
import com.numberone.app.data.repository.WalletRepository
import com.numberone.app.domain.model.VirtualService
import com.numberone.app.presentation.checkout.CheckoutScreen
import com.numberone.app.presentation.auth.CustomerAuthScreen
import com.numberone.app.presentation.orders.OrdersScreen
import com.numberone.app.presentation.store.StoreScreen
import com.numberone.app.presentation.support.SupportScreen
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var language by remember { mutableStateOf(AppLanguage.ARABIC) }
            val strings = AppStrings.get(language)
            val layoutDirection = if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val authRepository = remember { AuthRepository() }
            val servicesRepository = remember { ServicesRepository() }
            val walletRepository = remember { WalletRepository() }

            var selectedTab by remember { mutableStateOf(0) }
            var selectedServiceForCheckout by remember { mutableStateOf<VirtualService?>(null) }
            var adminRole by remember { mutableStateOf<StaffAccessRole?>(null) }
            var adminEmail by remember { mutableStateOf("") }
            var adminPassword by remember { mutableStateOf("") }
            var authBusy by remember { mutableStateOf(false) }
            var authError by remember { mutableStateOf<String?>(null) }
            var customerUserId by remember { mutableStateOf(authRepository.currentUserId()) }
            var customerEmail by remember { mutableStateOf("") }
            var customerPassword by remember { mutableStateOf("") }
            var customerIsSignUp by remember { mutableStateOf(false) }
            var customerAuthError by remember { mutableStateOf<String?>(null) }
            var customerAuthMessage by remember { mutableStateOf<String?>(null) }
            var liveServices by remember { mutableStateOf(emptyList<VirtualService>()) }
            var walletBalance by remember { mutableStateOf<Double?>(null) }
            var dataLoading by remember { mutableStateOf(false) }
            var dataError by remember { mutableStateOf<String?>(null) }
            var purchaseLoading by remember { mutableStateOf(false) }
            var purchaseError by remember { mutableStateOf<String?>(null) }
            var purchaseRequestId by remember { mutableStateOf(UUID.randomUUID().toString()) }

            LaunchedEffect(customerUserId) {
                if (customerUserId != null) {
                    dataLoading = true
                    dataError = null
                    val serviceResult = servicesRepository.getAvailableServices()
                    serviceResult.onSuccess { liveServices = it }
                        .onFailure { dataError = it.localizedMessage ?: "تعذر تحميل الخدمات." }
                    walletRepository.getMyWalletSummary()
                        .onSuccess { walletBalance = it.balance }
                        .onFailure { error ->
                            walletBalance = null
                            dataError = listOfNotNull(dataError, error.localizedMessage ?: "تعذر تحميل رصيد المحفظة.").joinToString("\\n")
                        }
                    dataLoading = false
                } else {
                    liveServices = emptyList()
                    walletBalance = null
                }
            }

            NumberOneTheme {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    if (customerUserId == null) {
                        CustomerAuthScreen(
                            email = customerEmail,
                            password = customerPassword,
                            isSignUp = customerIsSignUp,
                            loading = authBusy,
                            error = customerAuthError,
                            message = customerAuthMessage,
                            onEmailChange = { customerEmail = it; customerAuthError = null },
                            onPasswordChange = { customerPassword = it; customerAuthError = null },
                            onToggleMode = {
                                customerIsSignUp = !customerIsSignUp
                                customerAuthError = null
                                customerAuthMessage = null
                            },
                            onSubmit = {
                                scope.launch {
                                    authBusy = true
                                    customerAuthError = null
                                    customerAuthMessage = null
                                    if (customerIsSignUp) {
                                        authRepository.signUpForCustomer(customerEmail, customerPassword)
                                            .onSuccess { hasSession ->
                                                if (hasSession) {
                                                    customerUserId = authRepository.currentUserId()
                                                    customerPassword = ""
                                                } else {
                                                    customerAuthMessage = "تم إنشاء الحساب. افتح بريدك الإلكتروني وأكّد الحساب، ثم سجّل الدخول."
                                                    customerIsSignUp = false
                                                    customerPassword = ""
                                                }
                                            }
                                            .onFailure { e -> customerAuthError = e.localizedMessage ?: "تعذر إنشاء الحساب." }
                                    } else {
                                        authRepository.signInForCustomer(customerEmail, customerPassword)
                                            .onSuccess {
                                                customerUserId = authRepository.currentUserId()
                                                customerPassword = ""
                                            }
                                            .onFailure { e -> customerAuthError = e.localizedMessage ?: "تعذر تسجيل الدخول." }
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
                                    NavigationBarItem(
                                        selected = selectedTab == 3,
                                        onClick = {
                                            selectedTab = 3
                                            authError = null
                                        },
                                        icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
                                        label = { Text(strings.navAdmin) }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        val contentModifier = Modifier.padding(innerPadding)
                        val checkoutService = selectedServiceForCheckout
                        if (checkoutService != null) {
                            CheckoutScreen(
                                service = checkoutService,
                                strings = strings,
                                language = language,
                                loading = purchaseLoading,
                                error = purchaseError,
                                onConfirmWalletPurchase = {
                                    scope.launch {
                                        purchaseLoading = true
                                        purchaseError = null
                                        walletRepository.purchaseFromWallet(
                                            serviceId = checkoutService.id,
                                            requestId = purchaseRequestId
                                        ).onSuccess { order ->
                                            walletBalance = order.balanceAfter
                                            selectedServiceForCheckout = null
                                            selectedTab = 1
                                            purchaseRequestId = UUID.randomUUID().toString()
                                            servicesRepository.getAvailableServices().onSuccess { liveServices = it }
                                            Toast.makeText(context, "تم إنشاء الطلب بنجاح", Toast.LENGTH_LONG).show()
                                        }.onFailure { error ->
                                            purchaseError = error.localizedMessage ?: "تعذر تنفيذ الشراء."
                                        }
                                        purchaseLoading = false
                                    }
                                },
                                onCancel = {
                                    selectedServiceForCheckout = null
                                    purchaseError = null
                                    purchaseRequestId = UUID.randomUUID().toString()
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
                                    onSelectService = { service ->
                                        purchaseError = null
                                        purchaseRequestId = UUID.randomUUID().toString()
                                        selectedServiceForCheckout = service
                                    },
                                    onTopUpClick = {
                                        Toast.makeText(context, strings.topUp, Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = contentModifier
                                )
                                1 -> OrdersScreen(
                                    orders = emptyList(),
                                    strings = strings,
                                    modifier = contentModifier
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
                                    modifier = contentModifier
                                )
                                3 -> {
                                    if (adminRole == null) {
                                        AdminLoginScreen(
                                            email = adminEmail,
                                            password = adminPassword,
                                            error = authError,
                                            loading = authBusy,
                                            onEmailChange = { adminEmail = it },
                                            onPasswordChange = { adminPassword = it },
                                            onLogin = {
                                                scope.launch {
                                                    authBusy = true
                                                    authError = null
                                                    authRepository.signInForAdmin(adminEmail, adminPassword)
                                                        .onSuccess { role -> adminRole = role }
                                                        .onFailure { error ->
                                                            authError = error.localizedMessage
                                                                ?: "تعذر تسجيل الدخول. تحقق من البيانات."
                                                        }
                                                    authBusy = false
                                                }
                                            },
                                            modifier = contentModifier
                                        )
                                    } else {
                                        ProtectedAdminScreen(
                                            role = adminRole!!,
                                            onSignOut = {
                                                scope.launch {
                                                    authBusy = true
                                                    runCatching { authRepository.signOut() }
                                                    adminRole = null
                                                    customerUserId = null
                                                    adminPassword = ""
                                                    selectedTab = 0
                                                    authBusy = false
                                                }
                                            },
                                            modifier = contentModifier
                                        )
                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminLoginScreen(
    email: String,
    password: String,
    error: String?,
    loading: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.AdminPanelSettings,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(56.dp).height(56.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("دخول الإدارة", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "هذه المنطقة مخصصة لحسابات الإدارة والموظفين المصرح لهم فقط.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("البريد الإلكتروني") },
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("كلمة المرور") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onLogin,
            enabled = !loading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) CircularProgressIndicator(modifier = Modifier.width(20.dp).height(20.dp))
            else Text("تسجيل الدخول")
        }
    }
}

@Composable
private fun ProtectedAdminScreen(
    role: StaffAccessRole,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("تم التحقق من الهوية", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    if (role == StaffAccessRole.ADMIN) "الصلاحية: مدير النظام" else "الصلاحية: موظف معتمد",
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "تم السماح بالدخول بعد التحقق من حساب Supabase وصلاحياته.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Text(
            "بوابة الإدارة محمية. سيتم ربط أدوات إدارة الطلبات والاشتراكات بهذه المنطقة بعد استكمال مراجعة صلاحيات كل إجراء.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onSignOut, modifier = Modifier.align(Alignment.End)) {
            Text("تسجيل الخروج")
        }
    }
}
