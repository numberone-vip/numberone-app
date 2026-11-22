# تسليم الجلسة القادمة — ربط Supabase

## قبل الجلسة (على جهازك)
1. ارفع هذا المشروع إلى GitHub (الملف الأحدث `NumberOne_Phase5_Quick.zip`).
2. شغّل: `bash gradlew :app:testDebugUnitTest` ثم `bash gradlew :app:assembleDebug`، واحتفظ بنص أول خطأ إن ظهر.
3. فعّل موصّل Supabase في Claude (الإعدادات ← الموصّلات) وسجّل الدخول لمشروعك `rlxcsnposxvsyhwtmgbz`.
4. خذ لقطة/نسخة احتياطية من القاعدة قبل أي تغيير (لوحة Supabase ← Database ← Backups).

## ترتيب العمل المقترح في الجلسة (Supabase)
1. **قراءة فقط أولاً** (لا تغيير): `supabase/scripts/CHECK_PENDING_AGAINST_LIVE.sql` ثم `EXPORT_LIVE_SCHEMA.sql`.
2. **المرحلة 2:** حسم ما هو مطبّق من `supabase/pending_review/` وبناء baseline موثوق (راجع `supabase/README_MIGRATIONS_AR.md`).
3. **المرحلة 1 (الأمان):**
   - تفعيل حماية كلمات المرور المسرّبة (إعداد في Auth، ليس SQL).
   - نقل إضافة `pg_net` من مخطط `public`.
   - مراجعة `public.app_settings` (RLS مفعّل بلا سياسات).
   - مراجعة `purchase_manual_service_with_wallet`: منع تكرار الخصم + كيفية حفظ `p_access_code`.
4. **تطبيق الملفات المعلّقة** بعد مراجعة كل ملف، واحداً واحداً، بترتيب أرقامها:
   `admin_wallet_adjust` ← `admin_wallet_ledger` ← `draft_orders_txid_unique` ← `admin_reset_subscription_access`
   ← `admin_set_admin_role` ← `admin_audit_log`.
   (التطبيق جاهز لها: بدونها تظهر تنبيهات في لوحة الإدارة فقط.)
5. **المرحلة 4 (Sham Cash):** إعداد `payment_settings` وRPCs للشام كاش (ليرة/دولار) ثم تفعيل الواجهة.

## حالة التطبيق الآن
- لوحة الإدارة: 6 تبويبات (نظرة عامة + بطاقة آخر 7 أيام، الطلبات، شام كاش والإيداعات، الخدمات + هامش 5SIM، المستخدمون + الصلاحيات، سجل الإدارة).
- لم يُبنَ المشروع قط في بيئة Claude؛ كل ما سبق مراجعة يدوية. أخطاء الترجمة المحتملة ستظهر عند أول بناء عندك.

## ممنوع قبل المطابقة
`supabase db push` و`supabase migration repair` و`supabase db reset` — راجع `SUPABASE_LIVE_DRIFT_REPORT.md`.
