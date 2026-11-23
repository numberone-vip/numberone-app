package com.numberone.numberone.core.legal

import com.numberone.numberone.core.localization.AppLanguage

/**
 * Terms of Service and Privacy Policy text, shown from the login screen.
 *
 * DRAFT: written for the app's current behaviour. Before publishing, replace the
 * bracketed placeholders ([اسم الشركة] / [Company name], nuberonsy@gmail.com / [support email],
 * الجمهورية العربية السورية / [governing law]) and have a lawyer review the text.
 */
data class LegalSection(val heading: String, val body: String)

data class LegalDocument(
    val title: String,
    val updated: String,
    val sections: List<LegalSection>
)

object LegalDocuments {
    fun terms(language: AppLanguage): LegalDocument = if (language.isArabic) TERMS_AR else TERMS_EN
    fun privacy(language: AppLanguage): LegalDocument = if (language.isArabic) PRIVACY_AR else PRIVACY_EN

    private val TERMS_AR = LegalDocument(
        title = "الشروط والأحكام",
        updated = "آخر تحديث: 23 نوفمبر 2026",
        sections = listOf(
            LegalSection(
                "1. القبول",
                "إن وصولك إلى تطبيق Number One أو استخدامك لأيٍّ من خدماته يُعدّ إقراراً صريحاً وموافقةً كاملةً منك على الالتزام بهذه الشروط والأحكام. في حال عدم موافقتك على أي بند، يُرجى التوقف فوراً عن استخدام التطبيق."
            ),
            LegalSection(
                "2. الحساب",
                "يلتزم المستخدم بتقديم بيانات دقيقة وصحيحة عند التسجيل، ويتحمل المسؤولية الكاملة عن الحفاظ على سرية بيانات الدخول وكلمة المرور، وعن كافة الأنشطة التي تُجرى عبر حسابه، ويُحظر مشاركة الحساب مع أي طرف ثالث."
            ),
            LegalSection(
                "3. الأرقام الافتراضية",
                "يوفّر التطبيق أرقاماً افتراضية مؤقتة مخصصة لاستقبال رسائل التحقق النصية (OTP) من المنصات الرقمية. يخضع كل رقم لمدة صلاحية محددة، ولا تضمن إدارة التطبيق قبول جميع المنصات الخارجية لهذه الأرقام."
            ),
            LegalSection(
                "4. الرصيد والدفع",
                "يُستخدم الرصيد المودع حصراً لشراء الخدمات داخل التطبيق. تخضع كافة عمليات الشحن والتحويل المالي (بما في ذلك USDT والوسائل المعتمدة) للمراجعة والتحقق قبل إضافتها، وتُرفض المعاملات الخاطئة أو المكررة. يُطبّق الاسترداد وفق السياسة المعلنة داخل التطبيق أو عند ثبوت خلل تقني من جانبنا."
            ),
            LegalSection(
                "5. الاستخدام المقبول",
                "يُحظر حظراً باتاً استخدام التطبيق في أي أنشطة مخالفة للقوانين النافذة، أو في أعمال الاحتيال وانتحال الشخصية، أو محاولة اختراق البنية التقنية للتطبيق أو تعطيل خوادمه."
            ),
            LegalSection(
                "6. تعليق الحساب وإنهاؤه",
                "تحتفظ إدارة التطبيق بالحق الكامل في تعليق أو إنهاء أي حساب يثبت مخالفته لهذه الشروط، مع السعي لإخطار المستخدم متى ما أمكن ذلك."
            ),
            LegalSection(
                "7. إخلاء المسؤولية",
                "تُقدَّم خدمات التطبيق «كما هي» ووفقاً للتوافر التقني، ولا تتحمل إدارة التطبيق أي مسؤولية عن الأضرار غير المباشرة إلى أقصى حد يسمح به القانون."
            ),
            LegalSection(
                "8. التعديلات",
                "يحق لإدارة التطبيق تحديث هذه الشروط والأحكام في أي وقت، ويُعدّ استمرارك في استخدام التطبيق بعد نشر التعديلات موافقةً نهائيةً على النسخة المحدّثة."
            ),
            LegalSection(
                "9. القانون والتواصل",
                "تخضع هذه الشروط والأحكام وتُفسَّر وفقاً للقوانين المعمول بها في الجمهورية العربية السورية. للتواصل الرسمي والدعم: nuberonsy@gmail.com"
            )
        )
    )

    private val TERMS_EN = LegalDocument(
        title = "Terms and Conditions",
        updated = "Last updated: November 23, 2026",
        sections = listOf(
            LegalSection(
                "1. Acceptance",
                "By using the Number One app you agree to these terms. If you do not agree, do not use the app."
            ),
            LegalSection(
                "2. Your account",
                "You are responsible for the accuracy of your account details, for keeping your password secret, and for all activity on your account. Do not share your account with others."
            ),
            LegalSection(
                "3. Virtual numbers",
                "The app provides temporary numbers to receive verification messages from third-party services. Each number is valid for a limited time. We do not guarantee that every service will accept these numbers."
            ),
            LegalSection(
                "4. Balance and payments",
                "Your balance can only be used inside the app. Payments by transfer (such as USDT) or other available methods are verified before they are credited, and incorrect or duplicate transactions may be cancelled. Refunds follow what is stated in the app, or apply when a technical error is on our side."
            ),
            LegalSection(
                "5. Acceptable use",
                "You may not use the app for illegal purposes or fraud, create fake accounts, or attempt to attack or disrupt the service."
            ),
            LegalSection(
                "6. Suspension and termination",
                "We may suspend or terminate an account that breaks these terms. Where possible, we will notify you first."
            ),
            LegalSection(
                "7. Disclaimer",
                "The service is provided as is. We are not liable for indirect losses to the extent permitted by law."
            ),
            LegalSection(
                "8. Changes",
                "We may update these terms from time to time. Continuing to use the app after an update means you accept the updated terms."
            ),
            LegalSection(
                "9. Law and contact",
                "These terms are governed by the laws of the Syrian Arab Republic. Contact: nuberonsy@gmail.com."
            )
        )
    )

    private val PRIVACY_AR = LegalDocument(
        title = "سياسة الخصوصية",
        updated = "آخر تحديث: 23 نوفمبر 2026",
        sections = listOf(
            LegalSection(
                "1. البيانات التي نجمعها",
                "البريد الإلكتروني وبيانات الحساب، وسجل الطلبات والمعاملات والرصيد، وإيصالات الدفع التي ترفعها، والرسائل النصية الواردة إلى الأرقام المخصصة لك لعرضها داخل التطبيق، ومعرّف الإشعارات لإرسال التنبيهات إلى جهازك."
            ),
            LegalSection(
                "2. ما يبقى على جهازك",
                "إعدادات اللغة والمظهر، وإعداد الدخول بالبصمة. عند تفعيل البصمة تُحفظ بيانات الدخول مشفّرة على جهازك فقط، ولا تُفتح إلا بالبصمة. بيانات البصمة نفسها لا تُرسل إلينا، بل يتحقق منها نظام جهازك."
            ),
            LegalSection(
                "3. كيف نستخدم البيانات",
                "لتشغيل الخدمة، والتحقق من المدفوعات، وإرسال التنبيهات، ومنع الاحتيال، وتقديم الدعم الفني."
            ),
            LegalSection(
                "4. مشاركة البيانات",
                "نشارك البيانات فقط مع مزودي الخدمات التقنية الذين نحتاجهم لتشغيل التطبيق، مثل الاستضافة والمصادقة وقاعدة البيانات ومزود الأرقام وخدمة الإشعارات، وبالقدر اللازم فقط. لا نبيع بياناتك."
            ),
            LegalSection(
                "5. الاحتفاظ بالبيانات",
                "نحتفظ بالبيانات طالما حسابك نشط، ولمدة تقتضيها الالتزامات القانونية والمحاسبية، ثم نحذفها أو نجعلها مجهولة الهوية."
            ),
            LegalSection(
                "6. أمان البيانات",
                "نستخدم وسائل حماية معقولة، لكن لا يوجد نظام آمن تماماً. احرص على حماية كلمة مرورك وجهازك."
            ),
            LegalSection(
                "7. حقوقك",
                "يمكنك الاطلاع على بياناتك وتصحيحها، وطلب حذف حسابك، عبر التواصل معنا."
            ),
            LegalSection(
                "8. الأطفال",
                "الخدمة غير موجهة لمن هم دون 18 سنة، ولا نجمع بياناتهم عن قصد."
            ),
            LegalSection(
                "9. التعديلات والتواصل",
                "قد نحدّث هذه السياسة، وسنعلن التغييرات داخل التطبيق. للتواصل بشأن الخصوصية: nuberonsy@gmail.com."
            )
        )
    )

    private val PRIVACY_EN = LegalDocument(
        title = "Privacy Policy",
        updated = "Last updated: November 23, 2026",
        sections = listOf(
            LegalSection(
                "1. Data we collect",
                "Your email and account details, your order, transaction and balance history, payment receipts you upload, text messages received on your assigned numbers so you can view them in the app, and a notification token to send alerts to your device."
            ),
            LegalSection(
                "2. What stays on your device",
                "Language and theme settings, and the fingerprint sign-in setting. When fingerprint sign-in is on, your sign-in details are stored encrypted on your device only and unlock only with your fingerprint. Fingerprint data itself is never sent to us; your device system checks it."
            ),
            LegalSection(
                "3. How we use data",
                "To run the service, verify payments, send notifications, prevent fraud and provide support."
            ),
            LegalSection(
                "4. Sharing",
                "We share data only with technical providers we need to run the app, such as hosting, authentication, the database, the number provider and the notification service, and only to the extent needed. We do not sell your data."
            ),
            LegalSection(
                "5. Retention",
                "We keep data while your account is active and for as long as legal and accounting obligations require, then delete it or make it anonymous."
            ),
            LegalSection(
                "6. Security",
                "We use reasonable safeguards, but no system is completely secure. Protect your password and your device."
            ),
            LegalSection(
                "7. Your rights",
                "You can view and correct your data, and request deletion of your account, by contacting us."
            ),
            LegalSection(
                "8. Children",
                "The service is not directed at anyone under 18, and we do not knowingly collect their data."
            ),
            LegalSection(
                "9. Changes and contact",
                "We may update this policy and will announce changes in the app. For privacy questions: nuberonsy@gmail.com."
            )
        )
    )
}
