package com.numberone.numberone.presentation.viewmodel

import com.numberone.numberone.core.error.AppError
import com.numberone.numberone.core.error.ErrorMapper
import com.numberone.numberone.core.session.SessionEvents
import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.core.localization.trf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.numberone.numberone.core.error.ErrorPresentation
import com.numberone.numberone.di.AppContainer
import com.numberone.numberone.domain.repository.AdminRepository
import com.numberone.numberone.domain.repository.UserRepository
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
class AdminViewModel(
    private val adminRepo: AdminRepository = AppContainer.adminRepository,
    private val userRepo: UserRepository = AppContainer.userRepository
) : FeedbackViewModel() {
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

    override fun describeError(e: Throwable): String = ErrorPresentation.describe(e)

    override fun onTaskFailed(e: Throwable) {
        if (ErrorMapper.classify(e) == AppError.SessionExpired) SessionEvents.markExpired()
    }

    /**
     * Refreshes every section. Each section is independent: one failing RPC is reported in
     * [warning] with its own reason, and the rest of the panel still loads. Never throws, so a
     * successful write followed by a refresh can't be displayed as a failed write.
     */
    private suspend fun reload() {
        val repo = adminRepo
        val failed = mutableListOf<String>()
        fun <T> report(label: String, result: Result<T>, apply: (T) -> Unit) {
            result.onSuccess(apply).onFailure { e -> failed += "$label (${describeError(e).lineSequence().last()})" }
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
        warning = if (failed.isEmpty()) null else
            tr("تعذر تحميل بعض الأقسام:") + "\n" + failed.joinToString("\n") +
                "\n" + tr("تأكد من تطبيق ملف SQL الخاص بلوحة الإدارة.")
    }

    fun load() = run {
        check(userRepo.isAdmin().getOrThrow()) { tr("غير مصرح بالوصول") }
        reload()
    }

    fun approveShamCash(id: String, usdt: Double) = run {
        val credited = adminRepo.approveShamCashTopUp(id, usdt).getOrThrow()
        message = trf("تم اعتماد الطلب وإضافة {0} USDT", credited)
        reload()
    }

    fun rejectShamCash(id: String, reason: String) = run {
        adminRepo.rejectShamCashTopUp(id, reason).getOrThrow()
        message = tr("تم رفض الطلب")
        reload()
    }

    fun approveOrder(id: String, phone: String, expiry: String) = run {
        val subId = adminRepo.approveOrder(id, phone, expiry).getOrThrow()
        message = if (subId.isNotBlank()) trf("تم اعتماد الطلب وإسناد الرقم • UUID: {0}", subId) else tr("تم اعتماد الطلب")
        reload()
    }

    fun rejectOrder(id: String, reason: String) = run {
        adminRepo.rejectOrder(id, reason).getOrThrow()
        message = tr("تم رفض الطلب")
        reload()
    }

    fun showReceipt(path: String?) {
        if (path.isNullOrBlank()) { fail(tr("لا يوجد إيصال")); return }
        run { receiptUrl = adminRepo.getReceiptUrl(path).getOrThrow() }
    }

    // ---- Payment & app config ------------------------------------------------

    fun savePaymentConfig(config: AppPaymentConfig) {
        if (loading) return
        viewModelScope.launch {
            loading = true; error = null; message = null; configNotice = null
            try {
                adminRepo.updatePaymentConfig(config).getOrThrow()
                paymentConfig = adminRepo.getPaymentConfig().getOrNull() ?: config
                configNotice = tr("تم حفظ الإعدادات بنجاح")
                configNoticeIsError = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                configNotice = describeError(e)
                configNoticeIsError = true
            } finally {
                loading = false
            }
        }
    }

    // ---- Services catalog ----------------------------------------------------

    fun saveService(draft: ServiceDraft) = run {
        adminRepo.upsertService(draft).getOrThrow()
        message = if (draft.id == null) tr("تمت إضافة الخدمة") else tr("تم حفظ الخدمة")
        services = adminRepo.listServices().getOrThrow()
    }

    fun setServiceActive(service: AdminService, active: Boolean) = run {
        adminRepo.upsertService(
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
        services = adminRepo.listServices().getOrThrow()
    }

    fun deleteService(id: String) = run {
        adminRepo.deleteService(id).getOrThrow()
        message = tr("تم حذف الخدمة")
        services = adminRepo.listServices().getOrThrow()
    }

    // ---- Users & wallets -----------------------------------------------------

    fun adjustBalance(userId: String, amount: Double, note: String, requestId: String) = run {
        val newBalance = adminRepo.adjustWalletBalance(userId, amount, note, requestId).getOrThrow()
        // The write has succeeded. Set the success message before refreshing; reload() never throws.
        message = trf("تم تحديث الرصيد. الرصيد الجديد: {0} USDT", newBalance)
        reload()
    }

    fun assignSubscription(userId: String, serviceId: String, phone: String, code: String, expiry: String) = run {
        adminRepo.assignSubscription(userId, serviceId, phone, code, expiry).getOrThrow()
        message = tr("تم إسناد الاشتراك للمستخدم")
        reload()
    }

    fun sendNotification(targetUserId: String?, title: String, body: String) = run {
        val targets = if (targetUserId.isNullOrBlank()) users.map { it.userId } else listOf(targetUserId)
        val sent = adminRepo.sendAdminNotification(targets, title, body).getOrThrow()
        message = trf("تم إرسال الإشعار إلى {0} عميل بنجاح", sent)
    }
    fun resetAccess(subscriptionId: String, code: String, expiry: String) = run {
        adminRepo.resetSubscriptionAccess(subscriptionId, code, expiry).getOrThrow()
        message = tr("تم تحديث وصول الاشتراك")
        reload()
    }

    fun sendSubscriptionSms(subscriptionId: String, sender: String, body: String) = run {
        adminRepo.sendSubscriptionSms(subscriptionId, sender, body).getOrThrow()
        message = tr("تم إرسال الرسالة إلى صندوق وارد العميل")
    }
}
