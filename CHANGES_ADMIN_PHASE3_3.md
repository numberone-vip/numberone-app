# المرحلة 3.3 — صلاحيات المدير

## Android
- AdminRepository/Impl: `setAdminRole(userId, makeAdmin)` يستدعي RPC `admin_set_admin_role`.
- AdminViewModel: `setAdminRole` + `selfId` (لإخفاء الإجراء عن حسابك).
- AdminUsersTab: زر «ترقية إلى مدير / إزالة صلاحية المدير» داخل تفاصيل المستخدم.
- AdminScreen: حوار تأكيد قبل التنفيذ. الزر مخفي إذا كان الدور غير معروف (admin_list_admin_ids غير منشورة).
- ترجمات إنجليزية للنصوص الجديدة (3.1–3.3).

## Supabase (للمراجعة فقط — لم يُطبَّق)
- `supabase/pending_review/20261123100000_admin_set_admin_role.sql`
  - يمنع تغيير دورك بنفسك، وحذف آخر مدير، ويتحقق من وجود المستخدم، ومتكرر الأمان.
  - تحقق قبل التطبيق: وجود `public.admin_users(user_id)` وأنه لا توجد أعمدة إلزامية أخرى.
- يحتاج أيضاً ترحيل `20261121120000_admin_wallet_adjust.sql` (دالة admin_list_admin_ids) ليظهر الدور.
- لا تشغّل `supabase db push` قبل مطابقة سجل الترحيلات (راجع SUPABASE_LIVE_DRIFT_REPORT.md).
