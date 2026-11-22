# Phase 1 — Auth screens

Changed: LoginScreen, SignupScreen (new shared kit in AuthComponents.kt), LoginViewModel, nav wiring.
Untouched: ErrorMapper, AppViewModel.

Before release:
1. Fill `core/util/SupportContacts.kt` (WhatsApp number, Telegram username). Blank = buttons disabled.
2. (Optional) run `supabase/optional/check_email_available.sql` for true duplicate-email rejection when
   "Confirm email" is on. Without it the app relies on Supabase's own "already registered" error.

New dependencies (need network on first sync): androidx.biometric 1.1.0, fragment 1.8.6, appcompat 1.7.0.
MainActivity now extends FragmentActivity; AppTheme parent is Theme.AppCompat.Light.NoActionBar (BiometricPrompt).
The Access Code route/screen remain (still opened from Subscriptions); only the login entry card was removed.
