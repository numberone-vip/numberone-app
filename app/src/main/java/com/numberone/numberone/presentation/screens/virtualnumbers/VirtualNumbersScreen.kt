package com.numberone.numberone.presentation.screens.virtualnumbers

import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.core.localization.trf
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.numberone.numberone.core.components.CopyField
import com.numberone.numberone.core.components.EmptyState
import com.numberone.numberone.core.components.NOButton
import com.numberone.numberone.core.components.NOCard
import com.numberone.numberone.core.components.NOTopBar
import com.numberone.numberone.core.components.SectionTitle
import com.numberone.numberone.core.components.TopSnackbarHost
import com.numberone.numberone.core.components.dangerColor
import com.numberone.numberone.core.components.successColor
import com.numberone.numberone.core.components.warningColor
import com.numberone.numberone.core.util.parseInstantOrNull
import com.numberone.numberone.di.AppContainer
import com.numberone.numberone.domain.model.VirtualNumberCatalog
import com.numberone.numberone.domain.model.VirtualNumberCountry
import com.numberone.numberone.domain.model.VirtualNumberOperator
import com.numberone.numberone.domain.model.VirtualNumberOrder
import com.numberone.numberone.domain.model.VirtualNumberQuote
import com.numberone.numberone.domain.model.VirtualNumberService
import com.numberone.numberone.notifications.SmsNotifications
import com.numberone.numberone.presentation.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

private const val AUTO_OPERATOR = "auto"
private const val POLL_INTERVAL_MS = 5_000L
private val LIVE_STATUSES = setOf("PENDING", "WAITING", "ACTIVE", "RECEIVED")
private val CANCELLABLE_STATUSES = setOf("PENDING", "WAITING", "ACTIVE")

private fun isLive(status: String?): Boolean = status.orEmpty().uppercase() in LIVE_STATUSES

private fun isCancellable(status: String?): Boolean = status.orEmpty().uppercase() in CANCELLABLE_STATUSES

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualNumbersScreen(vm: AppViewModel, onBack: () -> Unit, onTopUp: () -> Unit, embedded: Boolean = false) {
    val repository = remember { AppContainer.virtualNumberRepository }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    var catalog by remember { mutableStateOf(VirtualNumberCatalog()) }
    var quote by remember { mutableStateOf<VirtualNumberQuote?>(null) }
    var order by remember { mutableStateOf<VirtualNumberOrder?>(null) }
    var selectedCountry by remember { mutableStateOf<VirtualNumberCountry?>(null) }
    var selectedService by remember { mutableStateOf<VirtualNumberService?>(null) }
    var selectedOperator by remember { mutableStateOf(AUTO_OPERATOR) }
    var pendingRequestId by remember { mutableStateOf<String?>(null) }
    var countrySheetOpen by remember { mutableStateOf(false) }
    var countrySearch by remember { mutableStateOf("") }
    var serviceSheetOpen by remember { mutableStateOf(false) }
    var serviceSearch by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }
    var catalogLoading by remember { mutableStateOf(true) }
    var quoteLoading by remember { mutableStateOf(false) }
    var actionLoading by remember { mutableStateOf(false) }
    var isResumed by remember { mutableStateOf(true) }

    // Snackbars are launched, never awaited inline: showSnackbar suspends until the message
    // is dismissed, and awaiting it would keep the screen's loading state on for seconds.
    fun say(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    // Stop polling 5SIM while the app is in the background; resume when it returns.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isResumed = true
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                isResumed = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        vm.loadWallet()
        catalogLoading = true
        repository.catalog()
            .onSuccess { result ->
                catalog = result
                selectedCountry = result.countries.firstOrNull()
                selectedService = result.services.firstOrNull { it.code.equals("whatsapp", ignoreCase = true) }
                    ?: result.services.firstOrNull()
            }
            .onFailure { say(it.message ?: tr("تعذر تحميل كتالوج الأرقام")) }
        catalogLoading = false
        // Recover the latest activation after navigating away or reopening the screen.
        repository.latestActivation().onSuccess { latest -> if (latest != null && isLive(latest.status)) order = latest }
    }

    LaunchedEffect(selectedCountry?.countryCode, selectedService?.code) {
        // A new country or service invalidates the quote and any idempotency key tied to it.
        pendingRequestId = null
        selectedOperator = AUTO_OPERATOR
        quote = null
        val country = selectedCountry
        val service = selectedService
        if (country == null || service == null) {
            quoteLoading = false
            return@LaunchedEffect
        }
        quoteLoading = true
        repository.quote(country.countryCode, service.code)
            .onSuccess { quote = it }
            .onFailure { say(it.message ?: tr("لا توجد تسعيرة متاحة لهذا الاختيار")) }
        quoteLoading = false
    }

    // Foreground polling of the live activation. 5SIM has no push; FCM covers the background.
    val activeId = order?.activationId
    val activeStatus = order?.status
    LaunchedEffect(activeId, activeStatus, isResumed) {
        val id = activeId ?: return@LaunchedEffect
        if (!isResumed || !isLive(activeStatus)) return@LaunchedEffect
        while (true) {
            delay(POLL_INTERVAL_MS)
            val fresh = repository.poll(id).getOrNull() ?: continue
            val previousCode = order?.smsCode
            order = fresh
            if (!fresh.smsCode.isNullOrBlank() && fresh.smsCode != previousCode) {
                SmsNotifications.show(
                    context,
                    SmsNotifications.notifyId(fresh.activationId),
                    tr("وصل رمز تفعيل جديد"),
                    trf("وصلت رسالة جديدة إلى الرقم {0}. افتح التطبيق لعرضها.", fresh.phone)
                )
                say(tr("وصل رمز تفعيل جديد"))
            }
            if (!isLive(fresh.status)) { if (fresh.status.uppercase() in setOf("CANCELLED", "CANCELED", "EXPIRED", "TIMEOUT")) order = null; break }
        }
    }

    fun runAction(
        action: suspend () -> Result<VirtualNumberOrder>,
        successMessage: String,
        afterSuccess: () -> Unit = {}
    ) {
        if (actionLoading) return
        scope.launch {
            actionLoading = true
            try {
                action()
                    .onSuccess { result ->
                        order = result
                        afterSuccess()
                        vm.loadWallet()
                        say(successMessage)
                    }
                    .onFailure { say(it.message ?: tr("تعذر تنفيذ العملية")) }
            } finally {
                actionLoading = false
            }
        }
    }

    val currentQuote = quote
    val availableOperators = currentQuote?.operators.orEmpty().filter { it.availableQty > 0 }
    val bestOperator = availableOperators.maxByOrNull { it.successRate }
    val chosenOperator = if (selectedOperator == AUTO_OPERATOR) {
        bestOperator
    } else {
        availableOperators.firstOrNull { it.name == selectedOperator }
    }

    val query = countrySearch.trim()
    val filteredCountries = remember(catalog, query) {
        catalog.countries.filter { country ->
            query.isEmpty() ||
                country.name.contains(query, ignoreCase = true) ||
                country.dialCode.contains(query) ||
                country.iso.contains(query, ignoreCase = true) ||
                country.countryCode.contains(query, ignoreCase = true)
        }
    }
    val sQuery = serviceSearch.trim()
    val filteredServices = remember(catalog, sQuery) { catalog.services.sortedBy { it.name.lowercase() }.filter { s -> sQuery.isEmpty() || s.name.contains(sQuery, ignoreCase = true) || s.code.contains(sQuery, ignoreCase = true) } }

    val activeOrder = order
    val canBuy = currentQuote != null && chosenOperator != null && selectedCountry != null &&
        selectedService != null && !quoteLoading && !actionLoading
    val finalPrice = if (currentQuote != null && chosenOperator != null) {
        currentQuote.finalPrice(chosenOperator)
    } else {
        null
    }
    val insufficient = finalPrice != null && vm.wallet.balance < finalPrice

    // Notifications float over the content and slide in from the top (never over the bottom tabs).
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { if (!embedded) NOTopBar(title = tr("الأرقام الافتراضية"), onBack = onBack) },
            // Inside the main tabs the shell already handles the system bars.
            contentWindowInsets = if (embedded) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets
        ) { padding ->
            PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = { scope.launch { isRefreshing = true; vm.loadWallet(); repository.catalog().onSuccess { res -> catalog = res }; val c = selectedCountry; val s = selectedService; if (c != null && s != null) repository.quote(c.countryCode, s.code).onSuccess { quote = it }; repository.latestActivation().onSuccess { if (it != null) order = it }; isRefreshing = false } }, modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (embedded) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(tr("أرقام فورية"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                            Text(
                                tr("اختر الخدمة والدولة، فيصلك الرقم والرمز تلقائياً خلال ثوانٍ."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    BalanceHeader(balance = vm.wallet.balance, currency = vm.wallet.currency)
                }

                if (catalog.services.isNotEmpty()) { item { Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("whatsapp" to "واتساب","telegram" to "تيليجرام","facebook" to "فيسبوك","google" to "غوغل","tiktok" to "تيك توك").forEach { (c, l) -> catalog.services.firstOrNull { it.code.equals(c, true) || c in it.name.lowercase() }?.let { m -> FilterChip(selected = selectedService?.code == m.code, onClick = { selectedService = m }, label = { Text(tr(l), fontWeight = FontWeight.Bold) }) } } } } }
                item { CountryPicker(country = selectedCountry, onClick = { countrySearch = ""; countrySheetOpen = true }) }
                item { ServicePicker(service = selectedService, totalCount = catalog.services.size, onClick = { serviceSearch = ""; serviceSheetOpen = true }) }

                item {
                    OperatorPicker(
                        loading = catalogLoading || quoteLoading,
                        quoteAvailable = currentQuote != null,
                        available = availableOperators,
                        selected = selectedOperator,
                        chosen = chosenOperator,
                        onSelect = { selectedOperator = it; pendingRequestId = null }
                    )
                }

                if (currentQuote != null && chosenOperator != null && finalPrice != null) {
                    item {
                        PriceSummary(
                            basePrice = chosenOperator.basePrice,
                            finalPrice = finalPrice,
                            balance = vm.wallet.balance,
                            currency = currentQuote.currency
                        )
                    }
                }

                if (insufficient) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                tr("رصيدك الحالي لا يكفي لهذا الشراء."),
                                color = dangerColor(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            OutlinedButton(
                                onClick = onTopUp,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(tr("شحن المحفظة"), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    NOButton(
                        text = tr("شراء رقم الآن"),
                        onClick = {
                            val country = selectedCountry
                            val service = selectedService
                            val operator = chosenOperator
                            if (country != null && service != null && operator != null) {
                                runAction(
                                    action = {
                                        val requestId = pendingRequestId
                                            ?: UUID.randomUUID().toString().also { pendingRequestId = it }
                                        repository.purchase(
                                            countryCode = country.countryCode,
                                            serviceCode = service.code,
                                            operator = if (selectedOperator == AUTO_OPERATOR) AUTO_OPERATOR else operator.name,
                                            requestId = requestId
                                        )
                                    },
                                    successMessage = tr("تم شراء الرقم وخصم المبلغ من المحفظة"),
                                    afterSuccess = { pendingRequestId = null }
                                )
                            }
                            // On failure the same request UUID is kept so a retry cannot double-charge.
                        },
                        enabled = canBuy && !insufficient,
                        loading = actionLoading
                    )
                }

                if (activeOrder != null) {
                    item {
                        ActivationCard(
                            order = activeOrder,
                            busy = actionLoading,
                            onRefresh = {
                                runAction(
                                    action = { repository.requestNextSms(activeOrder.activationId) },
                                    successMessage = tr("تم تحديث الرسائل الواردة")
                                )
                            },
                            onCancel = {
                                runAction(
                                    action = { repository.cancel(activeOrder.activationId) },
                                    successMessage = tr("تم إلغاء الرقم وتحديث الاسترداد إن وُجد"),
                                    afterSuccess = { order = null }
                                )
                            },
                            onDismiss = { order = null }
                        )
                    }
                }

                item {
                    Text(
                        tr("السعر النهائي يُحسب ويُتحقق منه على الخادم لحظة الشراء. أرسل طلب التحقق من التطبيق الذي تسجّل فيه، ثم حدّث الرسائل هنا."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
            }
        TopSnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .then(if (embedded) Modifier else Modifier.statusBarsPadding())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }

    if (countrySheetOpen) {
        ModalBottomSheet(
            onDismissRequest = {
                countrySheetOpen = false
                countrySearch = ""
            }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(tr("اختر الدولة"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = countrySearch,
                    onValueChange = { countrySearch = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(tr("ابحث بالاسم أو رمز الاتصال")) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    shape = RoundedCornerShape(16.dp)
                )
                if (filteredCountries.isEmpty()) {
                    EmptyState(tr("لا توجد نتائج مطابقة"))
                }
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
                    items(filteredCountries, key = { it.countryCode }) { country ->
                        val isSelected = country.countryCode == selectedCountry?.countryCode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCountry = country
                                    countrySheetOpen = false
                                    countrySearch = ""
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(flagEmoji(country.iso), style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    country.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (country.dialCode.isNotBlank()) {
                                    Text(
                                        country.dialCode,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (serviceSheetOpen) {
        ModalBottomSheet(onDismissRequest = { serviceSheetOpen = false; serviceSearch = "" }) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("اختر الخدمة"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("${filteredServices.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                OutlinedTextField(value = serviceSearch, onValueChange = { serviceSearch = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(tr("ابحث باسم التطبيق أو الخدمة")) }, leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }, shape = RoundedCornerShape(16.dp))
                if (filteredServices.isEmpty()) EmptyState(tr("لا توجد خدمات مطابقة"))
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
                    items(filteredServices, key = { it.code }) { service ->
                        val isSelected = service.code == selectedService?.code
                        Row(modifier = Modifier.fillMaxWidth().clickable { selectedService = service; serviceSheetOpen = false; serviceSearch = "" }.padding(vertical = 12.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(service.name, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                Text(service.code, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (isSelected) Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceHeader(balance: Double, currency: String) {
    NOCard {
        Text(
            tr("أرقام تفعيل مؤقتة"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black
        )
        Text(
            tr("استقبل رموز التحقق على رقم مؤقت. يُخصم السعر من رصيد محفظتك فقط عند الشراء."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("الرصيد المتاح"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text(
                "%.2f %s".format(balance, currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ServicePicker(service: VirtualNumberService?, totalCount: Int, onClick: () -> Unit) {
    NOCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tr("الخدمة"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(service?.name ?: tr("اختر الخدمة"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(if (totalCount > 0) "${tr("عرض الكل وبحث")} ($totalCount)" else "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CountryPicker(country: VirtualNumberCountry?, onClick: () -> Unit) {
    NOCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                flagEmoji(country?.iso.orEmpty()),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tr("الدولة"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    country?.name ?: tr("اختر الدولة"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (country != null && country.dialCode.isNotBlank()) {
                    Text(
                        country.dialCode,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = tr("تغيير الدولة"),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun OperatorPicker(
    loading: Boolean,
    quoteAvailable: Boolean,
    available: List<VirtualNumberOperator>,
    selected: String,
    chosen: VirtualNumberOperator?,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(title = tr("المشغّل"), subtitle = tr("التلقائي يختار أعلى نسبة تسليم متاحة"))
        when {
            loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            !quoteAvailable -> Text(
                tr("اختر الدولة والخدمة لعرض الأسعار المتاحة."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            available.isEmpty() -> Text(
                tr("لا يوجد مخزون لهذا الاختيار حالياً."),
                style = MaterialTheme.typography.bodyMedium,
                color = dangerColor()
            )
            else -> Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(operatorLabel(selected, chosen), fontWeight = FontWeight.SemiBold)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(tr("تلقائي • أعلى نسبة تسليم")) },
                        onClick = {
                            onSelect(AUTO_OPERATOR)
                            expanded = false
                        }
                    )
                    available.sortedByDescending { it.successRate }.forEach { operator ->
                        val rate = "%.1f".format(operator.successRate)
                        DropdownMenuItem(
                            text = { Text(trf("{0} • {1}% • {2} متاح", operator.name, rate, operator.availableQty)) },
                            onClick = {
                                onSelect(operator.name)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceSummary(
    basePrice: Double,
    finalPrice: Double,
    balance: Double,
    currency: String
) {
    val remaining = balance - finalPrice
    NOCard {
        SectionTitle(title = tr("ملخص السعر"))
        PriceRow(label = tr("السعر الأساسي"), value = "%.4f %s".format(basePrice, currency))
        PriceRow(label = tr("سعرك"), value = "%.4f %s".format(finalPrice, currency), emphasized = true)
        PriceRow(
            label = tr("الرصيد بعد الشراء"),
            value = "%.2f %s".format(remaining, currency),
            warning = remaining < 0
        )
    }
}

@Composable
private fun PriceRow(
    label: String,
    value: String,
    emphasized: Boolean = false,
    warning: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.SemiBold,
            color = when {
                warning -> dangerColor()
                emphasized -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun ActivationCard(
    order: VirtualNumberOrder,
    busy: Boolean,
    onRefresh: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    val code = order.smsCode?.takeIf { it.isNotBlank() }
    NOCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                tr("رقمك المؤقت"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            VirtualNumberStatusChip(order.status)
        }

        CopyField(label = tr("رقم الهاتف"), value = order.phone)

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    tr("رمز التحقق"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (code == null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(
                            if (isLive(order.status)) tr("بانتظار وصول الرسالة…") else tr("لم تصل أي رسالة لهذا الرقم"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                } else {
                    Text(
                        code,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 6.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        if (code != null) {
            CopyField(label = tr("نسخ الرمز"), value = code)
        }

        order.smsText?.takeIf { it.isNotBlank() }?.let { text ->
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isLive(order.status)) {
            ExpiryCountdown(expiresAt = order.expiresAt)
        }

        Text(
            tr("المبلغ المخصوم: %.4f USDT").format(order.chargedPrice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onRefresh,
                enabled = !busy && isLive(order.status),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(tr("تحديث الرسائل"), fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = onCancel,
                enabled = !busy && isCancellable(order.status),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = dangerColor()
                )
            ) {
                Text(tr("إلغاء الرقم"), fontWeight = FontWeight.SemiBold)
            }
        }

        if (!isLive(order.status)) {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(tr("إخفاء هذا الطلب"))
            }
        }
    }
}

@Composable
private fun ExpiryCountdown(expiresAt: String?) {
    val end = remember(expiresAt) { parseInstantOrNull(expiresAt) } ?: return
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(end) {
        while (true) {
            now = System.currentTimeMillis()
            if (now >= end.toEpochMilli()) break
            delay(1_000L)
        }
    }
    val remainingSeconds = ((end.toEpochMilli() - now) / 1000L).coerceAtLeast(0L)
    val expired = remainingSeconds == 0L
    Text(
        text = if (expired) {
            tr("انتهت مدة صلاحية الرقم")
        } else {
            tr("ينتهي خلال %02d:%02d").format(remainingSeconds / 60, remainingSeconds % 60)
        },
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = if (expired) dangerColor() else MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun VirtualNumberStatusChip(status: String) {
    val (label, color) = when (status.uppercase()) {
        "PENDING", "WAITING" -> tr("بانتظار الرسالة") to warningColor()
        "RECEIVED" -> tr("وصلت الرسالة") to successColor()
        "FINISHED" -> tr("مكتمل") to successColor()
        "CANCELED", "CANCELLED" -> tr("ملغى") to dangerColor()
        "TIMEOUT", "EXPIRED", "BANNED" -> tr("منتهي") to dangerColor()
        else -> status to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
        contentColor = color
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun operatorLabel(selected: String, chosen: VirtualNumberOperator?): String {
    return if (selected == AUTO_OPERATOR) {
        if (chosen == null) tr("تلقائي") else trf("تلقائي • {0}", chosen.name)
    } else {
        chosen?.name ?: tr("اختر المشغّل")
    }
}

/** Turns an ISO 3166 alpha-2 code such as "ca" into its flag emoji. Falls back to a globe. */
private fun flagEmoji(iso: String): String {
    val letters = iso.uppercase().filter { it in 'A'..'Z' }
    if (letters.length != 2) return "\uD83C\uDF0D"
    return letters
        .map { letter -> String(Character.toChars(0x1F1E6 + (letter - 'A'))) }
        .joinToString("")
}
