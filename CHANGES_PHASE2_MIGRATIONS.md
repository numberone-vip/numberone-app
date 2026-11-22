# المرحلة 2 — تنظيف الترحيلات

- `supabase/migrations/` أصبح فارغاً عمداً (لا يمكن تطبيق شيء بالخطأ عبر `supabase db push`).
- ملف 5SIM الأول → `supabase/history/` (مرجع فقط؛ يحتوي `create or replace function` كان سيستبدل تعريفات حية أحدث).
- الترحيلات الستة غير المطبّقة → `supabase/pending_review/`.
- إضافة `supabase/scripts/CHECK_PENDING_AGAINST_LIVE.sql` (قراءة فقط) لمعرفة ما هو مطبّق فعلاً.
- إضافة `supabase/README_MIGRATIONS_AR.md` بالخطوات والقيود. تحديث مسارات الملفات في ملفات CHANGES.
- لم يُنفَّذ أي SQL ولا نشر؛ لم يُنشأ baseline لأنه يحتاج وصولاً للقاعدة الحية.
