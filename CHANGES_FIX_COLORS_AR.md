# إصلاح ألوان الثيم المفقودة

## المشكلة
ملف `core/theme/Color.kt` لا يحتوي على بعض الألوان التي تستخدمها الشاشات والمكونات:
`NavyHeader`, `IndigoDeep`, `IndigoRoyal`, `ButtonPurple`, `Gold`.
هذا يمنع تجميع المشروع (Unresolved reference) حتى مع وجود باقي الملفات.

## ما تم
- أضفت الألوان الناقصة في `Color.kt` بقيم متوافقة مع `CHANGES_UI_PHASE1_THEME.md`:
  - `NavyHeader` = #0A0F1F
  - `IndigoDeep` = #1E1B4B
  - `IndigoRoyal` = #4338CA
  - `ButtonPurple` = #6D28D9
  - `IndigoAccent` = #818CF8
  - `Gold` = `Champagne` (#E8CD8A)
- لم يتم تغيير أي منطق أو Repository أو RPC.

## ملاحظات
- الثيم الحالي في `Theme.kt` ما زال بلون Obsidian/Champagne، بينما وثيقة Phase 1 تصف هوية Navy/Indigo. القرار بين الهويتين لم يُحسم بعد.
- لم يُختبر التجميع: بيئة العمل لا تحتوي Gradle أو Android SDK. يلزم تشغيل `bash gradlew :app:assembleDebug` محلياً.
