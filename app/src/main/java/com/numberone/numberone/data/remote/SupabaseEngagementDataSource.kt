package com.numberone.numberone.data.remote
import com.numberone.numberone.domain.model.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseEngagementDataSource {
    private val sb = SupabaseClientProvider.client
    private fun unwrapScalar(raw: String): String = raw.trim().removePrefix("\"").removeSuffix("\"")

    suspend fun countUnreadNotifications(): Int =
        unwrapScalar(sb.postgrest.rpcRaw("count_my_unread_notifications", buildJsonObject {})).toIntOrNull() ?: 0

    suspend fun getMyNotifications(limit: Int = 50): List<AppNotification> =
        sb.postgrest.rpcDecode<List<AppNotification>>("get_my_notifications", buildJsonObject { put("p_limit", limit.coerceIn(1, 100)) })

    suspend fun markNotificationRead(id: String): Boolean =
        unwrapScalar(sb.postgrest.rpcRaw("mark_my_notification_read", buildJsonObject { put("p_notification_id", id.trim()) })).toBooleanStrictOrNull() ?: false

    suspend fun markAllNotificationsRead(): Int =
        unwrapScalar(sb.postgrest.rpcRaw("mark_all_my_notifications_read", buildJsonObject {})).toIntOrNull() ?: 0

    suspend fun getOrCreateMyReferralCode(): String =
        unwrapScalar(sb.postgrest.rpcRaw("get_or_create_my_referral_code", buildJsonObject {}))

    suspend fun applyReferralCode(code: String): Boolean =
        unwrapScalar(sb.postgrest.rpcRaw("apply_referral_code", buildJsonObject { put("p_code", code.trim().uppercase()) })).toBooleanStrictOrNull() ?: false

    suspend fun validatePromoCode(code: String, amount: Double): PromoValidationResult =
        sb.postgrest.rpcDecode<List<PromoValidationResult>>("validate_promo_code", buildJsonObject { put("p_code", code.trim().uppercase()); put("p_amount", amount) }).single()

    suspend fun submitOrderReview(orderId: String, rating: Int, text: String?): String =
        unwrapScalar(sb.postgrest.rpcRaw("submit_my_order_review", buildJsonObject { put("p_order_id", orderId.trim()); put("p_rating", rating.coerceIn(1, 5)); if (!text.isNullOrBlank()) put("p_review_text", text.trim()) }))
}
