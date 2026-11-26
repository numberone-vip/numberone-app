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

/**
 * Single entry point for turning throwables into user-facing messages.
 *
 * - [classify] is pure: no locale, no Android. It is what unit tests exercise.
 * - [toUserMessage] keeps the historical signature used by existing callers.
 * Raw URLs, headers, JSON bodies and stack traces are never returned to the UI.
 */
object ErrorMapper {

    fun classify(error: Throwable?): AppError {
        if (error == null) return AppError.Unknown
        if (error is AppException) return error.error
        if (error is UserFacingException) return AppError.Localized(error.message.orEmpty())

        val raw = error.message.orEmpty()
        val lower = raw.lowercase()

        return when {
            error is SerializationException -> AppError.ServerResponse
            error is IOException -> AppError.Network
            // Messages written in Arabic by this app (validation) are already user-facing.
            containsArabic(raw) -> AppError.Localized(raw)
            "no stock" in lower || "out of stock" in lower -> AppError.NoStock
            "insufficient" in lower -> AppError.InsufficientBalance
            "payment not confirmed" in lower || "reconcil" in lower ||
                "persistence is pending" in lower || "retained" in lower -> AppError.PendingReview
            "already used with different" in lower -> AppError.DuplicateRequest
            "still pending" in lower -> AppError.PendingOrderExists
            "pending payment order" in lower -> AppError.PendingOrderExists
            "too many orders" in lower -> AppError.TooManyPendingOrders
            "cannot accept payment" in lower -> AppError.OrderExpired
            "activation not found" in lower -> AppError.NotFound
            "wallet not found" in lower -> AppError.WalletMissing
            "service not found" in lower -> AppError.ServiceUnavailable
            "payment is not configured" in lower -> AppError.PaymentUnavailable
            "failed (4" in lower -> AppError.ProviderUnavailable
            "invalid login" in lower || "invalid_credentials" in lower -> AppError.InvalidCredentials
            "email not confirmed" in lower -> AppError.EmailNotConfirmed
            "already registered" in lower || "already exists" in lower || "user_already_exists" in lower ->
                AppError.EmailAlreadyRegistered
            ("weak" in lower && "password" in lower) || "password should" in lower -> AppError.WeakPassword
            "rate limit" in lower || "too many" in lower || "over_request_rate_limit" in lower -> AppError.RateLimited
            "jwt" in lower || "not authenticated" in lower || "refresh_token" in lower ||
                "session" in lower || "authentication required" in lower -> AppError.SessionExpired
            else -> AppError.Unknown
        }
    }

    fun toUserMessage(error: Throwable?): String = classify(error).message(AppLocale.isArabic)

    private fun containsArabic(text: String): Boolean = text.any { it in '\u0600'..'\u06FF' }
}
