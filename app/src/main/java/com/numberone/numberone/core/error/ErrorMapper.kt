package com.numberone.numberone.core.error

import com.numberone.numberone.core.localization.AppLanguage
import com.numberone.numberone.core.localization.AppStrings
import kotlinx.serialization.SerializationException
import java.io.IOException

/**
 * App-wide language flag used when producing user-facing text outside Compose.
 * MainActivity keeps it in sync with the language the user selected.
 */
object AppLocale {
    /** True while Arabic is the selected UI language (driven by AppStrings). */
    val isArabic: Boolean get() = AppStrings.language == AppLanguage.ARABIC
}

/** An error whose message is already safe and localized for display. */
class UserFacingException(message: String) : Exception(message)

/**
 * Converts any throwable into a short, clean message for the user.
 * Raw URLs, headers, JSON bodies and stack traces are never returned.
 */
object ErrorMapper {

    private const val GENERIC_AR = "حدث خطأ غير متوقع. حاول مرة أخرى."
    private const val GENERIC_EN = "Something went wrong. Please try again."

    fun toUserMessage(error: Throwable?): String {
        val arabic = AppLocale.isArabic
        fun t(ar: String, en: String): String = if (arabic) ar else en

        if (error == null) return t(GENERIC_AR, GENERIC_EN)
        if (error is UserFacingException) return error.message ?: t(GENERIC_AR, GENERIC_EN)

        val raw = error.message.orEmpty()
        val lower = raw.lowercase()

        return when {
            error is SerializationException ->
                t("تعذر معالجة استجابة الخادم", "Could not read the server response.")

            error is IOException ->
                t(
                    "تعذر الاتصال بالخادم. تحقق من الإنترنت وحاول مرة أخرى.",
                    "Could not reach the server. Check your connection and try again."
                )

            "invalid login" in lower || "invalid_credentials" in lower ->
                t("البريد الإلكتروني أو كلمة المرور غير صحيحة", "Incorrect email or password.")

            "email not confirmed" in lower ->
                t("يرجى تأكيد البريد الإلكتروني أولاً", "Please confirm your email address first.")

            "already registered" in lower || "already exists" in lower || "user_already_exists" in lower ->
                t("هذا البريد الإلكتروني مسجل مسبقاً", "This email address is already registered.")

            ("weak" in lower && "password" in lower) || "password should" in lower ->
                t("كلمة المرور ضعيفة. استخدم 8 أحرف على الأقل", "Password is too weak. Use at least 8 characters.")

            "rate limit" in lower || "too many" in lower || "over_request_rate_limit" in lower ->
                t("محاولات كثيرة. انتظر قليلاً ثم حاول مجدداً", "Too many attempts. Wait a moment and try again.")

            "insufficient" in lower ->
                t("الرصيد غير كافٍ لإتمام العملية", "Insufficient balance to complete this action.")

            "jwt" in lower || "not authenticated" in lower || "refresh_token" in lower ->
                t("انتهت الجلسة. يرجى تسجيل الدخول مجدداً", "Your session has expired. Please sign in again.")

            (error is IllegalArgumentException || error is IllegalStateException) && containsArabic(raw) -> raw

            else -> t(GENERIC_AR, GENERIC_EN)
        }
    }

    private fun containsArabic(text: String): Boolean = text.any { it in '\u0600'..'\u06FF' }
}
