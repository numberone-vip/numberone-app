package com.numberone.numberone.data.repository
import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.data.remote.*
import com.numberone.numberone.domain.model.*
import io.github.jan.supabase.auth.auth

class EngagementRepository(private val source: SupabaseEngagementDataSource = SupabaseEngagementDataSource()) {
    private fun requireAuth() { check(SupabaseClientProvider.client.auth.currentUserOrNull() != null) { tr("يرجى تسجيل الدخول أولاً") } }
    suspend fun countUnreadNotifications() = runCatching { requireAuth(); source.countUnreadNotifications() }
    suspend fun getMyNotifications(limit: Int = 50) = runCatching { requireAuth(); source.getMyNotifications(limit) }
    suspend fun markNotificationRead(id: String) = runCatching { requireAuth(); require(id.isNotBlank()) { tr("معرّف الإشعار غير صالح") }; source.markNotificationRead(id) }
    suspend fun markAllNotificationsRead() = runCatching { requireAuth(); source.markAllNotificationsRead() }
    suspend fun getOrCreateMyReferralCode() = runCatching { requireAuth(); source.getOrCreateMyReferralCode() }
    suspend fun applyReferralCode(code: String) = runCatching { requireAuth(); val c = code.trim().uppercase(); require(c.length in 3..32) { tr("أدخل كود إحالة صالحاً") }; source.applyReferralCode(c) }
    suspend fun validatePromoCode(code: String, amount: Double) = runCatching { requireAuth(); val c = code.trim().uppercase(); require(c.length in 2..40) { tr("أدخل كود خصم صالحاً") }; require(amount > 0.0) { tr("المبلغ غير صالح") }; source.validatePromoCode(c, amount) }
    suspend fun submitOrderReview(orderId: String, rating: Int, text: String?) = runCatching { requireAuth(); require(orderId.isNotBlank()) { tr("معرّف الطلب غير صالح") }; require(rating in 1..5) { tr("التقييم يجب أن يكون بين 1 و 5 نجوم") }; source.submitOrderReview(orderId, rating, text) }
}
