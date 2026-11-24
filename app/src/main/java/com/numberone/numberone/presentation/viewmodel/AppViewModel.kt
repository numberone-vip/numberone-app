package com.numberone.numberone.presentation.viewmodel

import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.core.localization.trf
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.numberone.numberone.core.error.ErrorMapper
import com.numberone.numberone.di.AppContainer
import com.numberone.numberone.domain.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class AppViewModel : ViewModel() {
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var message by mutableStateOf<String?>(null); private set
    var wallet by mutableStateOf(Wallet()); private set
    var transactions by mutableStateOf<List<WalletTransaction>>(emptyList()); private set
    var deposits by mutableStateOf<List<WalletTopUp>>(emptyList()); private set
    var services by mutableStateOf<List<Service>>(emptyList()); private set
    var hexzCatalog by mutableStateOf<List<HexzCatalogItem>>(emptyList()); private set
    var hexzCatalogLoaded by mutableStateOf(false); private set
    var hexzSandboxChecked by mutableStateOf(false); private set
    var hexzSandboxBalance by mutableStateOf<String?>(null); private set
    var subscriptions by mutableStateOf<List<Subscription>>(emptyList()); private set
    var sms by mutableStateOf<List<SmsMessage>>(emptyList()); private set
    /** How many messages the user had seen when the inbox was last open. */
    var smsSeenCount by mutableStateOf(0); private set
    val unreadSmsCount: Int get() = (sms.size - smsSeenCount).coerceAtLeast(0)
    fun markSmsSeen() { smsSeenCount = sms.size }
    var history by mutableStateOf<List<Order>>(emptyList()); private set
    var selectedDeposit by mutableStateOf(""); private set
    var pendingOrder by mutableStateOf<PaymentOrderCreation?>(null); private set
    var shamTopUps by mutableStateOf<List<ShamCashTopUp>>(emptyList()); private set
    var shamSelected by mutableStateOf(""); private set

    // Input fields keep private backing properties. Public accessors are plain getters and
    // the setters are explicit functions, so no JVM setX() declaration can clash.
    private var _accessCode by mutableStateOf("")
    private var _amount by mutableStateOf("")
    private var _txHash by mutableStateOf("")
    private var _shamMethod by mutableStateOf("SHAM_CASH_SYP")
    private var _shamAmount by mutableStateOf("")

    val accessCode: String get() = _accessCode
    val amount: String get() = _amount
    val txHash: String get() = _txHash
    val shamMethod: String get() = _shamMethod
    val shamAmount: String get() = _shamAmount

    /** Idempotency keys for wallet purchases: reused on retry so a lost response can never double-charge. */
    private val walletRequestIds = mutableMapOf<String, String>()
    private val txHashRegex = Regex("^(0[xX])?[0-9a-fA-F]{64}$")

    fun fail(msg: String) { error = msg; message = null }

    /** Wipes every piece of per-user state (called on sign-out so the next account never sees it). */
    private fun clearSessionState() {
        _shamMethod = "SHAM_CASH_SYP"; _shamAmount = ""; shamTopUps = emptyList(); shamSelected = ""
        wallet = Wallet(); transactions = emptyList(); deposits = emptyList(); services = emptyList()
        hexzCatalog = emptyList(); hexzCatalogLoaded = false; hexzSandboxChecked = false; hexzSandboxBalance = null
        subscriptions = emptyList(); sms = emptyList(); smsSeenCount = 0; history = emptyList()
        _accessCode = ""; _amount = ""; _txHash = ""; selectedDeposit = ""; pendingOrder = null
        walletRequestIds.clear(); error = null; message = null
    }

    fun setAccessCode(v: String) { _accessCode = v }
    fun setAmount(v: String) { _amount = v }
    fun setShamMethod(v: String) { _shamMethod = v }
    fun setShamAmount(v: String) { _shamAmount = v }
    fun selectSham(v: String) { shamSelected = v }
    fun setTxHash(v: String) { _txHash = v }
    fun selectDeposit(v: String) { selectedDeposit = v }

    private fun run(task: suspend () -> Unit) {
        if (loading) return
        viewModelScope.launch {
            loading = true; error = null; message = null
            try {
                task()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = ErrorMapper.toUserMessage(e)
            } finally {
                loading = false
            }
        }
    }

    fun refresh() {
        run {
            val repo = AppContainer.walletRepository
            wallet = repo.getWallet().getOrThrow()
            transactions = repo.getTransactions().getOrThrow()
            deposits = repo.getMyTopUpRequests().getOrThrow()
            services = repo.getServices().getOrThrow()
            history = repo.getMyOrders().getOrThrow()
        }
    }

    fun loadWallet() {
        run {
            val repo = AppContainer.walletRepository
            wallet = repo.getWallet().getOrThrow()
            transactions = repo.getTransactions().getOrThrow()
            deposits = repo.getMyTopUpRequests().getOrThrow()
            shamTopUps = repo.getMyShamCashTopUps().getOrThrow()
        }
    }

    /** Everything the main dashboard needs in one request (wallet, history and purchased numbers). */
    fun loadDashboard() {
        run {
            val repo = AppContainer.walletRepository
            wallet = repo.getWallet().getOrThrow()
            transactions = repo.getTransactions().getOrThrow()
            deposits = repo.getMyTopUpRequests().getOrThrow()
            shamTopUps = repo.getMyShamCashTopUps().getOrThrow()
            history = repo.getMyOrders().getOrThrow()
        }
    }

    fun loadStore() { run { services = AppContainer.walletRepository.getServices().getOrThrow() } }

    fun checkHexzSandbox() {
        run {
            hexzSandboxBalance = com.numberone.numberone.data.remote.HexzCatalogDataSource().checkSandboxBalance()
            hexzSandboxChecked = true
            message = tr("اتصال Hexz Sandbox ناجح")
        }
    }

    /** Loads the Hexz catalog only; this deliberately does not create orders or debit wallet funds. */
    fun loadHexzCatalog() {
        if (hexzCatalogLoaded || loading) return
        run {
            hexzCatalog = com.numberone.numberone.data.remote.HexzCatalogDataSource().getCatalog()
            hexzCatalogLoaded = true
        }
    }

    fun topUp() {
        val value = amount.trim().toDoubleOrNull()
        if (value == null || !value.isFinite() || value <= 0 || value > 100000) {
            error = tr("أدخل مبلغاً صحيحاً (حتى 100000 USDT)"); return
        }
        run {
            val req = AppContainer.walletRepository.createTopUpRequest(com.numberone.numberone.domain.model.CreateWalletTopUp(value)).getOrThrow()
            selectedDeposit = req.id
            deposits = AppContainer.walletRepository.getMyTopUpRequests().getOrThrow()
            message = tr("أرسل المبلغ الظاهر بالضبط عبر شبكة BEP20 ثم اضغط تحقق من الإيداع.")
        }
    }

    /** Automatic matching: the server finds the Binance deposit by unique amount, so no TXID is needed. */
    fun checkDeposit() {
        val id = selectedDeposit
        if (id.isBlank()) { error = tr("اختر طلب إيداع أولاً"); return }
        run {
            val response = AppContainer.walletRepository.verifyDeposit(id, "").getOrThrow()
            message = response
            deposits = AppContainer.walletRepository.getMyTopUpRequests().getOrThrow()
            wallet = AppContainer.walletRepository.getWallet().getOrThrow()
        }
    }

    fun submitDeposit() {
        val id = selectedDeposit
        if (id.isBlank()) { error = tr("اختر طلب إيداع أولاً"); return }
        if (!txHashRegex.matches(txHash.trim())) { error = tr("أدخل رقم معاملة صالحاً بطول 64 خانة"); return }
        run {
            AppContainer.walletRepository.submitDepositTransaction(id, txHash).getOrThrow()
            val response = AppContainer.walletRepository.verifyDeposit(id, txHash).getOrThrow()
            message = response
            deposits = AppContainer.walletRepository.getMyTopUpRequests().getOrThrow()
            wallet = AppContainer.walletRepository.getWallet().getOrThrow()
        }
    }

    fun createShamTopUp() {
        val v = shamAmount.trim().toDoubleOrNull()
        if (v == null || !v.isFinite() || v <= 0) { error = tr("أدخل مبلغاً صحيحاً"); return }
        val method = shamMethod
        run {
            val repo = AppContainer.walletRepository
            val id = repo.createShamCashTopUp(method, v).getOrThrow()
            shamSelected = id
            _shamAmount = ""
            shamTopUps = repo.getMyShamCashTopUps().getOrThrow()
            message = tr("تم إنشاء الطلب. حوّل المبلغ عبر شام كاش ثم ارفع صورة الإيصال لهذا الطلب.")
        }
    }

    fun submitShamReceipt(bytes: ByteArray, mimeType: String, extension: String) {
        val id = shamSelected
        if (id.isBlank()) { error = tr("اختر طلب شام كاش أولاً"); return }
        run {
            val repo = AppContainer.walletRepository
            repo.submitShamCashReceipt(id, bytes, mimeType, extension).getOrThrow()
            shamSelected = ""
            shamTopUps = repo.getMyShamCashTopUps().getOrThrow()
            message = tr("تم إرسال الإيصال. الطلب قيد المراجعة من الإدارة.")
        }
    }

    fun buy(serviceId: String) {
        run {
            val requestId = walletRequestIds.getOrPut(serviceId) { java.util.UUID.randomUUID().toString() }
            val result = AppContainer.walletRepository.purchaseServiceWithWallet(serviceId, requestId).getOrThrow()
            walletRequestIds.remove(serviceId)
            message = trf("تم إنشاء الطلب {0} وحُجز المبلغ بنجاح", result.orderId)
            wallet = AppContainer.walletRepository.getWallet().getOrThrow()
            transactions = AppContainer.walletRepository.getTransactions().getOrThrow()
        }
    }

    fun createManualOrder(serviceId: String) {
        run {
            pendingOrder = AppContainer.walletRepository.createOrder(serviceId).getOrThrow()
            val order = pendingOrder
            message = trf("حوّل {0} {1} إلى عنوان المحفظة أدناه، ثم أرسل صورة الإيصال ورقم المعاملة خلال 20 دقيقة.", order?.price, order?.currency)
        }
    }

    fun submitManualOrder(bytes: ByteArray, mimeType: String, extension: String) {
        val order = pendingOrder
        if (order == null) { error = tr("أنشئ طلب الدفع أولاً"); return }
        if (!txHashRegex.matches(txHash.trim())) { error = tr("أدخل رقم معاملة صالحاً بطول 64 خانة قبل رفع الإيصال"); return }
        run {
            val path = AppContainer.walletRepository.uploadReceipt(order.orderId, bytes, mimeType, extension).getOrThrow()
            AppContainer.walletRepository.submitOrderPayment(order.orderId, txHash, path).getOrThrow()
            message = tr("تم إرسال إثبات الدفع بنجاح. الطلب قيد المراجعة.")
            pendingOrder = null
            _txHash = ""
            history = AppContainer.walletRepository.getMyOrders().getOrThrow()
        }
    }

    /** True when [value] is a 64-hex-character transaction hash, with or without a 0x prefix. */
    fun isValidTxHash(value: String): Boolean = txHashRegex.matches(value.trim())

    /**
     * Attaches a payment receipt to an EXISTING open order from the purchase-history screen.
     * It makes the same two repository calls as [submitManualOrder] (upload the receipt, then
     * submit the payment for review), so the server-side payment approval flow is unchanged.
     */
    fun submitReceiptForOrder(orderId: String, txHash: String, bytes: ByteArray, mimeType: String, extension: String) {
        if (orderId.isBlank()) { error = tr("طلب غير صالح"); return }
        if (!isValidTxHash(txHash)) { error = tr("أدخل رقم معاملة صالحاً بطول 64 خانة قبل رفع الإيصال"); return }
        run {
            val path = AppContainer.walletRepository.uploadReceipt(orderId, bytes, mimeType, extension).getOrThrow()
            AppContainer.walletRepository.submitOrderPayment(orderId, txHash.trim(), path).getOrThrow()
            message = tr("تم إرسال إثبات الدفع بنجاح. الطلب قيد المراجعة.")
            history = AppContainer.walletRepository.getMyOrders().getOrThrow()
        }
    }

    fun access() {
        val code = accessCode.trim()
        if (code.isBlank()) { error = tr("أدخل رمز الوصول"); return }
        run {
            val sub = AppContainer.subscriptionRepository.getSubscription(code)
            subscriptions = if (sub == null) emptyList() else listOf(sub)
            sms = AppContainer.subscriptionRepository.getMySms(code)
            if (sub == null) message = tr("لم يتم العثور على اشتراك بهذا الرمز")
        }
    }

    fun loadHistory() { run { history = AppContainer.walletRepository.getMyOrders().getOrThrow() } }

    fun signOut(onDone: () -> Unit) {
        run {
            try {
                AppContainer.userRepository.signOut().getOrThrow()
            } finally {
                clearSessionState()
            }
            onDone()
        }
    }
}
