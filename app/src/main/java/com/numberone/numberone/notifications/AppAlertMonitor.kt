package com.numberone.numberone.notifications

import android.content.Context
import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.core.localization.trf
import com.numberone.numberone.di.AppContainer
import com.numberone.numberone.domain.model.isOpenStatus
import com.numberone.numberone.presentation.util.needsAdminAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppAlertMonitor {
    private const val PREFS = "numberone_seen_alerts"

    private fun isNew(context: Context, key: String): Boolean {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (sp.getBoolean(key, false)) return false
        sp.edit().putBoolean(key, true).apply()
        return true
    }

    suspend fun checkAndNotify(context: Context) = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        SmsNotifications.ensureChannel(appCtx)
        if (!SmsNotifications.canPost(appCtx)) return@withContext

        // 1. Customer notifications (unread in-app alerts + new virtual SMS codes)
        runCatching {
            val notifs = AppContainer.engagementRepository.getMyNotifications().getOrNull().orEmpty()
            notifs.filter { it.isUnread }.take(5).forEach { n ->
                if (isNew(appCtx, "notif_${n.id}")) {
                    SmsNotifications.show(
                        context = appCtx,
                        id = ("notif_" + n.id).hashCode(),
                        title = n.title.ifBlank { "Number One" },
                        body = n.body.ifBlank { tr("لديك إشعار جديد في التطبيق") }
                    )
                }
            }
        }

        runCatching {
            val latest = AppContainer.virtualNumberRepository.latestActivation().getOrNull()
            if (latest != null && !latest.smsCode.isNullOrBlank()) {
                val sig = "sms_${latest.id}_${latest.smsCode}"
                if (isNew(appCtx, sig)) {
                    SmsNotifications.show(
                        context = appCtx,
                        id = SmsNotifications.notifyId(latest.id),
                        title = tr("وصل رمز تحقق SMS جديد"),
                        body = trf("الرقم {0} • افتح التطبيق لقراءة الرمز.", latest.phoneNumber)
                    )
                }
            }
        }

        // 2. Admin alerts (new number orders, Sham Cash receipts, USDT deposits)
        val isAdmin = runCatching { AppContainer.userRepository.isAdmin().getOrDefault(false) }.getOrDefault(false)
        if (isAdmin) {
            runCatching {
                val orders = AppContainer.adminRepository.listOrders().getOrNull().orEmpty()
                orders.filter { it.needsAdminAction() }.forEach { o ->
                    if (isNew(appCtx, "adm_order_${o.id}")) {
                        SmsNotifications.show(
                            context = appCtx,
                            id = ("adm_order_" + o.id).hashCode(),
                            title = tr("👑 طلب رقم جديد يحتاج قرارك"),
                            body = trf("الخدمة: {0} • المبلغ: {1} USDT", o.serviceName.ifBlank { tr("رقم خاص") }, o.price)
                        )
                    }
                }
            }

            runCatching {
                val shams = AppContainer.adminRepository.listShamCashTopUps().getOrNull().orEmpty()
                shams.filter { isOpenStatus(it.status) && !it.receiptPath.isNullOrBlank() }.forEach { s ->
                    if (isNew(appCtx, "adm_sham_${s.id}")) {
                        SmsNotifications.show(
                            context = appCtx,
                            id = ("adm_sham_" + s.id).hashCode(),
                            title = tr("💳 إيصال شام كاش جديد"),
                            body = trf("طلب شحن بقيمة {0} بانتظار المراجعة.", s.paidAmount)
                        )
                    }
                }
            }

            runCatching {
                val deps = AppContainer.adminRepository.listDeposits().getOrNull().orEmpty()
                deps.filter { isOpenStatus(it.status) && it.requestedAmount > 0.0 }.forEach { d ->
                    if (isNew(appCtx, "adm_dep_${d.id}")) {
                        SmsNotifications.show(
                            context = appCtx,
                            id = ("adm_dep_" + d.id).hashCode(),
                            title = tr("💰 طلب إيداع USDT جديد"),
                            body = trf("مبلغ {0} USDT بانتظار التحقق.", d.requestedAmount)
                        )
                    }
                }
            }
        }
    }
}
