package com.numberone.numberone.presentation.screens.home

import com.numberone.numberone.core.localization.tr
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Share
import com.numberone.numberone.presentation.screens.referrals.buildReferralShareMessage
import androidx.compose.material.icons.filled.Notifications
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
import com.numberone.numberone.data.remote.SupabaseClientProvider
import com.numberone.numberone.di.AppContainer
import io.github.jan.supabase.auth.auth
import com.numberone.numberone.domain.model.VirtualNumberOrder
import com.numberone.numberone.notifications.PushTokenRegistrar
import com.numberone.numberone.presentation.navigation.AppRoutes
import com.numberone.numberone.presentation.screens.store.StoreBody
import com.numberone.numberone.presentation.screens.virtualnumbers.VirtualNumbersScreen
import com.numberone.numberone.presentation.viewmodel.AppViewModel
import com.numberone.numberone.presentation.viewmodel.EngagementViewModel
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
    engagementVm: EngagementViewModel? = null,
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
                    engagementVm = engagementVm,
                    userEmail = userEmail,
                    isAdmin = isAdmin,
                    onNavigate = onNavigate,
                    onOpenTab = { goTo(it) },
                    onLogout = onLogout
                )
                TAB_PRIVATE -> PrivateNumbersTab(vm, onNavigate)
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
    engagementVm: EngagementViewModel? = null,
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
        engagementVm?.refreshUnreadCount()
        engagementVm?.loadMyReferralCode()
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
        GreetingHeader(userEmail = userEmail, unreadNotifications = engagementVm?.unreadCount ?: 0, onNotifications = { onNavigate(AppRoutes.NOTIFICATIONS) })
        VipSpotlightBanner(
            onPrivate = { onOpenTab(TAB_PRIVATE) },
            onInstant = { onOpenTab(TAB_INSTANT) },
            onReferrals = { onNavigate(AppRoutes.REFERRALS) }
        )
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
                    userEmail = userEmail,
                    onViewMessages = {
                        if (number.instant) onOpenTab(TAB_INSTANT) else onNavigate(AppRoutes.SMS_INBOX)
                    }
                )
            }
        }

        ActiveVipNumberHomeCard(vm = vm, onNavigate = onNavigate)

        // 3. Shortcuts: a tidy 2x2 grid, then the full-width rows.
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(tr("اختصارات"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
                ShortcutTile(tr("الرسائل"), tr("رموز التحقق والرسائل"), Icons.Filled.Sms, { onNavigate(AppRoutes.SMS_INBOX) }, Modifier.weight(1f).fillMaxHeight(), badge = vm.unreadSmsCount)
                ShortcutTile(tr("طلباتي"), tr("سجل الطلبات والمشتريات"), Icons.Filled.ReceiptLong, { onNavigate(AppRoutes.PURCHASE_HISTORY) }, Modifier.weight(1f).fillMaxHeight())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
                ShortcutTile(tr("اشتراكاتي"), tr("أرقامك الخاصة ورسائلها النشطة"), Icons.Filled.Subscriptions, { onNavigate(AppRoutes.SUBSCRIPTION) }, Modifier.weight(1f).fillMaxHeight())
                ShortcutTile(
                    tr("الحساب"),
                    userEmail?.takeIf { it.isNotBlank() } ?: tr("اللغة والمظهر والأمان"),
                    Icons.Filled.Settings,
                    { onNavigate(AppRoutes.SETTINGS) },
                    Modifier.weight(1f).fillMaxHeight()
                )
            }
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)), modifier = Modifier.fillMaxWidth()) {
                val shareCtx = LocalContext.current
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconBadge(Icons.Filled.CardGiftcard, size = 40.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tr("🎁 ادعُ أصدقاءك واربح مكافآت!"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                            Text(tr("شارك كود الدعوة ورابط التطبيق مع أصدقائك بضغطة واحدة"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { val c = engagementVm?.myReferralCode.orEmpty(); if (c.isNotBlank()) { val i = android.content.Intent(android.content.Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(android.content.Intent.EXTRA_TEXT, buildReferralShareMessage(c)) }; runCatching { shareCtx.startActivity(android.content.Intent.createChooser(i, tr("مشاركة كود الدعوة"))) } } else onNavigate(AppRoutes.REFERRALS) }, modifier = Modifier.weight(1f).height(42.dp), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(tr("مشاركة الآن"), fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(onClick = { onNavigate(AppRoutes.REFERRALS) }, modifier = Modifier.weight(1f).height(42.dp), shape = RoundedCornerShape(12.dp)) { Text(tr("صفحة الإحالات"), fontWeight = FontWeight.Bold) }
                    }
                }
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
private fun ActiveNumberCard(number: ActiveNumber, userEmail: String? = null, onViewMessages: () -> Unit) {
    val ctx = LocalContext.current
    var askCodeOpen by remember { mutableStateOf(false) }
    if (askCodeOpen) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { askCodeOpen = false },
            title = { Text(tr("طلب كود تحقق جديد (SMS)"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr("اختر التطبيق الذي طلبت منه الكود وسيتم إرسال طلبك فوراً:"), style = MaterialTheme.typography.bodySmall)
                    listOf("WhatsApp", "Facebook", "Telegram", "Google", "TikTok", tr("تطبيق آخر")).forEach { appName ->
                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                askCodeOpen = false
                                val msg = "طلب كود تحقق جديد (VIP)\nالرقم: ${number.phone}\nالتطبيق: $appName\nالحساب: ${userEmail ?: "-"}"
                                val uri = android.net.Uri.parse("https://t.me/NumbersStore_Sy?text=" + android.net.Uri.encode(msg))
                                runCatching { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri)) }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(appName, fontWeight = FontWeight.Bold) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { askCodeOpen = false }) { Text(tr("إغلاق")) } }
        )
    }
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
            if (!number.instant) {
                androidx.compose.material3.OutlinedButton(
                    onClick = { askCodeOpen = true },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(tr("طلب كود جديد الآن (SMS)"), fontWeight = FontWeight.Bold)
                }
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
                        Text(tr("كود سريع لمرة واحدة • 1346 تطبيق حول العالم"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
                    }
                }
                OutlinedButton(onClick = onPrivate, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(tr("رقم خاص دائم (VIP)"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                        Text(tr("مخصص لك • يقبله فيسبوك وواتساب وجميع المنصات العالمية"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun GreetingHeader(userEmail: String?, unreadNotifications: Int = 0, onNotifications: () -> Unit = {}) {
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
            val uid = remember { SupabaseClientProvider.client.auth.currentUserOrNull()?.id.orEmpty() }
            if (uid.isNotBlank()) {
                val clip = androidx.compose.ui.platform.LocalClipboardManager.current
                val ctx = LocalContext.current
                Spacer(Modifier.height(4.dp))
                Surface(
                    onClick = {
                        clip.setText(androidx.compose.ui.text.AnnotatedString(uid))
                        android.widget.Toast.makeText(ctx, tr("تم نسخ معرّف حسابك (ID)"), android.widget.Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = ltr("ID: ${uid.take(8)}…"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = tr("نسخ"),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
        androidx.compose.material3.BadgedBox(badge = { if (unreadNotifications > 0) androidx.compose.material3.Badge { Text(unreadNotifications.toString()) } }) {
            androidx.compose.material3.IconButton(onClick = onNotifications) { Icon(Icons.Filled.Notifications, contentDescription = tr("الإشعارات"), tint = MaterialTheme.colorScheme.primary) }
        }
    }
}

// ---------------------------------------------------------------------------
// Tab 2: private (manual) numbers
// ---------------------------------------------------------------------------

@Composable
private fun PrivateNumbersTab(vm: AppViewModel, onNavigate: (String) -> Unit = {}) {
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
                    tr("أرقام أعمال سحابية فورية التفعيل • استقبال SMS مباشر على مدار الساعة"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            com.numberone.numberone.data.remote.TelnyxCustomerCard(onOpenSms = { onNavigate(AppRoutes.SMS_INBOX) }, onPurchased = { vm.loadStore(); vm.loadWallet() })
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

@Composable
private fun ActiveVipNumberHomeCard(vm: AppViewModel, onNavigate: (String) -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (vm.history.isEmpty()) vm.loadHistory()
    }
    val activeSub = vm.subscriptions.firstOrNull { it.phoneNumber.isNotBlank() }
    val approvedOrder = vm.history.firstOrNull {
        !it.phoneNumber.isNullOrBlank() && it.status.uppercase() in setOf("COMPLETED", "APPROVED", "DELIVERED", "ACTIVE")
    }
    val phone = activeSub?.phoneNumber?.takeIf { it.isNotBlank() } ?: approvedOrder?.phoneNumber.orEmpty()
    if (phone.isBlank()) return

    val title = activeSub?.serviceName?.takeIf { it.isNotBlank() }
        ?: approvedOrder?.serviceName?.takeIf { it.isNotBlank() }
        ?: tr("رقم خاص (VIP)")
    val code = activeSub?.latestOtp?.takeIf { it.isNotBlank() }
        ?: approvedOrder?.smsCode?.takeIf { it.isNotBlank() }
    val expiry = activeSub?.expiryDate ?: approvedOrder?.expiresAt

    com.numberone.numberone.core.components.NOCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                tr("👑 رقمك الخاص النشط (VIP)"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            com.numberone.numberone.core.components.StatusChip("ACTIVE")
        }
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        com.numberone.numberone.core.components.CopyField(
            label = tr("رقم الهاتف"),
            value = phone
        )
        if (!code.isNullOrBlank()) {
            com.numberone.numberone.core.components.CopyField(
                label = tr("كود التحقق"),
                value = code
            )
        } else {
            Text(
                tr("بانتظار وصول كود التحقق من الإدارة…"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
        expiry?.takeIf { it.isNotBlank() }?.let {
            com.numberone.numberone.core.components.ExpiryBadge(expiry = it)
        }
        androidx.compose.material3.HorizontalDivider()
        Text(tr("🛡️ تعليمات حماية رقمك من الحظر"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(tr("• في اليوم الأول للتفعيل: استقبل الرسائل الواردة فقط ولا تبدأ محادثات مع أرقام غير مسجّلة على هاتفك."), style = MaterialTheme.typography.bodySmall)
        Text(tr("• فعّل التحقق بخطوتين فوراً (PIN) لحماية حسابك."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(tr("• تجنّب الرسائل الجماعية والروابط والانضمام لمجموعات كثيرة في الأيام الأولى."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            androidx.compose.material3.OutlinedButton(
                onClick = {
                    vm.loadHistory()
                    activeSub?.id?.takeIf { it.isNotBlank() }?.let { vm.loadSubscriptionById(it) }
                },
                enabled = !vm.loading,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(tr("تحديث الكود"), fontWeight = FontWeight.Bold)
            }
            androidx.compose.material3.Button(
                onClick = { onNavigate(AppRoutes.SMS_INBOX) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(tr("صندوق الرسائل"), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun VipSpotlightBanner(onPrivate: () -> Unit, onInstant: () -> Unit, onReferrals: () -> Unit) {
    var slide by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(4000L); slide = (slide + 1) % 3 } }
    val titles = listOf(tr("👑 أرقام خاصة دائمة (VIP)"), tr("⚡ تفعيل فوري خلال ثوانٍ"), tr("🎁 ادعُ أصدقاءك واربح رصيداً"))
    val subtitles = listOf(
        tr("مخصّصة لك وحدك • تدعم واتساب وتيليجرام وفيسبوك بأعلى استقرار"),
        tr("أكثر من 1,340 تطبيقاً ودولة • استرداد تلقائي خلال 15 دقيقة"),
        tr("شارك كود الدعوة واحصل على مكافآت وخصومات حصرية")
    )
    val icons = listOf(Icons.Filled.Bolt, Icons.Filled.Bolt, Icons.Filled.CardGiftcard)
    val actions = listOf(onPrivate, onInstant, onReferrals)

    Surface(
        onClick = actions[slide],
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(1.2.dp, Brush.horizontalGradient(listOf(NumberOneColors.GoldLight, NumberOneColors.GoldDeep))),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.background(Brush.linearGradient(listOf(Color(0xFF231D13), Color(0xFF14110C), Color(0xFF2A2112)))).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            androidx.compose.animation.Crossfade(targetState = slide, label = "vip_slide") { idx ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(Brush.linearGradient(listOf(NumberOneColors.GoldLight, NumberOneColors.GoldDeep)))
                    ) { Icon(icons[idx], contentDescription = null, tint = NumberOneColors.Obsidian, modifier = Modifier.size(24.dp)) }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(titles[idx], style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = NumberOneColors.GoldLight)
                        Text(subtitles[idx], style = MaterialTheme.typography.bodySmall, color = Color(0xFFE8DFCE))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.align(Alignment.CenterHorizontally)) {
                (0..2).forEach { i ->
                    Box(modifier = Modifier.clickable { slide = i }.size(width = if (slide == i) 20.dp else 7.dp, height = 6.dp).clip(CircleShape).background(if (slide == i) NumberOneColors.GoldLight else Color.White.copy(alpha = 0.28f)))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTab(
    vm: AppViewModel,
    userEmail: String?,
    offer: TelnyxOffer?,
    selectedCaller: String,
    unreadNotifications: Int,
    onOpenAddNumber: () -> Unit,
    onSelectCaller: (String) -> Unit,
    onNavigate: (String) -> Unit,
    onRefresh: () -> Unit
) {
    val clip = LocalClipboardManager.current
    val ctx = LocalContext.current
    val uid = remember { SupabaseClientProvider.client.auth.currentUserOrNull()?.id.orEmpty() }
    val name = userEmail?.substringBefore("@")?.takeIf { it.isNotBlank() } ?: tr("عميلنا")

    PullToRefreshBox(isRefreshing = vm.loading, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("NUMBER ONE", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                BadgedBox(badge = { if (unreadNotifications > 0) Badge { Text(unreadNotifications.toString()) } }) {
                    IconButton(onClick = { onNavigate(AppRoutes.NOTIFICATIONS) }) { Icon(Icons.Filled.Notifications, contentDescription = tr("الإشعارات"), tint = MaterialTheme.colorScheme.primary) }
                }
            }
            Text(trf("مرحباً، {0}", name), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(tr("الرصيد المتاح"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        if (uid.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.clickable {
                                clip.setText(AnnotatedString(uid))
                                android.widget.Toast.makeText(ctx, tr("تم نسخ معرّف حسابك"), android.widget.Toast.LENGTH_SHORT).show()
                            }) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(successColor()))
                                Text(ltr("ID: ${uid.take(8)}"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(ltr("${formatAmount(vm.wallet.balance)} ${vm.wallet.currency}"), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                        Button(onClick = { onNavigate(AppRoutes.WALLET) }, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = NumberOneColors.GoldMid, contentColor = NumberOneColors.Obsidian)) {
                            Text(tr("شحن الرصيد"), fontWeight = FontWeight.Black)
                        }
                    }
                    if (vm.wallet.balance < 1.0) Text(tr("الرصيد منخفض — اشحن محفظتك لمواصلة المكالمات وشراء الأرقام"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Surface(onClick = onOpenAddNumber, shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(modifier = Modifier.size(46.dp).clip(CircleShape).background(NumberOneColors.GoldMid), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = NumberOneColors.Obsidian, modifier = Modifier.size(26.dp))
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(tr("احصل على رقم أعمال جديد"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        Text(tr("US • Canada • UK • Australia • مكالمات + SMS"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            val myNums = offer?.numbers.orEmpty()
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(tr("أرقامي النشطة"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                TextButton(onClick = onOpenAddNumber) { Text(tr("إضافة رقم"), fontWeight = FontWeight.Bold) }
            }
            if (myNums.isEmpty()) {
                NOCard { Text(tr("لا تملك رقماً سحابياً نشطاً بعد. اضغط «احصل على رقم أعمال جديد» لاختيار رقمك المفضل."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                myNums.forEach { n ->
                    val isCaller = (selectedCaller.ifBlank { myNums.first().phoneNumber }) == n.phoneNumber
                    Surface(onClick = { onSelectCaller(n.phoneNumber) }, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                                Text("US", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(ltr(n.phoneNumber), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                                n.expiresAt?.let { Text(tr("يتجدد في ") + ltr(it.take(10)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary.copy(alpha = if (isCaller) 0.18f else 0.08f)) {
                                Text(if (isCaller) "Caller ID ›" else tr("اتصال ›"), modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            Surface(onClick = { onNavigate(AppRoutes.VIRTUAL_NUMBERS) }, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(Icons.Filled.Bolt, size = 44.dp)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(tr("أرقام تفعيل فورية (5SIM)"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                        Text(tr("كود سريع لمرة واحدة • أكثر من 1,340 تطبيقاً ودولة"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AccountTab(
    vm: AppViewModel,
    engagementVm: EngagementViewModel?,
    userEmail: String?,
    isAdmin: Boolean,
    offer: TelnyxOffer?,
    activity: TelnyxUserActivity?,
    selectedCaller: String,
    onSelectCaller: (String) -> Unit,
    onOpenBuyNumber: () -> Unit,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val clip = LocalClipboardManager.current
    val ctx = LocalContext.current
    val uid = remember { SupabaseClientProvider.client.auth.currentUserOrNull()?.id.orEmpty() }
    val name = userEmail?.substringBefore("@")?.takeIf { it.isNotBlank() } ?: tr("عميلنا")
    val numbers = offer?.numbers.orEmpty()
    val caller = selectedCaller.ifBlank { numbers.firstOrNull()?.phoneNumber.orEmpty() }
    var txDialogOpen by remember { mutableStateOf(false) }

    if (txDialogOpen) {
        AlertDialog(
            onDismissRequest = { txDialogOpen = false },
            title = { Text(tr("سجل المعاملات والفواتير"), fontWeight = FontWeight.Black) },
            text = {
                Column(modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val txs = activity?.transactions.orEmpty()
                    if (txs.isEmpty()) Text(tr("لا توجد معاملات مسجلة بعد."), style = MaterialTheme.typography.bodySmall)
                    txs.forEach { t ->
                        val title = when (t.referenceType) {
                            "telnyx_voice" -> tr("مكالمة صوتية دولية")
                            "telnyx_sms" -> tr("رسالة SMS صادرة")
                            "telnyx_purchase" -> tr("شراء رقم سحابي")
                            else -> t.description.ifBlank { tr("معاملة مالية") }
                        }
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text(ltr("${if (t.direction == "debit") "-" else "+"}${formatAmount(t.amount)} USDT"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = if (t.direction == "debit") dangerColor() else successColor())
                                }
                                if (t.description.isNotBlank()) Text(t.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(t.createdAt.take(16).replace("T", " "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { txDialogOpen = false }) { Text(tr("إغلاق")) } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(tr("حسابي"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                    Text(name.take(2).uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    if (!userEmail.isNullOrBlank()) Text(userEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (uid.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.clickable {
                            clip.setText(AnnotatedString(uid))
                            android.widget.Toast.makeText(ctx, tr("تم نسخ معرّف الحساب"), android.widget.Toast.LENGTH_SHORT).show()
                        }) {
                            Text(ltr("ID: ${uid.take(8)}…"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Icon(Icons.Filled.ContentCopy, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Surface(onClick = { onNavigate(AppRoutes.WALLET) }, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)), modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(tr("الرصيد"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(ltr("${formatAmount(vm.wallet.balance)} USDT"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(tr("شحن الرصيد ›"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Surface(onClick = onOpenBuyNumber, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(tr("الأرقام"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(numbers.size.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(tr("نشط • شراء ›"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = successColor())
                }
            }
        }
        ActionRow(tr("الرقم الصادر (Caller ID)"), caller.ifBlank { tr("لا يوجد رقم نشط — اضغط للشراء") }, Icons.Filled.PhoneAndroid, {
            if (numbers.size > 1) onSelectCaller(numbers[(numbers.indexOfFirst { it.phoneNumber == caller } + 1) % numbers.size].phoneNumber)
            else if (numbers.isEmpty()) onOpenBuyNumber()
        })
        ActionRow(tr("المعاملات والفواتير"), tr("الشحن، الأرقام، المكالمات الصوتية، والرسائل"), Icons.Filled.ReceiptLong, { txDialogOpen = true })
        ActionRow(tr("حالة الاتصال"), if (caller.isNotBlank()) tr("متصل (Online)") else tr("جاهز للتفعيل"), Icons.Filled.WifiTethering, {})
        ActionRow(tr("ادعُ أصدقاءك واربح رصيداً"), tr("مشاركة كود الدعوة والمكافآت"), Icons.Filled.CardGiftcard, {
            val c = engagementVm?.myReferralCode.orEmpty()
            if (c.isNotBlank()) {
                val i = android.content.Intent(android.content.Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(android.content.Intent.EXTRA_TEXT, buildReferralShareMessage(c)) }
                runCatching { ctx.startActivity(android.content.Intent.createChooser(i, tr("مشاركة كود الدعوة"))) }
            } else onNavigate(AppRoutes.REFERRALS)
        })
        ActionRow(tr("الدعم الفني"), tr("تواصل مع فريق الدعم والمساعدة"), Icons.Filled.SupportAgent, { onNavigate(AppRoutes.SUPPORT) })
        ActionRow(tr("الإعدادات"), tr("اللغة، المظهر، والأمان"), Icons.Filled.Settings, { onNavigate(AppRoutes.SETTINGS) })
        if (isAdmin) ActionRow(tr("لوحة الإدارة"), tr("إدارة نسب الربح والطلبات والإيداعات"), Icons.Filled.AdminPanelSettings, { onNavigate(AppRoutes.ADMIN) }, highlighted = true)
        TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text(tr("تسجيل الخروج"), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
    }
}
