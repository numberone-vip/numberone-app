package com.numberone.numberone.data.remote
import com.numberone.numberone.domain.model.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseEngagementDataSource {
    private val sb = SupabaseClientProvider.client
    suspend fun countUnreadNotifications(): Int = sb.postgrest.rpcDecode<Int>("count_my_unread_notifications")
    suspend fun getMyNotifications(limit: Int = 50): List<AppNotification> = sb.postgrest.rpcDecode<List<AppNotification>>("get_my_notifications", buildJsonObject { put("p_limit", limit.coerceIn(1, 100)) })
    suspend fun markNotificationRead(id: String): Boolean = sb.postgrest.rpcDecode<Boolean>("mark_my_notification_read", buildJsonObject { put("p_notification_id", id.trim()) })
    suspend fun markAllNotificationsRead(): Int = sb.postgrest.rpcDecode<Int>("mark_all_my_notifications_read")
    suspend fun getOrCreateMyReferralCode(): String = sb.postgrest.rpcDecode<String>("get_or_create_my_referral_code")
    suspend fun applyReferralCode(code: String): Boolean = sb.postgrest.rpcDecode<Boolean>("apply_referral_code", buildJsonObject { put("p_code", code.trim().uppercase()) })
    suspend fun validatePromoCode(code: String, amount: Double): PromoValidationResult = sb.postgrest.rpcDecode<List<PromoValidationResult>>("validate_promo_code", buildJsonObject { put("p_code", code.trim().uppercase()); put("p_amount", amount) }).single()
    suspend fun submitOrderReview(orderId: String, rating: Int, text: String?): String = sb.postgrest.rpcDecode<String>("submit_my_order_review", buildJsonObject { put("p_order_id", orderId.trim()); put("p_rating", rating.coerceIn(1, 5)); if (!text.isNullOrBlank()) put("p_review_text", text.trim()) })
}
