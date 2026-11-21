# تطبيقات 21-11-2026 (طُبّقت يدوياً في Supabase SQL Editor - الإنتاج)

ترتيب التشغيل إذا أُعيد بناء قاعدة من الصفر:
1. `01_unique_deposit_amount.sql`
2. `02_attach_matched_deposit.sql`
3. `04_owner_auto_approve_config.sql`
4. `03_sham_auto_approve_wrapper.sql` (يعتمد على 4 و على الدالة الداخلية الموجودة أصلاً)

الميزات:
- الاعتماد التلقائي للمدير معطّل افتراضياً (`enabled = false`).
- سعر الصرف: `public.app_settings.syp_usd_rate = 16000`.
- Edge Function: `supabase/functions/verify-binance-deposit/index.ts` (منشورة).

تنبيه: لا تشغّل `supabase db push` قبل مطابقة سجل الترحيلات مع القاعدة الحية (انظر SUPABASE_LIVE_DRIFT_REPORT.md).
