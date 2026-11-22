package com.numberone.numberone.presentation.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.numberone.numberone.core.components.CopyIconButton
import com.numberone.numberone.core.components.EmptyState
import com.numberone.numberone.core.components.NOCard
import com.numberone.numberone.core.components.NoticeBanner
import com.numberone.numberone.core.components.formatDateTime
import com.numberone.numberone.core.components.ltr
import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.presentation.viewmodel.AdminViewModel

/** Admin audit log: who did what and when, newest first. Read-only. */
internal fun LazyListScope.auditItems(vm: AdminViewModel) {
    if (vm.auditUnavailable) {
        item {
            NoticeBanner(
                text = tr("سجل الإدارة غير مفعّل بعد. طبّق ملف SQL الخاص بسجل التدقيق (admin_audit_log) بعد مراجعته.") +
                    (vm.auditError?.let { "\n\n[$it]" } ?: ""),
                isError = true
            )
        }
        return
    }
    if (vm.auditLog.isEmpty() && !vm.loading) {
        item { EmptyState(tr("لا توجد إجراءات مسجلة بعد")) }
    }
    items(vm.auditLog, key = { "audit-" + it.id }) { e ->
        NOCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(auditActionLabel(e.action), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        vm.emailFor(e.adminId, e.adminEmail) + " • " + formatDateTime(e.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            e.targetId?.takeIf { it.isNotBlank() }?.let { target ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        ltr(target),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    CopyIconButton(value = target)
                }
            }
            e.details?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Human label for an audit action code. Unknown codes are shown as stored. */
private fun auditActionLabel(code: String): String = when (code.trim()) {
    "order_approved" -> tr("اعتماد طلب")
    "order_rejected" -> tr("رفض طلب")
    "topup_approved" -> tr("اعتماد إيداع شام كاش")
    "topup_rejected" -> tr("رفض إيداع شام كاش")
    "wallet_adjusted" -> tr("تعديل رصيد")
    "subscription_assigned" -> tr("إسناد اشتراك")
    "access_reset" -> tr("تحديث وصول اشتراك")
    "service_saved" -> tr("حفظ خدمة")
    "service_toggled" -> tr("تفعيل/تعطيل خدمة")
    "service_deleted" -> tr("حذف خدمة")
    "payment_config_updated" -> tr("تحديث إعدادات الدفع")
    "virtual_margin_set" -> tr("تغيير هامش الأرقام الافتراضية")
    "admin_role_changed" -> tr("تغيير صلاحية مدير")
    else -> code.replace('_', ' ')
}
