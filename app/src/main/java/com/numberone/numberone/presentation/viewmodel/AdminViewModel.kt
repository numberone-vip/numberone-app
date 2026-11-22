package com.numberone.numberone.presentation.viewmodel

import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.core.localization.trf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.numberone.numberone.core.error.ErrorMapper
import com.numberone.numberone.di.AppContainer
import com.numberone.numberone.data.remote.SupportChatSource
import com.numberone.numberone.domain.model.AdminAuditEntry
import com.numberone.numberone.domain.model.ChatConversation
import com.numberone.numberone.domain.model.ChatMessage
import com.numberone.numberone.domain.model.AdminPendingPurchase
import com.numberone.numberone.domain.model.AdminPhoneMatch
import com.numberone.numberone.domain.model.AdminService
import com.numberone.numberone.domain.model.AdminShamCashTopUp
import com.numberone.numberone.domain.model.AdminSubscription
import com.numberone.numberone.domain.model.AdminSummary
import com.numberone.numberone.domain.model.AdminUser
import com.numberone.numberone.domain.model.AdminWalletDeposit
import com.numberone.numberone.domain.model.AppPaymentConfig
import com.numberone.numberone.domain.model.Order
import com.numberone.numberone.domain.model.ServiceDraft
import com.numberone.numberone.domain.model.WalletTransaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** UI state only. Every action is authorized by the server (is_admin() inside each RPC). */
class AdminViewModel : ViewModel() {
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var message by mutableStateOf<String?>(null); private set
    var summary by mutableStateOf<AdminSummary?>(null); private set
    var shamTopUps by mutableStateOf<List<AdminShamCashTopUp>>(emptyList()); private set
    var orders by mutableStateOf<List<Order>>(emptyList()); private set
    var receiptUrl by mutableStateOf<String?>(null); private set
    var deposits by mutableStateOf<List<AdminWalletDeposit>>(emptyList()); private set
    var subscriptions by mutableStateOf<List<AdminSubscription>>(emptyList()); private set
    var services by mutableStateOf<List<AdminService>>(emptyList()); private set
    var users by mutableStateOf<List<AdminUser>>(emptyList()); private set
    var ledger by mutableStateOf<List<WalletTransaction>>(emptyList()); private set
    var paymentConfig by mutableStateOf<AppPaymentConfig?>(null); private set
    var virtualMargin by mutableStateOf<Double?>(null); private set
    var auditLog by mutableStateOf<List<AdminAuditEntry>>(emptyList()); private set
    var pendingPurchases by mutableStateOf<List<AdminPendingPurchase>>(emptyList()); private set
    // Customer chat (inbox + the open conversation).
    var conversations by mutableStateOf<List<ChatConversation>>(emptyList()); private set
    var chatUserId by mutableStateOf<String?>(null); private set
    var chatEmail by mutableStateOf(""); private set
    var chatMessages by mutableStateOf<List<ChatMessage>>(emptyList()); private set
    var chatError by mutableStateOf<String?>(null); private set
    var chatSending by mutableStateOf(false); private set
    /** null = no search yet; empty list = searched and nothing matched. */
    var phoneMatches by mutableStateOf<List<AdminPhoneMatch>?>(null); private set
    var phoneSearchError by mutableStateOf<String?>(null); private set
    /** True when the audit-log RPCs are missing or failing (migration not applied). Not shown as a warning banner. */
    var auditUnavailable by mutableStateOf(false); private set
    var auditError by mutableStateOf<String?>(null); private set
    /** Id of the signed-in admin, used to hide role actions on one's own row (the server also blocks it). */
    var selfId by mutableStateOf<String?>(null); private set
    /** Non-fatal: sections that failed to load, each with the technical reason. */
    var warning by mutableStateOf<String?>(null); private set
    /** Inline success/failure feedback shown inside the payment-config card. */
    var configNotice by mutableStateOf<String?>(null); private set
    var configNoticeIsError by mutableStateOf(false); private set

    fun clearConfigNotice() { configNotice = null }

    /** Best available e-mail for a user id (order/top-up rows may not carry it). */
    fun emailFor(userId: String?, fallback: String? = null): String {
        if (!fallback.isNullOrBlank()) return fallback
        if (userId.isNullOrBlank()) return "—"
        return users.firstOrNull { it.userId == userId }?.email ?: userId.take(8) + "…"
    }

    fun fail(msg: String) { error = msg; message = null }
    fun closeReceipt() { receiptUrl = null }

    /**
     * Admin-only error text: the friendly message plus the real exception, so a failing RPC
     * (missing function, wrong parameter, decode mismatch, RLS denial) is identifiable.
     */
    private fun detailOf(e: Throwable): String {
        val friendly = ErrorMapper.toUserMessage(e)
        val technical = listOfNotNull(e::class.simpleName, e.message?.takeIf { it.isNotBlank() })
            .joinToString(": ")
        return if (technical.isBlank() || technical in friendly) friendly else "$friendly\n\n[$technical]"
    }

    private fun run(task: suspend () -> Unit) {
        if (loading) return
        viewModelScope.launch {
            loading = true; error = null; message = null
            try {
                task()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = detailOf(e)
            } finally {
                loading = false
            }
        }
    }

    /**
     * Refreshes every section. Each section is independent: one failing RPC is reported in
     * [warning] with its own reason, and the rest of the panel still loads. Never throws, so a
     * successful write followed by a refresh can't be displayed as a failed write.
     */
    private suspend fun reload() {
        val repo = AppContainer.adminRepository
        val failed = mutableListOf<String>()
        fun <T> report(label: String, result: Result<T>, apply: (T) -> Unit) {
            result.onSuccess(apply).onFailure { e -> failed += "$label (${detailOf(e).lineSequence().last()})" }
        }
        report(tr("الملخص"), repo.getSummary()) { summary = it }
        report(tr("طلبات الشام كاش"), repo.listShamCashTopUps()) { shamTopUps = it }
        report(tr("الطلبات"), repo.listOrders()) { orders = it }
        report(tr("الإيداعات"), repo.listDeposits()) { deposits = it }
        report(tr("الاشتراكات"), repo.listSubscriptions()) { subscriptions = it }
        report(tr("المستخدمون"), repo.listUsers()) { users = it }
        report(tr("سجل الحركات"), repo.listWalletTransactions()) { ledger = it }
        report(tr("الخدمات"), repo.listServices()) { services = it }
        report(tr("إعدادات الدفع"), repo.getPaymentConfig()) { paymentConfig = it }
        report(tr("هامش الأرقام الافتراضية"), repo.getVirtualNumberMargin()) { virtualMargin = it }
        report(tr("طلبات الشراء اليدوي"), repo.listPendingPurchases()) { pendingPurchases = it }
        refreshAuditNow()
        warning = if (failed.isEmpty()) null else
            tr("تعذر تحميل بعض الأقسام:") + "\n" + failed.joinToString("\n") +
                "\n" + tr("تأكد من تطبيق ملف SQL الخاص بلوحة الإدارة.")
    }

    private suspend fun refreshAuditNow() {
        AppContainer.adminRepository.listAuditLog()
            .onSuccess { auditLog = it; auditUnavailable = false; auditError = null }
            .onFailure { e -> auditUnavailable = true; auditError = detailOf(e) }
    }

    /** Silent refresh used when the audit tab is opened; never touches the loading/error banners. */
    fun refreshAudit() {
        viewModelScope.launch { refreshAuditNow() }
    }

    /** Best-effort audit entry after a successful write. A logging failure never fails the action itself. */
    private suspend fun audit(action: String, target: String? = null, details: String? = null) {
        if (auditUnavailable) return
        AppContainer.adminRepository.logAdminAction(action, target, details)
    }

    /** True only while [load] runs, so the pull-to-refresh spinner is not shown for every admin action. */
    var refreshing by mutableStateOf(false); private set

    fun load() = run {
        refreshing = true
        try {
            check(AppContainer.userRepository.isAdmin().getOrThrow()) { tr("غير مصرح بالوصول") }
            selfId = AppContainer.userRepository.getCurrentUser().getOrNull()?.id
            reload()
        } finally {
            refreshing = false
        }
    }

    fun approveShamCash(id: String, usdt: Double) = run {
        val credited = AppContainer.adminRepository.approveShamCashTopUp(id, usdt).getOrThrow()
        message = trf("تم اعتماد الطلب وإضافة {0} USDT", credited)
        audit("topup_approved", id, "$credited USDT")
        reload()
    }

    fun rejectShamCash(id: String, reason: String) = run {
        AppContainer.adminRepository.rejectShamCashTopUp(id, reason).getOrThrow()
        audit("topup_rejected", id, reason)
        message = tr("تم رفض الطلب")
        reload()
    }

    fun approveManualPurchase(requestId: String, phone: String, expiry: String) = run {
        AppContainer.adminRepository.approveManualPurchase(requestId, phone, expiry).getOrThrow()
        audit("manual_purchase_approved", requestId, "expiry $expiry")
        message = tr("تم اعتماد الشراء وربط الرقم بالعميل")
        reload()
    }

    fun loadConversations() {
        viewModelScope.launch {
            runCatching { SupportChatSource.adminConversations() }
                .onSuccess { conversations = it; chatError = null }
                .onFailure { chatError = detailOf(it) }
        }
    }

    fun openChat(userId: String, email: String) {
        chatUserId = userId; chatEmail = email; chatMessages = emptyList(); chatError = null
        reloadChat()
    }

    fun reloadChat() {
        val id = chatUserId ?: return
        viewModelScope.launch {
            runCatching { SupportChatSource.adminChat(id) }
                .onSuccess { if (chatUserId == id) { chatMessages = it; chatError = null } }
                .onFailure { chatError = detailOf(it) }
        }
    }

    fun closeChat() {
        chatUserId = null; chatMessages = emptyList(); chatError = null
        loadConversations()
    }

    fun sendChat(text: String) {
        val id = chatUserId ?: return
        val body = text.trim()
        if (body.isEmpty()) return
        viewModelScope.launch {
            chatSending = true
            runCatching { SupportChatSource.adminSend(id, body) }
                .onSuccess { reloadChat() }
                .onFailure { chatError = detailOf(it) }
            chatSending = false
        }
    }

    /** Looks up the customer who was assigned this number. Errors stay local to the search box. */
    fun searchByPhone(phone: String) {
        viewModelScope.launch {
            phoneSearchError = null
            AppContainer.adminRepository.findCustomersByPhone(phone)
                .onSuccess { phoneMatches = it }
                .onFailure { e -> phoneMatches = emptyList(); phoneSearchError = detailOf(e).lineSequence().first() }
        }
    }

    /** Sends the verification code the admin received on the number to the customer's account. The code is not logged. */
    fun sendVerificationCode(subscriptionId: String, code: String) = run {
        AppContainer.adminRepository.addSmsToSubscription(subscriptionId, "Verification", code).getOrThrow()
        audit("verification_code_sent", subscriptionId)
        message = tr("تم إرسال الرمز للعميل")
        reload()
    }

    fun approveOrder(id: String, phone: String, code: String, expiry: String) = run {
        AppContainer.adminRepository.approveOrder(id, phone, code, expiry).getOrThrow()
        audit("order_approved", id, "expiry $expiry")
        message = tr("تم اعتماد الطلب")
        reload()
    }

    fun rejectOrder(id: String, reason: String) = run {
        AppContainer.adminRepository.rejectOrder(id, reason).getOrThrow()
        audit("order_rejected", id, reason)
        message = tr("تم رفض الطلب")
        reload()
    }

    fun showReceipt(path: String?) {
        if (path.isNullOrBlank()) { fail(tr("لا يوجد إيصال")); return }
        run { receiptUrl = AppContainer.adminRepository.getReceiptUrl(path).getOrThrow() }
    }

    // ---- Payment & app config ------------------------------------------------

    fun savePaymentConfig(config: AppPaymentConfig) {
        if (loading) return
        viewModelScope.launch {
            loading = true; error = null; message = null; configNotice = null
            try {
                AppContainer.adminRepository.updatePaymentConfig(config).getOrThrow()
                audit("payment_config_updated")
                paymentConfig = AppContainer.adminRepository.getPaymentConfig().getOrNull() ?: config
                configNotice = tr("تم حفظ الإعدادات بنجاح")
                configNoticeIsError = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                configNotice = detailOf(e)
                configNoticeIsError = true
            } finally {
                loading = false
            }
        }
    }

    // ---- Services catalog ----------------------------------------------------

    fun saveService(draft: ServiceDraft) = run {
        AppContainer.adminRepository.upsertService(draft).getOrThrow()
        audit("service_saved", draft.id, draft.name)
        message = if (draft.id == null) tr("تمت إضافة الخدمة") else tr("تم حفظ الخدمة")
        services = AppContainer.adminRepository.listServices().getOrThrow()
    }

    fun setServiceActive(service: AdminService, active: Boolean) = run {
        AppContainer.adminRepository.upsertService(
            ServiceDraft(
                id = service.id,
                name = service.name,
                country = service.country,
                countryCode = service.countryCode,
                price = service.price,
                description = service.description.orEmpty(),
                isActive = active
            )
        ).getOrThrow()
        message = if (active) tr("تم تفعيل الخدمة") else tr("تم تعطيل الخدمة")
        audit("service_toggled", service.id, if (active) "enabled" else "disabled")
        services = AppContainer.adminRepository.listServices().getOrThrow()
    }

    fun deleteService(id: String) = run {
        AppContainer.adminRepository.deleteService(id).getOrThrow()
        audit("service_deleted", id)
        message = tr("تم حذف الخدمة")
        services = AppContainer.adminRepository.listServices().getOrThrow()
    }

    // ---- Users & wallets -----------------------------------------------------

    fun adjustBalance(userId: String, amount: Double, note: String, requestId: String) = run {
        val newBalance = AppContainer.adminRepository.adjustWalletBalance(userId, amount, note, requestId).getOrThrow()
        // The write has succeeded. Set the success message before refreshing; reload() never throws.
        message = trf("تم تحديث الرصيد. الرصيد الجديد: {0} USDT", newBalance)
        audit("wallet_adjusted", userId, "amount $amount")
        reload()
    }

    fun setAdminRole(userId: String, makeAdmin: Boolean) = run {
        AppContainer.adminRepository.setAdminRole(userId, makeAdmin).getOrThrow()
        audit("admin_role_changed", userId, if (makeAdmin) "promoted" else "demoted")
        message = if (makeAdmin) tr("تمت ترقية المستخدم إلى مدير") else tr("تمت إزالة صلاحية المدير")
        reload()
    }

    fun saveVirtualMargin(percent: Double) = run {
        virtualMargin = AppContainer.adminRepository.setVirtualNumberMargin(percent).getOrThrow()
        audit("virtual_margin_set", null, "$percent%")
        message = tr("تم حفظ هامش الربح")
    }

    fun assignSubscription(userId: String, serviceId: String, phone: String, code: String, expiry: String) = run {
        AppContainer.adminRepository.assignSubscription(userId, serviceId, phone, code, expiry).getOrThrow()
        audit("subscription_assigned", userId, "service $serviceId, expiry $expiry")
        message = tr("تم إسناد الاشتراك للمستخدم")
        reload()
    }

    fun resetAccess(subscriptionId: String, code: String, expiry: String) = run {
        AppContainer.adminRepository.resetSubscriptionAccess(subscriptionId, code, expiry).getOrThrow()
        audit("access_reset", subscriptionId, listOfNotNull(if (code.isNotBlank()) "code rotated" else null, if (expiry.isNotBlank()) "expiry $expiry" else null).joinToString(", "))
        message = tr("تم تحديث وصول الاشتراك")
        reload()
    }
}
