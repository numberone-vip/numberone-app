# المرحلة 3.2 — لوحة الإدارة

- AdminScreen.kt: تحديث بالسحب (PullToRefreshBox) + حالة بحث الإيداعات.
- AdminShamTab.kt: بحث في إيداعات USDT بالبريد/TXID/الحالة (حتى 100 نتيجة).
- AdminOverviewTab.kt: زر «عرض الكل / عرض أقل» لسجل حركات المحافظ (حتى 100 صف).

لم يتغير SQL أو Edge Functions. لم يُبنَ المشروع هنا: نفّذ `bash gradlew :app:assembleDebug`.
المرحلة التالية 3.3: صلاحيات المدير (تحتاج RPC جديد بعد المراجعة).
