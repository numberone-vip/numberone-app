package com.numberone.numberone.presentation.navigation

import com.numberone.numberone.core.localization.tr
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.numberone.numberone.di.AppContainer
import com.numberone.numberone.domain.model.UserProfile
import com.numberone.numberone.presentation.screens.admin.AdminScreen
import com.numberone.numberone.presentation.screens.auth.AccessCodeScreen
import com.numberone.numberone.presentation.screens.auth.LoginScreen
import com.numberone.numberone.presentation.screens.auth.SignupScreen
import com.numberone.numberone.presentation.screens.home.HomeScreen
import com.numberone.numberone.presentation.screens.orders.OrdersScreen
import com.numberone.numberone.presentation.screens.settings.SettingsScreen
import com.numberone.numberone.presentation.screens.sms.SmsInboxScreen
import com.numberone.numberone.presentation.screens.store.StoreScreen
import com.numberone.numberone.presentation.screens.subscription.SubscriptionsScreen
import com.numberone.numberone.presentation.screens.support.SupportScreen
import com.numberone.numberone.presentation.screens.wallet.WalletScreen
import com.numberone.numberone.presentation.screens.virtualnumbers.VirtualNumbersScreen
import com.numberone.numberone.core.components.NOScaffold
import com.numberone.numberone.presentation.viewmodel.AppViewModel
import com.numberone.numberone.presentation.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    var currentUser by remember { mutableStateOf<UserProfile?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        currentUser = AppContainer.userRepository.getCurrentUser().getOrNull()
        if (currentUser != null && navController.currentDestination?.route == AppRoutes.LOGIN) {
            navController.navigate(AppRoutes.HOME) { popUpTo(AppRoutes.LOGIN) { inclusive = true } }
        }
    }

    val appViewModel = remember { AppViewModel() }
    val loginViewModel = remember { LoginViewModel() }
    var isAdmin by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        isAdmin = currentUser != null && AppContainer.userRepository.isAdmin().getOrDefault(false)
    }

    fun signOutToLogin() {
        appViewModel.signOut {
            currentUser = null
            isAdmin = false
            navController.navigate(AppRoutes.LOGIN) { popUpTo(0) { inclusive = true } }
        }
    }

    LaunchedEffect(appViewModel.sessionExpired) {
        if (appViewModel.sessionExpired) {
            appViewModel.acknowledgeSessionExpired()
            signOutToLogin()
        }
    }

    NavHost(navController = navController, startDestination = AppRoutes.LOGIN) {
        composable(AppRoutes.LOGIN) {
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    scope.launch {
                        currentUser = AppContainer.userRepository.getCurrentUser().getOrNull()
                        isAdmin = AppContainer.userRepository.isAdmin().getOrDefault(false)
                        navController.navigate(AppRoutes.HOME) { popUpTo(AppRoutes.LOGIN) { inclusive = true } }
                    }
                },
                onCreateAccountClick = { navController.navigate(AppRoutes.CREATE_ACCOUNT) }
            )
        }
        composable(AppRoutes.CREATE_ACCOUNT) {
            SignupScreen(
                viewModel = loginViewModel,
                onBack = { navController.popBackStack() },
                // Account created: go straight back to Sign-In.
                onRegistered = { navController.popBackStack() }
            )
        }
        composable(AppRoutes.ACCESS_CODE) {
            AccessCodeScreen(
                vm = appViewModel,
                onBack = { navController.popBackStack() },
                onDone = { navController.navigate(AppRoutes.SUBSCRIPTION) }
            )
        }
        composable(AppRoutes.HOME) {
            HomeScreen(
                vm = appViewModel,
                userEmail = currentUser?.email,
                isAdmin = isAdmin,
                onNavigate = { route -> navController.navigate(route) },
                onLogout = { signOutToLogin() }
            )
        }
        composable(AppRoutes.WALLET) {
            WalletScreen(vm = appViewModel, onBack = { navController.popBackStack() })
        }
        composable(AppRoutes.PURCHASE) {
            StoreScreen(
                vm = appViewModel,
                onBack = { navController.popBackStack() },
                onTopUp = { navController.navigate(AppRoutes.WALLET) }
            )
        }
        composable(AppRoutes.PURCHASE_HISTORY) {
            OrdersScreen(vm = appViewModel, onBack = { navController.popBackStack() })
        }
        composable(AppRoutes.VIRTUAL_NUMBERS) {
            VirtualNumbersScreen(
                vm = appViewModel,
                onBack = { navController.popBackStack() },
                onTopUp = { navController.navigate(AppRoutes.WALLET) }
            )
        }
        composable(AppRoutes.SUBSCRIPTION) {
            SubscriptionsScreen(
                vm = appViewModel,
                onBack = { navController.popBackStack() },
                onSms = { navController.navigate(AppRoutes.SMS_INBOX) },
                onAccessCode = { navController.navigate(AppRoutes.ACCESS_CODE) }
            )
        }
        composable(AppRoutes.SMS_INBOX) {
            SmsInboxScreen(vm = appViewModel, onBack = { navController.popBackStack() })
        }
        composable(AppRoutes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = { signOutToLogin() },
                userEmail = currentUser?.email
            )
        }
        composable(AppRoutes.SUPPORT) {
            SupportScreen(onBack = { navController.popBackStack() })
        }
        composable(AppRoutes.ADMIN) {
            if (isAdmin) {
                AdminScreen(onBack = { navController.popBackStack() })
            } else {
                NOScaffold(title = tr("لوحة الإدارة"), onBack = { navController.popBackStack() }) {
                    Text(tr("ليس لديك صلاحية الوصول إلى هذه الصفحة."), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
