package com.numberone.numberone.core.error

/**
 * Typed, language-independent description of a failure.
 *
 * Classification (what went wrong) lives here and in [ErrorMapper.classify].
 * Presentation (what the user reads) lives in [AppError.message]. Keeping them
 * separate lets us unit-test classification without Android or locale state.
 */
sealed class AppError(val category: Category) {

    enum class Category { NETWORK, AUTH, SESSION, RATE_LIMIT, BUSINESS, VALIDATION, SERVER, UNKNOWN }

    object Network : AppError(Category.NETWORK)
    object ServerResponse : AppError(Category.SERVER)
    object InvalidCredentials : AppError(Category.AUTH)
    object EmailNotConfirmed : AppError(Category.AUTH)
    object EmailAlreadyRegistered : AppError(Category.AUTH)
    object WeakPassword : AppError(Category.AUTH)
    object SessionExpired : AppError(Category.SESSION)
    object RateLimited : AppError(Category.RATE_LIMIT)
    object InsufficientBalance : AppError(Category.BUSINESS)
    object NoStock : AppError(Category.BUSINESS)
    object PendingReview : AppError(Category.BUSINESS)
    object DuplicateRequest : AppError(Category.BUSINESS)
    object PendingOrderExists : AppError(Category.BUSINESS)
    object NotFound : AppError(Category.BUSINESS)
    object ProviderUnavailable : AppError(Category.SERVER)
    object ServiceInUse : AppError(Category.BUSINESS)
    object TooManyPendingOrders : AppError(Category.BUSINESS)
    object OrderExpired : AppError(Category.BUSINESS)

    /** A message that the app itself wrote in Arabic (validation); shown as-is. */
    data class Localized(val text: String) : AppError(Category.VALIDATION)

    object Unknown : AppError(Category.UNKNOWN)
}

/** Carries a typed [AppError] through layers that throw exceptions. */
class AppException(val error: AppError, cause: Throwable? = null) : Exception(error::class.simpleName, cause)

/** An error whose message is already safe and localized for display. */
class UserFacingException(message: String) : Exception(message)

/** Renders an [AppError] for the user in Arabic or English. */
fun AppError.message(arabic: Boolean): String {
    fun t(ar: String, en: String) = if (arabic) ar else en
    return when (this) {
        AppError.Network -> t(
            "تعذر الاتصال بالخادم. تحقق من الإنترنت وحاول مرة أخرى.",
            "Could not reach the server. Check your connection and try again."
        )
        AppError.ServerResponse -> t("تعذر معالجة استجابة الخادم", "Could not read the server response.")
        AppError.InvalidCredentials -> t("البريد الإلكتروني أو كلمة المرور غير صحيحة", "Incorrect email or password.")
        AppError.EmailNotConfirmed -> t("يرجى تأكيد البريد الإلكتروني أولاً", "Please confirm your email address first.")
        AppError.EmailAlreadyRegistered -> t("هذا البريد الإلكتروني مسجل مسبقاً", "This email address is already registered.")
        AppError.WeakPassword -> t("كلمة المرور ضعيفة. استخدم 8 أحرف على الأقل", "Password is too weak. Use at least 8 characters.")
        AppError.SessionExpired -> t("انتهت الجلسة. يرجى تسجيل الدخول مجدداً", "Your session has expired. Please sign in again.")
        AppError.RateLimited -> t("محاولات كثيرة. انتظر قليلاً ثم حاول مجدداً", "Too many attempts. Wait a moment and try again.")
        AppError.InsufficientBalance -> t("رصيد محفظتك غير كافٍ لإتمام هذا الشراء. اشحن المحفظة ثم حاول مجدداً.", "Your wallet balance is not enough for this purchase. Top up your wallet and try again.")
        AppError.NoStock -> t(
            "لا يوجد مخزون لهذا الاختيار حالياً. جرّب مشغلاً أو دولة أخرى.",
            "No stock available for this selection right now. Try another operator or country."
        )
        is AppError.Localized -> text
        AppError.PendingReview -> t(
            "عملية الشراء قيد المراجعة. لن يُخصم المبلغ مرتين، حاول لاحقاً أو تواصل مع الدعم.",
            "This purchase is under review. You will not be charged twice; try again later or contact support."
        )
        AppError.DuplicateRequest -> t(
            "معرّف الطلب مستخدم بتفاصيل مختلفة. أعد المحاولة من جديد.",
            "This request ID was already used with different details. Please try again."
        )
        AppError.PendingOrderExists -> t(
            "لديك طلب دفع سابق لم يكتمل بعد. أكمل الدفع أو انتظر انتهاء مهلته قبل طلب رقم جديد.",
            "You have an unfinished payment order. Complete the payment or wait for it to expire before requesting a new number."
        )
        AppError.NotFound -> t("لم يتم العثور على هذا التفعيل.", "This activation was not found.")
        AppError.ProviderUnavailable -> t(
            "لا توجد أرقام متاحة لهذا الاختيار الآن. جرّب لاحقاً.",
            "No numbers are available for this selection right now. Try later."
        )
        AppError.ServiceInUse -> t(
            "لا يمكن حذف خدمة مرتبطة بطلبات أو اشتراكات. عطّلها بدلاً من حذفها.",
            "This service is linked to orders or subscriptions and cannot be deleted. Disable it instead."
        )
        AppError.TooManyPendingOrders -> t(
            "لديك عدد كبير من الطلبات قيد المراجعة. انتظر حتى تكتمل قبل إنشاء طلب جديد.",
            "You have too many orders under review. Wait for them to complete before creating a new one."
        )
        AppError.OrderExpired -> t(
            "انتهت مهلة الدفع لهذا الطلب. أنشئ طلباً جديداً للمتابعة.",
            "The payment window for this order has expired. Create a new order to continue."
        )
        AppError.Unknown -> t("حدث خطأ غير متوقع. حاول مرة أخرى.", "Something went wrong. Please try again.")
    }
}
