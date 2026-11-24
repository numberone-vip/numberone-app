# تقرير إصلاحات فرع الجاهزية للإنتاج — Number One

تاريخ العمل: 2026-11-24  
الفرع: `fix/production-readiness-2026-11`  
الفرع الأصلي المحفوظ: `main`  
نقطة البداية: `c11ac9a15299fb999cf223d2b12c54455ed7fb3b`

## حماية النسخة الأصلية
- أُنشئ فرع مستقل من commit محدد على `main`.
- لم تُعدّل ملفات الفرع الأصلي.
- لم تُحذف أو تُنقل أو تُستبدل ملفات أيقونة التطبيق أو ملفات الهوية البصرية.
- لم يُنشر أي تغيير لقاعدة البيانات أو Edge Functions إلى الإنتاج.

## الإصلاحات في المصدر
1. أضيف جلب `get_payment_config` إلى مصدر بيانات المحفظة ومستودعها.
2. تُحمّل إعدادات الدفع عند فتح المحفظة ولوحة المستخدم.
3. عنوان BEP20 وبيانات شام كاش لم تعد تعتمد على ثوابت فارغة داخل APK.
4. يُمنع إنشاء طلب إيداع USDT إذا لم يكن عنوان الاستقبال مضبوطاً، مع رسالة واضحة.
5. يُمنع إنشاء طلب شام كاش إذا لم يكن حساب الاستلام مضبوطاً، وتُعرض بيانات الحساب واسم صاحبه من الخادم.
6. صُحح استدعاء الإدارة: قاعدة الإنتاج تعرض `public.admin_update_payment_config` بخمسة معاملات؛ الكود القديم كان يرسل معاملين وبأسماء غير مطابقة. أصبح الاستدعاء يرسل العنوان والحساب والاسم وسعر الصرف ونص الإعلان.
7. أضيفت حقول إعدادات الدفع والإعلان إلى بطاقة الإدارة مع التحقق من الإدخال.
8. عُدّل GitHub Actions ليبني Debug APK على هذا الفرع. بناء Release الموقّع اختياري عند توفر أسرار التوقيع، وإعداد Firebase يستعاد اختيارياً من `GOOGLE_SERVICES_JSON_BASE64`.

## فحوص Supabase الحية — قراءة فقط
- سجل الإنتاج يحتوي على **67 ترحيلاً** وقت الفحص؛ التقارير الأقدم التي تذكر 41 أصبحت قديمة.
- `public.get_payment_config()` موجود ويعيد إعدادات الدفع.
- توقيع `public.admin_update_payment_config`:
  `(p_binance_wallet_address text, p_sham_cash_account text, p_sham_cash_name text, p_syp_usd_rate numeric, p_promo_banner_text text)`.
- دوال `admin_adjust_wallet_balance` و`admin_list_wallet_transactions` و`admin_reset_subscription_access` موجودة.
- فحص تكرار TXID النشط أعاد صفراً من التعارضات.
- لم تُنفذ عمليات كتابة SQL، ولم يطبق أي migration، ولم تنشر Edge Function.

## تحذيرات لا يجوز تجاوزها
- ملفات migrations في المستودع لا تمثل كامل تاريخ قاعدة الإنتاج (67 ترحيلاً). لا تشغّل `supabase db push` قبل استعادة ومطابقة التاريخ الكامل.
- أظهر Security Advisor 11 جدولاً عليه RLS بلا سياسات. بعضها في مخطط `private` ومقفول افتراضياً أمام أدوار العميل، بينما `public.app_settings` يُقرأ عبر RPC. يجب مراجعة كل جدول وفق نموذج الوصول الفعلي لا إضافة سياسة عامة.
- أظهر Security Advisor 20 دالة SECURITY DEFINER قابلة للاستدعاء من `authenticated`. يجب تدقيق فحوص الصلاحيات داخل كل دالة، لا سحب EXECUTE جماعياً.
- أظهر التحذير أيضاً وجود `pg_net` في مخطط `public`.
- لا يمكن قراءة أسرار GitHub Actions عبر صلاحية المستودع؛ يلزم التحقق من توفر أسرار Firebase والتوقيع في إعدادات Actions.

## إعادة التحقق — 2026-11-24

### GitHub Actions وملفات APK
- تشغيل GitHub Actions: [45418859524](https://github.com/saamabbas83-arch/numberone-app/actions/runs/45418859524).
- الـ commit المبني: `9efa5a11ebec60934f7eac69ab0ceb378d06e3cc`.
- نتيجة المهمة: `success`؛ خطوات الاختبار وبناء Debug وRelease الموقّع ورفع الملفين كلها ناجحة.
- Debug Artifact: `NumberOne-Debug-APK`، المعرّف `14931424631`، الحجم 26,500,057 بايت؛ APK المستخرج 27,571,869 بايت، SHA-256: `2c33a1cf33958d60c98272c6b2b31ab2bee75191b6e42f46117b2f371e43941d`.
- Release Artifact: `NumberOne-Release-APK`، المعرّف `14932399454`، الحجم 18,242,746 بايت؛ APK المستخرج 19,192,887 بايت، SHA-256: `7d264f5884acda556dd5787761ec8b54910d7c97c30396dce493b4733d423753`.
- نُزّل ملفا Artifact وفُك ضغطهما وفُحص APK نفسه: كلاهما يحمل package name `com.numberone.numberone` وversionName `1.1.0` وversionCode `2`، ويتضمن جدول الموارد مراجع `ic_launcher` و`ic_launcher_round`، كما يحتوي كل APK على APK Signing Block. لم يتم التثبيت أو اختبار التشغيل على جهاز فعلي.
- سجل التشغيل يثبت أن `GOOGLE_SERVICES_JSON_BASE64` كان فارغاً؛ لذلك هذان الملفان لا يحتويان تهيئة Firebase/FCM.

### Firebase
- عُثر على `app/google-services.json` في المستودع الخاص `Number`، ولم يُنسخ إلى المستودع العام.
- فُحصت بياناته الوصفية: مشروع Firebase هو `number-one-e78f5`، واسم الحزمة المسجل هو `com.numberone.numberone`، وهو مطابق لـ `applicationId` في Android.
- لا تتوفر صلاحية إدارة GitHub Actions Secrets عبر اتصال GitHub الحالي؛ يلزم إضافة السر `GOOGLE_SERVICES_JSON_BASE64` يدوياً من إعدادات المستودع، ثم إعادة تشغيل البناء. لم تُكشف قيمة الملف أو مفتاحه في التقرير أو في المستودع العام.

### تحسين سير العمل
- أُحدّث `.github/workflows/build-release.yml` على فرع الإصلاح فقط في commit `982163fc1deb7b087d8600e57e6c4d0a5fb164e1`.
- يتحقق سير العمل الآن من JSON الخاص بـ Firebase واسم حزمة Android قبل البناء، ويُفشل البناء عند وجود ملف غير صالح أو حزمة غير مطابقة.
- يضيف تحققاً من package name وأيقونة launcher داخل APK، ويُظهر حالة Firebase والتوقيع والاختبارات في ملخص التشغيل.
- رُفع إصدار `actions/setup-java` إلى v5 لإزالة تحذير الإهمال الذي ظهر في التشغيل السابق.
- نتائج CI لهذا التعديل لم تُعلن بعد؛ يجب انتظار تشغيل الفرع بعد هذا التغيير قبل اعتباره متحققاً.

### قاعدة البيانات
- إعادة الفحص الحي أكدت 67 migration في `supabase_migrations.schema_migrations`، و23 جدولاً مفعلاً عليه RLS (13 public و10 private).
- Security Advisor أعاد 11 جدولاً بلا سياسات RLS؛ تشمل جداول `private` المقصود أن تكون غير مكشوفة للعميل، و`public.app_settings` و`public.hexz_catalog_cache`. لا تُضاف سياسات عامة تلقائياً.
- Advisor أعاد 20 دالة SECURITY DEFINER قابلة للاستدعاء من authenticated. فحص ACL الحي أكد أن الدوال الإدارية المفحوصة لا تسمح بـ anon، وأن عمليات الحجز/التأكيد/الاسترداد الداخلية الخاصة بالأرقام الافتراضية محجوبة عن anon وauthenticated. تبقى مراجعة منطق تفويض المدير داخل كل دالة مطلوبة قبل أي تغيير.
- لا تزال تحذيرات `pg_net` في public وحماية كلمات المرور المسربة معطلة قائمة.
- وظيفة `five-sim-push` نشطة (الإصدار 32، `verify_jwt=false` مع تحقق Bearer داخلي). سجل `function_edge_logs` أظهر استدعاءات مجدولة عبر `pg_net` كل دقيقة تقريباً بين 14:37 و15:06 UTC بتاريخ 2026-11-24، وكلها HTTP 200. هذا يؤكد وصول المجدول واجتياز حواجز 401/503/500 في تلك الدورات، لكنه لا يثبت إرسال إشعار إلى جهاز مستخدم؛ لا يظهر عدد الرسائل من سجلات HTTP وحدها.
- مصدر الوظيفة يحدد أسرارها المطلوبة: `FIVE_SIM_PUSH_CRON_SECRET` و`FCM_SERVICE_ACCOUNT_JSON` و`FIVESIM_API_KEY` (إضافة إلى `SUPABASE_URL` و`SUPABASE_SERVICE_ROLE_KEY` المقدمين للوظيفة). لا توجد أداة متاحة لقراءة قيم الأسرار، ولم تُعرض أو تُعدّل.
- لم تُنفذ أي كتابة SQL أو migrations أو تغييرات صلاحيات أو نشر Edge Functions في هذا العمل.

## المطلوب لإكمال Firebase
من جهاز موثوق لديه نسخة الملف الخاص:
```bash
base64 -w0 app/google-services.json
```
انسخ الناتج إلى GitHub Repository → Settings → Secrets and variables → Actions → New repository secret باسم:
`GOOGLE_SERVICES_JSON_BASE64`
لا ترسل محتوى الملف أو قيمة السر في المحادثة ولا ترفع الملف إلى المستودع العام.
