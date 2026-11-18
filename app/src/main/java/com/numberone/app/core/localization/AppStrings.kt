package com.numberone.app.core.localization

enum class AppLanguage(val code: String, val isRtl: Boolean) {
    ARABIC("ar", true),
    ENGLISH("en", false)
}

data class Strings(
    val brandName: String,
    val tagline: String,
    val navStore: String,
    val navOrders: String,
    val navSupport: String,
    val navAdmin: String,
    val adminOnlineStatus: String,
    val walletBalance: String,
    val topUp: String,
    val searchHint: String,
    val filterAll: String,
    val guaranteeBadge: String,
    val orderNow: String,
    val copy: String,
    val copied: String,
    val statusPending: String,
    val statusApproved: String,
    val statusRejected: String,
    val statusSmsReady: String,
    val uploadReceipt: String,
    val submitOrder: String,
    val otpCodeLabel: String,
    val tapToCopyCode: String,
    val privacyPolicy: String,
    val termsOfUse: String,
    val faq: String,
    val contactSupport: String,
    val switchLanguage: String
)

object AppStrings {
    val arabic = Strings(
        brandName = "Number One",
        tagline = "YOUR NUMBERS. YOUR CONTROL.",
        navStore = "المتجر",
        navOrders = "طلباتي",
        navSupport = "الدعم",
        navAdmin = "الإدارة",
        adminOnlineStatus = "الإدارة متصلة • متوسط المراجعة 5 دقائق",
        walletBalance = "رصيد المحفظة",
        topUp = "شحن الرصيد",
        searchHint = "ابحث عن تطبيق أو دولة...",
        filterAll = "الكل",
        guaranteeBadge = "ضمان استرداد الرصيد تلقائياً في حال عدم وصول الكود",
        orderNow = "اطلب الرقم الآن",
        copy = "نسخ",
        copied = "تم النسخ!",
        statusPending = "قيد المراجعة",
        statusApproved = "مقبول",
        statusRejected = "مرفوض",
        statusSmsReady = "وصل الكود",
        uploadReceipt = "إرفاق إيصال الدفع",
        submitOrder = "تأكيد وإرسال الطلب",
        otpCodeLabel = "كود التفعيل",
        tapToCopyCode = "اضغط لنسخ الكود",
        privacyPolicy = "سياسة الخصوصية",
        termsOfUse = "شروط الاستخدام",
        faq = "الأسئلة الشائعة",
        contactSupport = "تواصل مع الدعم",
        switchLanguage = "English"
    )

    val english = Strings(
        brandName = "Number One",
        tagline = "YOUR NUMBERS. YOUR CONTROL.",
        navStore = "Store",
        navOrders = "Orders",
        navSupport = "Support",
        navAdmin = "Admin",
        adminOnlineStatus = "Admin Online • Avg. review ~5 mins",
        walletBalance = "Wallet Balance",
        topUp = "Top Up",
        searchHint = "Search app or country...",
        filterAll = "All",
        guaranteeBadge = "100% Balance Refund Guarantee if no SMS arrives",
        orderNow = "Order Number",
        copy = "Copy",
        copied = "Copied!",
        statusPending = "Pending Review",
        statusApproved = "Approved",
        statusRejected = "Rejected",
        statusSmsReady = "Code Ready",
        uploadReceipt = "Upload Payment Receipt",
        submitOrder = "Confirm & Submit Order",
        otpCodeLabel = "Activation Code",
        tapToCopyCode = "Tap to copy code",
        privacyPolicy = "Privacy Policy",
        termsOfUse = "Terms of Use",
        faq = "FAQ",
        contactSupport = "Contact Support",
        switchLanguage = "العربية"
    )

    fun get(language: AppLanguage): Strings {
        return if (language == AppLanguage.ARABIC) arabic else english
    }
}
