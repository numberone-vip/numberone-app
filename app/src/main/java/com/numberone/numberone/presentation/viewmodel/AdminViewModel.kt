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
import kotlinx.coroutines.delay
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

    /** Retry only transient transport failures; never retry authorization, SQL, or decode errors. */
    private suspend fun <T> retryTransient(block: suspend () -> Result<T>): Result<T> {
        var result = block()
        for (attempt in 1..2) {
            val failure = result.exceptionOrNull() ?: return result
            if (!isTransientNetworkFailure(failure)) return result
            delay(350L * attempt)
            result = block()
        }
        return result
    }

    private fun isTransientNetworkFailure(error: Throwable): Boolean {
        return generateSequence(error) { it.cause }.any { cause ->
            val type = cause::class.simpleName.orEmpty()
            val message = cause.message.orEmpty().lowercase()
            type in setOf("IOException", "SocketTimeoutException", "ConnectException", "UnknownHostException") ||
                listOf("timeout", "connection abort", "connection reset", "failed to connect",
                    "unable to resolve host", "network is unreachable", "socket").any(message::contains)
        }
    }

    /**
     * Refreshes every section independently. Brief network failures get bounded retries; persistent
     * failures are shown per section while successful sections remain available. This function
     * never throws, so a successful write is not reported as failed just because refresh failed.
     */
    private suspend fun reload() {
        val repo = AppContainer.adminRepository
        val failed = mutableListOf<String>()
        fun <T> report(label: String, result: Result<T>, apply: (T) -> Unit) {
            result.onSuccess(apply).onFailure { e -> failed += "$label (${detailOf(e).lineSequence().last()})" }
        }
        report(tr("الملخص"), retryTransient { repo.getSummary() }) { summary = it }
        report(tr("طلبات الشام كاش"), retryTransient { repo.listShamCashTopUps() }) { shamTopUps = it }
        report(tr("الطلبات"), retryTransient { repo.listOrders() }) { orders = it }
        report(tr("الإيداعات"), retryTransient { repo.listDeposits() }) { deposits = it }
        report(tr("الاشتراكات"), retryTransient { repo.listSubscriptions() }) { subscriptions = it }
        report(tr("المستخدمون"), retryTransient { repo.listUsers() }) { users = it }
        report(tr("سجل الحركات"), retryTransient { repo.listWalletTransactions() }) { ledger = it }
        report(tr("الخدمات"), retryTransient { repo.listServices() }) { services = it }
        report(tr("إعدادات الدفع"), retryTransient { repo.getPaymentConfig() }) { paymentConfig = it }

        if (failed.isEmpty()) {
            warning = null
        } else {
            val details = failed.joinToString("\n")
            val sqlRelated = failed.any { item ->
                val value = item.lowercase()
                listOf("pgrst202", "42883", "42501", "permission denied", "schema cache",
                    "does not exist", "function").any(value::contains)
            }
            val advice = if (sqlRelated) {
                tr("توجد مؤشرات على مشكلة في الدوال أو الصلاحيات؛ راجع ملف SQL الخاص بلوحة الإدارة.")
            } else {
                tr("تعذر الاتصال بالخادم مؤقتاً. تحقق من الإنترنت وحالة Supabase ثم اضغط تحديث؛ لا حاجة لتطبيق SQL بسبب أخطاء الاتصال وحدها.")
            }
            warning = tr("تعذر تحميل بعض الأقسام:") + "\n" + details + "\n" + advice
        }
    }

    fun load() = run {
        check(AppContainer.userRepository.isAdmin().getOrThrow()) { tr("غير مصرح بالوصول") }
        reload()
    }

    fun approveShamCash(id: String, usdt: Double) = run {
        val credited = AppContainer.adminRepository.approveShamCashTopUp(id, usdt).getOrThrow()
        message = trf("تم اعتماد الطلب وإضافة {0} USDT", credited)
        reload()
    }

    fun rejectShamCash(id: String, reason: String) = run {
        AppContainer.adminRepository.rejectShamCashTopUp(id, reason).getOrThrow()
        message = tr("تم رفض الطلب")
        reload()
    }

    fun approveOrder(id: String, phone: String, code: String, expiry: String) = run {
        AppContainer.adminRepository.approveOrder(id, phone, code, expiry).getOrThrow()
        message = tr("تم اعتماد الطلب")
        reload()
    }

    fun rejectOrder(id: String, reason: String) = run {
        AppContainer.adminRepository.rejectOrder(id, reason).getOrThrow()
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
        services = AppContainer.adminRepository.listServices().getOrThrow()
    }

    fun deleteService(id: String) = run {
        AppContainer.adminRepository.deleteService(id).getOrThrow()
        message = tr("تم حذف الخدمة")
        services = AppContainer.adminRepository.listServices().getOrThrow()
    }

    // ---- Users & wallets -----------------------------------------------------

    fun adjustBalance(userId: String, amount: Double, note: String, requestId: String) = run {
        val newBalance = AppContainer.adminRepository.adjustWalletBalance(userId, amount, note, requestId).getOrThrow()
        // The write has succeeded. Set the success message before refreshing; reload() never throws.
        message = trf("تم تحديث الرصيد. الرصيد الجديد: {0} USDT", newBalance)
        reload()
    }

    fun assignSubscription(userId: String, serviceId: String, phone: String, code: String, expiry: String) = run {
        AppContainer.adminRepository.assignSubscription(userId, serviceId, phone, code, expiry).getOrThrow()
        message = tr("تم إسناد الاشتراك للمستخدم")
        reload()
    }

    fun resetAccess(subscriptionId: String, code: String, expiry: String) = run {
        AppContainer.adminRepository.resetSubscriptionAccess(subscriptionId, code, expiry).getOrThrow()
        message = tr("تم تحديث وصول الاشتراك")
        reload()
    }
}
