# UI Phase 2 - English language switching + label cleanup

(Cumulative: includes Phase 1 theme and top notifications.)

1. Localization: every Arabic UI string now goes through `tr("...")` / `trf("... {0} ...", args)`
   (core/localization/AppStrings.kt). Translations are in core/localization/Translations.kt
   (532 entries). Switching to English in Settings updates all screens, tabs, buttons and labels
   immediately, and the layout switches to LTR.
2. The app-wide language is kept in sync from NumberOneApplication, so notifications and
   ViewModel messages follow the selected language too.
3. "API" removed from the Virtual Numbers tab and header: "أرقام فورية" / "Instant Numbers".
4. Enums (PasswordStrength, OrderFilter) read their labels on each use so they follow the language.
5. Matching keywords (CountryFlag, status keys) stay Arabic/English as data; only display text is translated.

Not changed: backend, RPC names, repositories' logic, server status keys.
Note: Toast/notification text not covered by Compose may still appear in Arabic in rare system messages.
