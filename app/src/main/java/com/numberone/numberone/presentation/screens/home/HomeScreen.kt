package com.numberone.numberone.presentation.screens.home

import com.numberone.numberone.core.localization.tr
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.numberone.numberone.core.components.ActionRow
import com.numberone.numberone.core.components.BalanceHeroCard
import com.numberone.numberone.core.components.CopyRow
import com.numberone.numberone.core.components.NOCard
import com.numberone.numberone.core.components.formatDateTime
import com.numberone.numberone.core.components.ExpiryCountdown
import com.numberone.numberone.core.components.FeedbackBanner
import com.numberone.numberone.core.components.IconBadge
import com.numberone.numberone.core.components.StatusTone
import com.numberone.numberone.core.components.formatAmount
import com.numberone.numberone.core.components.ltr
import com.numberone.numberone.core.components.statusTone
import com.numberone.numberone.core.components.successColor
import com.numberone.numberone.core.theme.NumberOneColors
import com.numberone.numberone.core.util.parseInstantOrNull
import com.numberone.numberone.di.AppContainer
import com.numberone.numberone.domain.model.VirtualNumberOrder
import com.numberone.numberone.notifications.PushTokenRegistrar
import com.numberone.numberone.presentation.navigation.AppRoutes
import com.numberone.numberone.presentation.screens.store.StoreBody
import com.numberone.numberone.presentation.screens.virtualnumbers.VirtualNumbersScreen
import com.numberone.numberone.presentation.viewmodel.AppViewModel

private const val TAB_HOME = 0
private const val TAB_PRIVATE = 1
private const val TAB_INSTANT = 2

private data class MainTab(val label: String, val icon: ImageVector)

// A function (not a val) so the labels follow the selected language.
private fun mainTabs() = listOf(
    MainTab(tr("الرئيسية"), Icons.Filled.Home),
    MainTab(tr("أرقامي"), Icons.Filled.SimCard),
    MainTab(tr("أرقام فورية"), Icons.Filled.Bolt)
)

private val LIVE_API_STATUSES = setOf("PENDING", "WAITING", "ACTIVE", "RECEIVED")

/** The number the customer currently owns, from either a private (manual) order or a live API activation. */
private class ActiveNumber(
    val phone: String,
    val title: String,
    val expiresAt: String?,
    val instant: Boolean,
    val code: String?
)

/**
 * Main authenticated experience: three inner tabs, ordered physically left to right as
 * الرئيسية | أرقامي | أرقام فورية (the bar is forced LTR so the order holds in Arabic/RTL too).
 */
@Composable
fun HomeScreen(
    vm: AppViewModel,
    userEmail: String?,
    isAdmin: Boolean,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(TAB_HOME) }

    LaunchedEffect(Unit) {
        // Runs after sign-in, so the backend can bind this device's token to the user.
        PushTokenRegistrar.registerCurrent(context)
    }

    BackHandler(enabled = tab != TAB_HOME) { tab = TAB_HOME }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    tonalElevation = 0.dp
                ) {
                    mainTabs().forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = NumberOneColors.IndigoRoyal,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = {
                                Text(
                                    item.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (tab == index) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                TAB_HOME -> DashboardTab(
                    vm = vm,
                    userEmail = userEmail,
                    isAdmin = isAdmin,
                    onNavigate = onNavigate,
                    onOpenTab = { tab = it },
                    onLogout = onLogout
                )
                TAB_PRIVATE -> PrivateNumbersTab(vm)
                else -> VirtualNumbersScreen(
                    vm = vm,
                    onBack = { tab = TAB_HOME },
                    onTopUp = { onNavigate(AppRoutes.WALLET) },
                    embedded = true
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Tab 1: dashboard
// ---------------------------------------------------------------------------

@Composable
private fun DashboardTab(
    vm: AppViewModel,
    userEmail: String?,
    isAdmin: Boolean,
    onNavigate: (String) -> Unit,
    onOpenTab: (Int) -> Unit,
    onLogout: () -> Unit
) {
    val repository = remember { AppContainer.virtualNumberRepository }
    var apiActivation by remember { mutableStateOf<VirtualNumberOrder?>(null) }

    LaunchedEffect(Unit) {
        vm.loadDashboard()
        repository.latestActivation().onSuccess { apiActivation = it }
    }
    // New numbers and verification codes appear without a manual refresh. Stops when the screen leaves.
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            vm.refreshAccount()
        }
    }

    val active = remember(vm.history, apiActivation) { findActiveNumbers(vm, apiActivation) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Number at the top: the real number appears only once it is active (see findActiveNumbers).
        if (active.isEmpty()) {
            GetNumberCard(onInstant = { onOpenTab(TAB_INSTANT) }, onPrivate = { onOpenTab(TAB_PRIVATE) })
        } else {
            active.forEach { number ->
                ActiveNumberCard(
                    number = number,
                    onViewMessages = {
                        if (number.instant) onOpenTab(TAB_INSTANT) else onNavigate(AppRoutes.SMS_INBOX)
                    }
                )
            }
        }

        // 2. Access code and this number's messages (placed directly under the number)
        AccessAndMessagesCard(vm)

        // 3. Wallet
        BalanceHeroCard(
            label = tr("المحفظة"),
            amount = ltr("${formatAmount(vm.wallet.balance)} ${vm.wallet.currency}"),
            footer = {
                Text(
                    tr("الشبكة: BEP20 • شام كاش"),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.72f)
                )
                Button(
                    onClick = { onNavigate(AppRoutes.WALLET) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = NumberOneColors.IndigoRoyal
                    )
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(tr("  شحن الرصيد"), fontWeight = FontWeight.Bold)
                }
            }
        )

        FeedbackBanner(loading = false, error = vm.error, message = null)

        // 4. Remaining sections
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(tr("اختصارات"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ActionRow(tr("الرسائل"), tr("رموز التحقق والرسائل الواردة"), Icons.Filled.Sms, { onNavigate(AppRoutes.SMS_INBOX) })
            ActionRow(tr("طلباتي"), tr("متابعة مشترياتك السابقة"), Icons.Filled.ReceiptLong, { onNavigate(AppRoutes.PURCHASE_HISTORY) })
            ActionRow(tr("اشتراكاتي"), tr("حالة الاشتراك ورمز الوصول"), Icons.Filled.Subscriptions, { onNavigate(AppRoutes.SUBSCRIPTION) })
            ActionRow(
                tr("الحساب"),
                userEmail?.takeIf { it.isNotBlank() } ?: tr("اللغة والمظهر والأمان"),
                Icons.Filled.Settings,
                { onNavigate(AppRoutes.SETTINGS) }
            )
            ActionRow(tr("الدعم الفني"), tr("تواصل مع فريق الدعم"), Icons.Filled.SupportAgent, { onNavigate(AppRoutes.SUPPORT) })
            if (isAdmin) {
                ActionRow(
                    tr("لوحة الإدارة"),
                    tr("مراجعة الطلبات والإيداعات"),
                    Icons.Filled.AdminPanelSettings,
                    { onNavigate(AppRoutes.ADMIN) },
                    highlighted = true
                )
            }
        }

        TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text(tr("تسجيل الخروج"), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun findActiveNumbers(vm: AppViewModel, api: VirtualNumberOrder?): List<ActiveNumber> {
    val now = System.currentTimeMillis()
    fun notExpired(raw: String?): Boolean {
        val t = parseInstantOrNull(raw)?.toEpochMilli() ?: return true
        return t > now
    }
    val result = mutableListOf<ActiveNumber>()
    // Numbers linked to the account (manual purchases and approved orders). Expired ones come back as 'expired'.
    vm.mySubscriptions
        .filter { it.status.equals("active", ignoreCase = true) && it.phoneNumber.isNotBlank() }
        .forEach { sub ->
            result += ActiveNumber(
                phone = sub.phoneNumber,
                title = tr("رقمك") + if (!sub.serviceName.isNullOrBlank()) " • ${sub.serviceName}" else "",
                expiresAt = sub.expiryDate?.let { "${it}T23:59:59Z" },
                instant = false,
                code = sub.latestOtp?.takeIf { it.isNotBlank() }
            )
        }
    if (api != null && api.phone.isNotBlank() && api.status.uppercase() in LIVE_API_STATUSES && notExpired(api.expiresAt)) {
        result += ActiveNumber(
            phone = api.phone,
            title = tr("رقم فوري") + if (api.productCode.isNotBlank()) " • ${api.productCode}" else "",
            expiresAt = api.expiresAt,
            instant = true,
            code = api.smsCode?.takeIf { it.isNotBlank() }
        )
    }
    vm.history
        .filter { !it.phoneNumber.isNullOrBlank() && statusTone(it.status) == StatusTone.SUCCESS && notExpired(it.expiresAt) }
        .maxByOrNull { it.createdAt }
        ?.takeIf { o -> result.none { it.phone == o.phoneNumber.orEmpty() } }
        ?.let { o ->
            result += ActiveNumber(
                phone = o.phoneNumber.orEmpty(),
                title = tr("رقم خاص") + if (o.serviceName.isNotBlank()) " • ${o.serviceName}" else "",
                expiresAt = o.expiresAt,
                instant = false,
                code = o.smsCode?.takeIf { it.isNotBlank() }
            )
        }
    return result
}

@Composable
private fun ActiveNumberCard(number: ActiveNumber, onViewMessages: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(Icons.Filled.PhoneAndroid, size = 46.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(tr("رقمك النشط"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text(
                        number.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = successColor().copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(successColor()))
                        Text(tr("نشط"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = successColor())
                    }
                }
            }
            CopyRow(label = tr("رقمك • اضغط للنسخ"), value = number.phone, emphasized = true)
            number.code?.let { CopyRow(label = tr("آخر رمز تحقق"), value = it) }
            ExpiryCountdown(expiresAt = number.expiresAt)
            Button(
                onClick = onViewMessages,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Sms, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(tr("  عرض الرسائل"), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GetNumberCard(onInstant: () -> Unit, onPrivate: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(54.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(30.dp))
            }
            Text(tr("+ احصل على رقم الآن"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text(
                tr("لا يوجد لديك رقم نشط. فعّل واتساب أو تيليجرام برقم فوري، أو اشترك برقم خاص."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onInstant, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(14.dp)) {
                    Text(tr("رقم فوري"), fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onPrivate, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(14.dp)) {
                    Text(tr("رقم خاص"), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Tab 2: private (manual) numbers
// ---------------------------------------------------------------------------

@Composable
private fun PrivateNumbersTab(vm: AppViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(tr("أرقامي"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(
                tr("أرقام خاصة شهرية وسنوية تُجهَّز وتُسلَّم يدوياً من الإدارة."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        StoreBody(vm)
    }
}

/** The account's messages, shown automatically under the active number (no access code). */
@Composable
private fun AccessAndMessagesCard(vm: AppViewModel) {
    if (vm.sms.isEmpty()) return
    NOCard {
        Text(tr("رسائل رقمك"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        vm.sms.take(20).forEach { m ->
            val code = Regex("\\b\\d{4,8}\\b").find(m.messageText)?.value
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            m.sender.ifBlank { tr("رسالة") },
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            formatDateTime(m.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(m.messageText, style = MaterialTheme.typography.bodyMedium)
                    code?.let { CopyRow(label = tr("رمز التحقق • اضغط للنسخ"), value = it) }
                }
            }
        }
    }
}
