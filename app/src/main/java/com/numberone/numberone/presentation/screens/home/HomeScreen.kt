package com.numberone.numberone.presentation.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.numberone.numberone.core.components.*
import com.numberone.numberone.core.localization.*
import com.numberone.numberone.core.theme.NumberOneColors
import com.numberone.numberone.data.remote.*
import com.numberone.numberone.notifications.PushTokenRegistrar
import com.numberone.numberone.presentation.navigation.AppRoutes
import com.numberone.numberone.presentation.screens.referrals.buildReferralShareMessage
import com.numberone.numberone.presentation.viewmodel.AppViewModel
import com.numberone.numberone.presentation.viewmodel.EngagementViewModel
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val TAB_HOME = 0
private const val TAB_CALLS = 1
private const val TAB_MESSAGES = 2
private const val TAB_ACCOUNT = 3
private const val TAB_COUNT = 4
private val TAB_BAR_HEIGHT = 72.dp
private val PILL_WIDTH = 56.dp
private val PILL_HEIGHT = 30.dp
private val PILL_TOP = 8.dp
private data class MainTab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector)
private fun mainTabs() = listOf(
    MainTab(tr("الرئيسية"), Icons.Outlined.Home, Icons.Filled.Home),
    MainTab(tr("المكالمات"), Icons.Outlined.Dialpad, Icons.Filled.Dialpad),
    MainTab(tr("الرسائل"), Icons.AutoMirrored.Outlined.Message, Icons.AutoMirrored.Filled.Message),
    MainTab(tr("حسابي"), Icons.Outlined.Person, Icons.Filled.Person)
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(vm: AppViewModel, engagementVm: EngagementViewModel? = null, userEmail: String?, isAdmin: Boolean, onNavigate: (String) -> Unit, onLogout: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var savedTab by rememberSaveable { mutableIntStateOf(TAB_HOME) }
    val pagerState = rememberPagerState(initialPage = savedTab, pageCount = { TAB_COUNT })
    var telnyxOffer by remember { mutableStateOf<TelnyxOffer?>(null) }
    var telnyxActivity by remember { mutableStateOf<TelnyxUserActivity?>(null) }
    var selectedCaller by rememberSaveable { mutableStateOf("") }
    var addNumberOpen by remember { mutableStateOf(false) }
    var composeRecipient by remember { mutableStateOf<String?>(null) }

    fun refreshTelnyx() {
        scope.launch {
            runCatching { TelnyxClient.getOffer() }.onSuccess {
                telnyxOffer = it
                if (selectedCaller.isBlank() || it.numbers.none { n -> n.phoneNumber == selectedCaller }) selectedCaller = it.numbers.firstOrNull()?.phoneNumber.orEmpty()
            }
            runCatching { TelnyxClient.getUserActivity() }.onSuccess { telnyxActivity = it }
        }
    }
    LaunchedEffect(Unit) { PushTokenRegistrar.registerCurrent(context); vm.loadDashboard(); engagementVm?.refreshUnreadCount(); engagementVm?.loadMyReferralCode(); refreshTelnyx() }
    LaunchedEffect(pagerState) { snapshotFlow { pagerState.settledPage }.collect { savedTab = it } }
    LaunchedEffect(pagerState) { snapshotFlow { pagerState.settledPage }.drop(1).collect { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } }
    fun goTo(page: Int) { scope.launch { pagerState.animateScrollToPage(page) } }
    BackHandler(enabled = pagerState.currentPage != TAB_HOME) { goTo(TAB_HOME) }

    if (addNumberOpen) TelnyxAddNumberDialog(onDismiss = { addNumberOpen = false }, onPurchased = { num -> selectedCaller = num; addNumberOpen = false; vm.loadDashboard(); refreshTelnyx() })

    Scaffold(containerColor = MaterialTheme.colorScheme.background, bottomBar = { MainBottomBar(pagerState = pagerState, onSelect = { goTo(it) }) }) { padding ->
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize().padding(padding), beyondViewportPageCount = 1) { page ->
            when (page) {
                TAB_HOME -> DashboardTab(vm, userEmail, telnyxOffer, selectedCaller, engagementVm?.unreadCount ?: 0, { addNumberOpen = true }, { selectedCaller = it; goTo(TAB_CALLS) }, onNavigate) { vm.loadDashboard(); refreshTelnyx() }
                TAB_CALLS -> CallsTab(telnyxOffer, telnyxActivity, selectedCaller, { selectedCaller = it }, { addNumberOpen = true }, { to -> composeRecipient = to; goTo(TAB_MESSAGES) }) { vm.loadDashboard(); refreshTelnyx() }
                TAB_MESSAGES -> MessagesTab(telnyxOffer, telnyxActivity, composeRecipient, { composeRecipient = null }, { onNavigate(AppRoutes.SMS_INBOX) }) { vm.loadDashboard(); refreshTelnyx() }
                else -> AccountTab(vm, engagementVm, userEmail, isAdmin, telnyxOffer, telnyxActivity, selectedCaller, { selectedCaller = it }, { addNumberOpen = true }, onNavigate, onLogout)
            }
        }
    }
}

@Composable
private fun MainBottomBar(pagerState: PagerState, onSelect: (Int) -> Unit) {
    val tabs = mainTabs()
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Box(modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(TAB_BAR_HEIGHT)) {
                    val slot = maxWidth / tabs.size
                    Box(modifier = Modifier.offset { val slotPx = slot.toPx(); val pos = pagerState.currentPage + pagerState.currentPageOffsetFraction; IntOffset(x = (pos * slotPx + (slotPx - PILL_WIDTH.toPx()) / 2f).roundToInt(), y = PILL_TOP.roundToPx()) }.size(width = PILL_WIDTH, height = PILL_HEIGHT).clip(CircleShape).background(Brush.linearGradient(listOf(NumberOneColors.GoldLight, NumberOneColors.GoldMid, NumberOneColors.GoldDeep))))
                    Row(modifier = Modifier.fillMaxSize()) {
                        tabs.forEachIndexed { index, item ->
                            Column(modifier = Modifier.weight(1f).fillMaxHeight().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Tab, onClick = { onSelect(index) }).semantics { this.selected = pagerState.currentPage == index }.padding(top = PILL_TOP), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(modifier = Modifier.size(width = PILL_WIDTH, height = PILL_HEIGHT), contentAlignment = Alignment.Center) { Icon(if (pagerState.currentPage == index) item.selectedIcon else item.icon, null, tint = if (pagerState.currentPage == index) NumberOneColors.Obsidian else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp)) }
                                Spacer(Modifier.height(4.dp))
                                Text(item.label, style = MaterialTheme.typography.labelSmall, fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Medium, color = if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTab(vm: AppViewModel, userEmail: String?, offer: TelnyxOffer?, selectedCaller: String, unreadNotifications: Int, onOpenAddNumber: () -> Unit, onSelectCaller: (String) -> Unit, onNavigate: (String) -> Unit, onRefresh: () -> Unit) {
    val clip = LocalClipboardManager.current
    val ctx = LocalContext.current
    val uid = remember { SupabaseClientProvider.client.auth.currentUserOrNull()?.id.orEmpty() }
    val name = userEmail?.substringBefore("@")?.takeIf { it.isNotBlank() } ?: tr("عميلنا")

    PullToRefreshBox(isRefreshing = vm.loading, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("NUMBER ONE", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                BadgedBox(badge = { if (unreadNotifications > 0) Badge { Text(unreadNotifications.toString()) } }) { IconButton(onClick = { onNavigate(AppRoutes.NOTIFICATIONS) }) { Icon(Icons.Filled.Notifications, null, tint = MaterialTheme.colorScheme.primary) } }
            }
            Text(trf("مرحباً، {0}", name), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(tr("الرصيد المتاح"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        if (uid.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.clickable { clip.setText(AnnotatedString(uid)); android.widget.Toast.makeText(ctx, tr("تم نسخ معرّف حسابك"), android.widget.Toast.LENGTH_SHORT).show() }) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(successColor()))
                            Text(ltr("ID: ${uid.take(8)}"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(ltr("${formatAmount(vm.wallet.balance)} ${vm.wallet.currency}"), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                        Button(onClick = { onNavigate(AppRoutes.WALLET) }, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = NumberOneColors.GoldMid, contentColor = NumberOneColors.Obsidian)) { Text(tr("شحن الرصيد"), fontWeight = FontWeight.Black) }
                    }
                    if (vm.wallet.balance < 1.0) Text(tr("الرصيد منخفض — اشحن محفظتك لمواصلة المكالمات وشراء الأرقام"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Surface(onClick = onOpenAddNumber, shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(modifier = Modifier.size(46.dp).clip(CircleShape).background(NumberOneColors.GoldMid), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Add, null, tint = NumberOneColors.Obsidian, modifier = Modifier.size(26.dp)) }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(tr("احصل على رقم أعمال جديد"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        Text(tr("US • Canada • UK • Australia • مكالمات + SMS"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            val myNums = offer?.numbers.orEmpty()
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(tr("أرقامي النشطة"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                TextButton(onClick = onOpenAddNumber) { Text(tr("إضافة رقم"), fontWeight = FontWeight.Bold) }
            }
            if (myNums.isEmpty()) NOCard { Text(tr("لا تملك رقماً سحابياً نشطاً بعد. اضغط «احصل على رقم أعمال جديد» لاختيار رقمك المفضل."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            else myNums.forEach { n ->
                val isCaller = (selectedCaller.ifBlank { myNums.first().phoneNumber }) == n.phoneNumber
                Surface(onClick = { onSelectCaller(n.phoneNumber) }, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) { Text("US", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary) }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(ltr(n.phoneNumber), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                            n.expiresAt?.let { Text(tr("يتجدد في ") + ltr(it.take(10)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary.copy(alpha = if (isCaller) 0.18f else 0.08f)) { Text(if (isCaller) "Caller ID ›" else tr("اتصال ›"), modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
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
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AccountTab(vm: AppViewModel, engagementVm: EngagementViewModel?, userEmail: String?, isAdmin: Boolean, offer: TelnyxOffer?, activity: TelnyxUserActivity?, selectedCaller: String, onSelectCaller: (String) -> Unit, onOpenBuyNumber: () -> Unit, onNavigate: (String) -> Unit, onLogout: () -> Unit) {
    val clip = LocalClipboardManager.current
    val ctx = LocalContext.current
    val uid = remember { SupabaseClientProvider.client.auth.currentUserOrNull()?.id.orEmpty() }
    val name = userEmail?.substringBefore("@")?.takeIf { it.isNotBlank() } ?: tr("عميلنا")
    val numbers = offer?.numbers.orEmpty()
    val caller = selectedCaller.ifBlank { numbers.firstOrNull()?.phoneNumber.orEmpty() }
    var txDialogOpen by remember { mutableStateOf(false) }
    var adminConfirmed by remember(isAdmin) { mutableStateOf(isAdmin) }
    LaunchedEffect(Unit) { if (!adminConfirmed) runCatching { com.numberone.numberone.core.di.AppContainer.userRepository.isAdmin().getOrDefault(false) }.onSuccess { if (it) adminConfirmed = true } }

    if (txDialogOpen) AlertDialog(
        onDismissRequest = { txDialogOpen = false }, title = { Text(tr("سجل المعاملات والفواتير"), fontWeight = FontWeight.Black) },
        text = { Column(modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val txs = activity?.transactions.orEmpty()
            if (txs.isEmpty()) Text(tr("لا توجد معاملات مسجلة بعد."), style = MaterialTheme.typography.bodySmall)
            txs.forEach { t ->
                val title = when (t.referenceType) { "telnyx_voice" -> tr("مكالمة صوتية دولية"); "telnyx_sms" -> tr("رسالة SMS صادرة"); "telnyx_purchase" -> tr("شراء رقم سحابي"); else -> t.description.ifBlank { tr("معاملة مالية") } }
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text(ltr("${if (t.direction == "debit") "-" else "+"}${formatAmount(t.amount)} USDT"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = if (t.direction == "debit") dangerColor() else successColor()) }
                        if (t.description.isNotBlank()) Text(t.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(t.createdAt.take(16).replace("T", " "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } }, confirmButton = { TextButton(onClick = { txDialogOpen = false }) { Text(tr("إغلاق")) } }
    )

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(tr("حسابي"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) { Text(name.take(2).uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary) }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    if (!userEmail.isNullOrBlank()) Text(userEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (uid.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.clickable { clip.setText(AnnotatedString(uid)); android.widget.Toast.makeText(ctx, tr("تم نسخ معرّف الحساب"), android.widget.Toast.LENGTH_SHORT).show() }) {
                        Text(ltr("ID: ${uid.take(8)}…"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Icon(Icons.Filled.ContentCopy, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Surface(onClick = { onNavigate(AppRoutes.WALLET) }, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)), modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(tr("الرصيد"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(ltr("${formatAmount(vm.wallet.balance)} USDT"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black); Text(tr("شحن الرصيد ›"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
            }
            Surface(onClick = onOpenBuyNumber, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(tr("الأرقام"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(numbers.size.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black); Text(tr("نشط • شراء ›"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = successColor()) }
            }
        }
        ActionRow(tr("الرقم الصادر (Caller ID)"), caller.ifBlank { tr("لا يوجد رقم نشط — اضغط للشراء") }, Icons.Filled.PhoneAndroid, {
            if (numbers.size > 1) onSelectCaller(numbers[(numbers.indexOfFirst { item -> item.phoneNumber == caller } + 1) % numbers.size].phoneNumber)
            else if (numbers.isEmpty()) onOpenBuyNumber()
        })
        ActionRow(tr("المعاملات والفواتير"), tr("الشحن، الأرقام، المكالمات الصوتية، والرسائل"), Icons.Filled.ReceiptLong, { txDialogOpen = true })
        ActionRow(tr("حالة الاتصال"), if (caller.isNotBlank()) tr("متصل (Online)") else tr("جاهز للتفعيل"), Icons.Filled.CheckCircle, {})
        ActionRow(tr("ادعُ أصدقاءك واربح رصيداً"), tr("مشاركة كود الدعوة والمكافآت"), Icons.Filled.CardGiftcard, {
            val c = engagementVm?.myReferralCode.orEmpty()
            if (c.isNotBlank()) { val i = android.content.Intent(android.content.Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(android.content.Intent.EXTRA_TEXT, buildReferralShareMessage(c)) }; runCatching { ctx.startActivity(android.content.Intent.createChooser(i, tr("مشاركة كود الدعوة"))) } } else onNavigate(AppRoutes.REFERRALS)
        })
        ActionRow(tr("الدعم الفني"), tr("تواصل مع فريق الدعم والمساعدة"), Icons.Filled.SupportAgent, { onNavigate(AppRoutes.SUPPORT) })
        ActionRow(tr("الإعدادات"), tr("اللغة، المظهر، والأمان"), Icons.Filled.Settings, { onNavigate(AppRoutes.SETTINGS) })
        if (adminConfirmed) ActionRow(tr("لوحة الإدارة"), tr("إدارة نسب الربح والطلبات والإيداعات"), Icons.Filled.AdminPanelSettings, { onNavigate(AppRoutes.ADMIN) }, highlighted = true)
        TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text(tr("تسجيل الخروج"), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
    }
}
