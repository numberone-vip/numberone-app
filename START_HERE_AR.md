# Number One — ابدأ من هنا

هذه نسخة مصدر المشروع بعد مراجعة 21-11-2026.

## رفع المشروع
1. فك ضغط `NumberOne_V4_REVIEWED_UPLOAD.zip`.
2. ارفع مجلد `Number One` كاملاً إلى مستودع GitHub أو افتحه في Android Studio.
3. احتفظ بملف `app/google-services.json` ضمن نسخة البناء المحلية؛ الملف موجود في الأرشيف لكنه مستثنى من Git بواسطة `.gitignore`.

## بناء APK تجريبي
من داخل مجلد `Number One` نفّذ:

```bash
bash gradlew :app:assembleDebug
```

المسار المتوقع بعد نجاح البناء:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## تنبيه مهم جداً
- لم يتم إنتاج APK في بيئة المراجعة لأن تنزيل Gradle 9.6.1 تعذّر بسبب انقطاع الوصول إلى `services.gradle.org`.
- ملفات توقيع إصدار Release غير موجودة في الأرشيف؛ لا تشارك أو ترفع مفاتيح التوقيع إلى GitHub.
- لا تشغّل `supabase db push` ولا تطبّق ملفات SQL الموجودة قبل مطابقة سجل الترحيلات مع قاعدة Supabase الحية. راجع `SUPABASE_LIVE_DRIFT_REPORT.md` أولاً.
- لم يتم نشر أي Edge Function أو تطبيق أي ترحيل أثناء هذه المراجعة.
