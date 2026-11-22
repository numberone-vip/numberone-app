# ترحيلات Supabase — وضع المجلدات (المرحلة 2)

## لماذا أُعيد ترتيب المجلدات؟
القاعدة الحية فيها 41 ترحيلاً، والمحلي كان فيه 6 ملفات فقط (غير قابلة لإعادة التشغيل). أي `supabase db push`
من مجلد `migrations/` القديم كان خطراً بشكل خاص لأن ملف 5SIM الأول يستخدم `create or replace function`،
وهذا كان سيستبدل تعريفات حية أحدث (`virtual_numbers_reserve_wallet` و`virtual_numbers_commit_purchase`)
بنسخ أقدم تفتقد تتبع المحاولات وتثبيت الهامش وقت الحجز.

لذلك `supabase/migrations/` الآن **فارغ عمداً** (فيه `.gitkeep` فقط): لا يوجد ما يمكن تطبيقه بالخطأ.

## أين ذهبت الملفات؟
| المسار الجديد | الحالة |
|---|---|
| `history/20261120150000_add_virtual_numbers_5sim.sql` | مرجع تاريخي فقط. الإصدار الحي المقابل اسمه `20261120181738_add_virtual_numbers_5sim`. **لا يُطبَّق.** |
| `pending_review/20261121120000_admin_wallet_adjust.sql` | غير مطبّق (حسب تقرير المحاذاة). ينشئ `admin_adjust_wallet_balance` (4 معاملات) و`admin_list_admin_ids`. |
| `pending_review/20261122100000_admin_wallet_ledger.sql` | غير مطبّق. `admin_list_wallet_transactions`. |
| `pending_review/20261122110000_draft_orders_txid_unique.sql` | غير مطبّق. فهرس فريد لـ TXID؛ فيه فحوص مدمجة تمنع التنفيذ إن تعارض شيء. |
| `pending_review/20261122120000_admin_reset_subscription_access.sql` | غير مطبّق. |
| `pending_review/20261123100000_admin_set_admin_role.sql` | جديد (المرحلة 3.3). يحتاج `public.admin_users(user_id)`. |
| `pending_review/20261123110000_admin_audit_log.sql` | جديد (المرحلة 3.5). مستقل عن الباقي. |
| `optional/PENDING_REVIEW_config_services_subscriptions.sql` | **لا يُطبَّق: تجاوزته الدوال الحية.** مثلاً يعرّف `admin_delete_service(p_id uuid)` بينما `services.id` نصي في الحي، ويعرّف نسخة من `admin_adjust_wallet_balance` بثلاثة معاملات تتعارض مع نسخة الأربعة. |

تنبيه: تقرير الانحراف يذكر أن ثلاث دوال إدارية «مفحوصة» (منها `admin_adjust_wallet_balance`)، بينما تقرير المحاذاة
يقول إن ترحيلاتها لم تُطبَّق. لا أستطيع الجزم من الأرشيف، لذلك أُضيف سكربت قراءة فقط يحسم ذلك.

## الخطوات الموصى بها (بالترتيب)
1. في Supabase SQL Editor شغّل كل استعلام في `scripts/CHECK_PENDING_AGAINST_LIVE.sql` (قراءة فقط) وأرسل النتائج.
   النتيجة 1 هي القائمة الحية الدقيقة للترحيلات، والباقي يحدد أي ملف في `pending_review/` مطبّق فعلاً.
2. (اختياري، على جهازك) ثبّت Supabase CLI ثم: `supabase login` و`supabase link --project-ref rlxcsnposxvsyhwtmgbz`
   ثم `supabase migration list` لمقارنة المحلي بالحي. لجلب نصوص الترحيلات الحية المفقودة جرّب
   `supabase migration fetch` (متوفر في الإصدارات الحديثة؛ تأكد بـ `supabase migration --help`).
   وإن لم يتوفر: `supabase db pull` ينشئ ملف baseline من المخطط الحي (يحتاج Docker).
3. بعد الحصول على baseline موثوق: ضعه في `migrations/` باسم يطابق إصداراً حياً، ثم انقل إليه من
   `pending_review/` فقط ما ثبت أنه غير مطبّق، بترتيب أرقامه، بعد مراجعة كل ملف.
4. لا تستخدم `supabase migration repair` ولا `supabase db push` قبل أن تكون القائمتان متطابقتين،
   فالأول يغيّر سجل الترحيلات فقط (دون تطبيق SQL) والثاني يطبّق فعلاً.
5. خذ نسخة احتياطية أو لقطة من القاعدة قبل تطبيق أي ملف على الحي.

## ما لم يُفعل
لم يُنفَّذ أي SQL ولا نشر أي دالة، ولم يُنشأ baseline لأنه يحتاج وصولاً للقاعدة الحية (غير متاح هنا).
