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
import androidx.compose.runtime.collectAsState
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
import com.numberone.app.domain.model.VirtualService
import com.numberone.app.presentation.StoreViewModel
import com.numberone.app.presentation.OrdersViewModel
import com.numberone.app.presentation.OrdersUiState
import com.numberone.app.presentation.auth.CustomerAuthScreen
import com.numberone.app.presentation.StoreUiState
import com.numberone.app.presentation.checkout.CheckoutScreen
import com.numberone.app.presentation.orders.OrdersScreen
import com.numberone.app.presentation.store.StoreScreen
import com.numberone.app.presentation.support.SupportScreen
import kotlinx.coroutines.launch

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
            val storeViewModel = remember { StoreViewModel() }
            val storeUiState by storeViewModel.uiState.collectAsState()
            val ordersViewModel = remember { OrdersViewModel() }
            val ordersUiState by ordersViewModel.uiState.collectAsState()

            var selectedTab by remember { mutableStateOf(0) }
            var customerUserId by remember { mutableStateOf(authRepository.currentCustomerUserId()) }
            var selectedServiceForCheckout by remember { mutableStateOf<VirtualService?>(null) }
            var adminRole by remember { mutableStateOf<StaffAccessRole?>(null) }
            var adminEmail by remember { mutableStateOf("") }
            var adminPassword by remember { mutableStateOf("") }
            var authBusy by remember { mutableStateOf(false) }
            var authError by remember { mutableStateOf<String?>(null) }

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
                                },
                                actions = {
                                    TextButton(
                                        onClick = {
                                            language = if (language == AppLanguage.ARABIC) {
                                                AppLanguage.ENGLISH
                                            } else {
                                                AppLanguage.ARABIC
                                            }
                                        }
                                    ) {
                                        Text(strings.switchLanguage)
                                    }
                                    if (customerUserId != null && selectedTab != 3) {
                                        TextButton(
                                            onClick = {
                                                scope.launch {
                                                    authBusy = true
                                                    runCatching { authRepository.signOutCustomer() }
                                                    customerUserId = null
                                                    selectedServiceForCheckout = null
                                                    selectedTab = 0
                                                    authBusy = false
                                                }
                                            }
                                        ) {
                                            Text(if (language == AppLanguage.ARABIC) "خروج" else "Sign out")
                                        }
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
                                        onClick = { selectedTab = 1; if (customerUserId != null) ordersViewModel.loadOrders() },
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
                        if (selectedTab != 3 && customerUserId == null) {
                            CustomerAuthScreen(
                                authRepository = authRepository,
                                onAuthenticated = { userId ->
                                    customerUserId = userId
                                    selectedTab = 0
                                    storeViewModel.loadServices()
                                    ordersViewModel.loadOrders()
                                },
                                language = language,
                                modifier = contentModifier
                            )
                        } else if (checkoutService != null) {
                            CheckoutScreen(
                                service = checkoutService,
                                strings = strings,
                                language = language,
                                onCancel = { selectedServiceForCheckout = null },
                                modifier = contentModifier
                            )
                        } else {
                            when (selectedTab) {
                                0 -> when (val state = storeUiState) {
                                    StoreUiState.Loading -> Column(
                                        modifier = contentModifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator()
                                        Spacer(Modifier.height(12.dp))
                                        Text(if (language == AppLanguage.ARABIC) "جارٍ تحميل الأرقام المتاحة..." else "Loading available numbers...")
                                    }
                                    is StoreUiState.Error -> Column(
                                        modifier = contentModifier.fillMaxSize().padding(24.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(state.message, color = MaterialTheme.colorScheme.error)
                                        Spacer(Modifier.height(12.dp))
                                        Button(onClick = { storeViewModel.loadServices() }) {
                                            Text(if (language == AppLanguage.ARABIC) "إعادة المحاولة" else "Try again")
                                        }
                                    }
                                    is StoreUiState.Success -> StoreScreen(
                                        strings = strings,
                                        language = language,
                                        walletBalance = 0.0,
                                        services = state.services,
                                        onSelectService = { selectedServiceForCheckout = it },
                                        onTopUpClick = {},
                                        modifier = contentModifier
                                    )
                                }
                                1 -> when (val state = ordersUiState) {
                                    OrdersUiState.Loading -> Column(
                                        modifier = contentModifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator()
                                        Spacer(Modifier.height(12.dp))
                                        Text(if (language == AppLanguage.ARABIC) "جارٍ تحميل طلباتك..." else "Loading your orders...")
                                    }
                                    is OrdersUiState.Error -> Column(
                                        modifier = contentModifier.fillMaxSize().padding(24.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(state.message, color = MaterialTheme.colorScheme.error)
                                        Spacer(Modifier.height(12.dp))
                                        Button(onClick = { ordersViewModel.loadOrders() }) {
                                            Text(if (language == AppLanguage.ARABIC) "إعادة المحاولة" else "Try again")
                                        }
                                    }
                                    is OrdersUiState.Success -> OrdersScreen(
                                        orders = state.orders,
                                        strings = strings,
                                        modifier = contentModifier
                                    )
                                }
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
                                        val message = if (language == AppLanguage.ARABIC) {
                                            "لم يتم إعداد رابط الدعم داخل التطبيق بعد."
                                        } else {
                                            "A support contact link has not been configured in the app yet."
                                        }
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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
                                                    // Avoid carrying a customer's UI state into an administrator session.
                                                    if (customerUserId != null) {
                                                        runCatching { authRepository.signOutCustomer() }
                                                        customerUserId = null
                                                        selectedServiceForCheckout = null
                                                    }
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
                                                    adminPassword = ""
                                                    customerUserId = null
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
