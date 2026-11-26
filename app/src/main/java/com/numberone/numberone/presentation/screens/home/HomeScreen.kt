package com.numberone.numberone.presentation.screens.home

import com.numberone.numberone.core.localization.tr
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.layout.defaultMinSize
import com.numberone.numberone.core.components.SkeletonBlock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.numberone.numberone.core.components.ActionRow
import com.numberone.numberone.core.components.BalanceHeroCard
import com.numberone.numberone.core.components.CopyRow
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
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// The three main screens, in the order they appear in the bottom bar (left to right).
private const val TAB_HOME = 0
private const val TAB_PRIVATE = 1
private const val TAB_INSTANT = 2
private const val TAB_COUNT = 3

private val TAB_BAR_HEIGHT = 72.dp
private val PILL_WIDTH = 64.dp
private val PILL_HEIGHT = 32.dp
private val PILL_TOP = 8.dp

private data class MainTab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

// A function (not a val) so the labels follow the selected language.
private fun mainTabs() = listOf(
    MainTab(tr("الرئيسية"), Icons.Outlined.Home, Icons.Filled.Home),
    MainTab(tr("أرقام خاصة"), Icons.Outlined.SimCard, Icons.Filled.SimCard),
    MainTab(tr("فوري"), Icons.Outlined.Bolt, Icons.Filled.Bolt)
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
 * Main authenticated experience: three screens that swipe horizontally and stay in sync
 * with the bottom bar, ordered الرئيسية | أرقامي | أرقام فورية from left to right.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    vm: AppViewModel,
    userEmail: String?,
    isAdmin: Boolean,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var savedTab by rememberSaveable { mutableIntStateOf(TAB_HOME) }
    val pagerState = rememberPagerState(initialPage = savedTab, pageCount = { TAB_COUNT })

    LaunchedEffect(Unit) {
        // Runs after sign-in, so the backend can bind this device's token to the user.
        PushTokenRegistrar.registerCurrent(context)
    }

    // Remember the settled screen so rotation returns to the same tab.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { savedTab = it }
    }

    // A light tick each time a new screen settles (the first value is skipped).
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .drop(1)
            .collect { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }

    fun goTo(page: Int) {
        scope.launch { pagerState.animateScrollToPage(page) }
    }

    BackHandler(enabled = pagerState.currentPage != TAB_HOME) { goTo(TAB_HOME) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { MainBottomBar(pagerState = pagerState, onSelect = { goTo(it) }) }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(padding),
            beyondViewportPageCount = 1
        ) { page ->
            when (page) {
                TAB_HOME -> DashboardTab(
                    vm = vm,
                    userEmail = userEmail,
                    isAdmin = isAdmin,
                    onNavigate = onNavigate,
                    onOpenTab = { goTo(it) },
                    onLogout = onLogout
                )
                TAB_PRIVATE -> PrivateNumbersTab(vm)
                else -> VirtualNumbersScreen(
                    vm = vm,
                    onBack = { goTo(TAB_HOME) },
                    onTopUp = { onNavigate(AppRoutes.WALLET) },
                    embedded = true
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Bottom bar: three equal slots, one icon per slot, and a pill that slides with the pager.
// ---------------------------------------------------------------------------

@Composable
private fun MainBottomBar(pagerState: PagerState, onSelect: (Int) -> Unit) {
    val tabs = mainTabs()
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Box(modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(TAB_BAR_HEIGHT)) {
                    val slot = maxWidth / tabs.size

                    // The pill follows the live pager position, so it moves with the finger.
                    Box(
                        modifier = Modifier
                            .offset {
                                val slotPx = slot.toPx()
                                val position = pagerState.currentPage + pagerState.currentPageOffsetFraction
                                IntOffset(
                                    x = (position * slotPx + (slotPx - PILL_WIDTH.toPx()) / 2f).roundToInt(),
                                    y = PILL_TOP.roundToPx()
                                )
                            }
                            .size(width = PILL_WIDTH, height = PILL_HEIGHT)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(NumberOneColors.GoldLight, NumberOneColors.GoldMid, NumberOneColors.GoldDeep))
                            )
                    )

                    Row(modifier = Modifier.fillMaxSize()) {
                        tabs.forEachIndexed { index, item ->
                            BottomTabItem(
                                item = item,
                                isSelected = pagerState.currentPage == index,
                                onClick = { onSelect(index) },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomTabItem(
    item: MainTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .semantics { this.selected = isSelected }
            .padding(top = PILL_TOP),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(width = PILL_WIDTH, height = PILL_HEIGHT),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isSelected) item.selectedIcon else item.icon,
                contentDescription = null,
                tint = if (isSelected) NumberOneColors.Obsidian else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            item.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ---------------------------------------------------------------------------
// Tab 1: dashboard
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
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

    val active = remember(vm.history, apiActivation) { findActiveNumbers(vm, apiActivation) }
    val scope = rememberCoroutineScope()
    // Show a placeholder only on the very first load, before any data has arrived.
    val showSkeleton = vm.loading && vm.wallet.balance == 0.0 && vm.transactions.isEmpty()

    PullToRefreshBox(
        isRefreshing = vm.loading,
        onRefresh = {
            vm.loadDashboard()
            scope.launch { repository.latestActivation().onSuccess { apiActivation = it } }
        },
        modifier = Modifier.fillMaxSize()
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GreetingHeader(userEmail = userEmail)
        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(NumberOneColors.Champagne, NumberOneColors.GoldDeep))), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text(tr("تفعيل فوري خلال ثوانٍ وأرقام VIP دائمة تدعم فيسبوك وواتساب"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }

        // 1. Wallet
        if (showSkeleton) SkeletonBlock(height = 150.dp) else BalanceHeroCard(
            label = tr("المحفظة"),
            amount = ltr("${formatAmount(vm.wallet.balance)} ${vm.wallet.currency}"),
            footer = {
                Text(
                    tr("الشحن عبر USDT • BEP20 أو شام كاش"),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.72f)
                )
                Button(
                    onClick = { onNavigate(AppRoutes.WALLET) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = NumberOneColors.Obsidian
                    )
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(tr("شحن الرصيد"), fontWeight = FontWeight.Bold)
                }
            }
        )

        FeedbackBanner(loading = false, error = vm.error, message = null)

        // 2. Active number
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

        // 3. Shortcuts: a tidy 2x2 grid, then the full-width rows.
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(tr("اختصارات"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
                ShortcutTile(tr("الرسائل"), tr("رموز التحقق والرسائل"), Icons.Filled.Sms, { onNavigate(AppRoutes.SMS_INBOX) }, Modifier.weight(1f).fillMaxHeight(), badge = vm.unreadSmsCount)
                ShortcutTile(tr("طلباتي"), tr("سجل الطلبات والمشتريات"), Icons.Filled.ReceiptLong, { onNavigate(AppRoutes.PURCHASE_HISTORY) }, Modifier.weight(1f).fillMaxHeight())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
                ShortcutTile(tr("اشتراكاتي"), tr("إدارة الباقات ورموز التفعيل"), Icons.Filled.Subscriptions, { onNavigate(AppRoutes.SUBSCRIPTION) }, Modifier.weight(1f).fillMaxHeight())
                ShortcutTile(
                    tr("الحساب"),
                    userEmail?.takeIf { it.isNotBlank() } ?: tr("اللغة والمظهر والأمان"),
                    Icons.Filled.Settings,
                    { onNavigate(AppRoutes.SETTINGS) },
                    Modifier.weight(1f).fillMaxHeight()
                )
            }
            ActionRow(tr("الدعم الفني"), tr("المساعدة وخدمة العملاء"), Icons.Filled.SupportAgent, { onNavigate(AppRoutes.SUPPORT) })
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
}

private fun findActiveNumbers(vm: AppViewModel, api: VirtualNumberOrder?): List<ActiveNumber> {
    val now = System.currentTimeMillis()
    fun notExpired(raw: String?): Boolean {
        val t = parseInstantOrNull(raw)?.toEpochMilli() ?: return true
        return t > now
    }
    val result = mutableListOf<ActiveNumber>()
    if (api != null && api.phone.isNotBlank() && api.status.uppercase() in LIVE_API_STATUSES && notExpired(api.expiresAt)) {
        result += ActiveNumber(
            phone = api.phone,
            title = tr("رقم لطلب كود مرة واحدة") + if (api.productCode.isNotBlank()) " • ${api.productCode}" else "",
            expiresAt = api.expiresAt,
            instant = true,
            code = api.smsCode?.takeIf { it.isNotBlank() }
        )
    }
    vm.history
        .filter { !it.phoneNumber.isNullOrBlank() && statusTone(it.status) == StatusTone.SUCCESS && notExpired(it.expiresAt) }
        .maxByOrNull { it.createdAt }
        ?.let { o ->
            result += ActiveNumber(
                phone = o.phoneNumber.orEmpty(),
                title = tr("رقم اشتراك شهري ودائم") + if (o.serviceName.isNotBlank()) " • ${o.serviceName}" else "",
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
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(successColor()))
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
                Text(tr("عرض الرسائل"), fontWeight = FontWeight.Bold)
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
            Text(tr("اختر باقة الرقم المناسبة لك"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text(
                tr("حدد نوع الرقم حسب احتياجك الفوري أو الدائم"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onInstant, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(tr("رقم تفعيل فوري"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                        Text(tr("كود سريع لمرة واحدة • واتساب وتيليجرام"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
                    }
                }
                OutlinedButton(onClick = onPrivate, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(tr("رقم خاص دائم (VIP)"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                        Text(tr("مخصص لك • يقبله فيسبوك والمنصات الحساسة"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun GreetingHeader(userEmail: String?) {
    val name = userEmail?.substringBefore("@")?.takeIf { it.isNotBlank() }
    val initial = name?.firstOrNull()?.uppercaseChar()?.toString() ?: "N"
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text(initial, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(tr("مرحباً بعودتك"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                name ?: tr("عميلنا"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Tab 2: private (manual) numbers
// ---------------------------------------------------------------------------

@Composable
private fun PrivateNumbersTab(vm: AppViewModel) {
    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    PullToRefreshBox(
        isRefreshing = vm.loading,
        onRefresh = { vm.loadStore(); vm.loadWallet() },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(tr("الأرقام الخاصة (VIP)"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(
                    tr("أرقام خاصة شهرية وسنوية تُجهَّز وتُسلَّم يدوياً من الإدارة."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StoreBody(vm)
        }
    }
}

@Composable
private fun ShortcutTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Int = 0
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 132.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                IconBadge(icon, tint = MaterialTheme.colorScheme.primary, size = 44.dp)
                Spacer(Modifier.weight(1f))
                if (badge > 0) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 22.dp)
                            .height(22.dp)
                            .clip(CircleShape)
                            .background(NumberOneColors.GoldMid)
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (badge > 99) "99+" else badge.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = NumberOneColors.Obsidian
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
