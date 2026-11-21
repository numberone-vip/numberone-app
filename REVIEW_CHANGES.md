# Review pass: build fixes, virtual-number UX, and notifications

## Build fixes
- `data/remote/FiveSimDataSource.kt`: added the missing `kotlinx.serialization.Serializable` import. This was a compile error.
- `AndroidManifest.xml` verified: application class, launcher `MainActivity` (exported), FCM service (`exported=false`, `MESSAGING_EVENT`), permissions (INTERNET, ACCESS_NETWORK_STATE, POST_NOTIFICATIONS), and `AppTheme` all resolve. Added `default_notification_channel_id` so FCM messages use the SMS channel.
- All XML resources parse, and every `libs.*` accessor in the Gradle files resolves against `libs.versions.toml`.
- The Google Services plugin is applied only when `app/google-services.json` exists. The project builds without it; push delivery stays inactive until the file is added.

## Virtual numbers (5SIM)
- Purchase and load buttons no longer stay in a spinner for several seconds after success. Snackbars are launched instead of awaited, because `showSnackbar` suspends until dismissal.
- Backend errors are mapped to short Arabic messages (no stock, insufficient balance, pending reconciliation, expired session). Raw exception text is no longer shown.
- Foreground polling pauses while the app is in the background, so 5SIM is not polled from a backgrounded screen.
- Cancellation is rethrown in the repository guard, so leaving the screen stops its work at once.
- Expiry timestamps are parsed with `OffsetDateTime`, which accepts `Z` and `+00:00` offsets. The old `Instant.parse` path could fail on some runtimes.
- Redesigned screen: balance header, horizontal service chips, a searchable country sheet with flags, an operator dropdown, a price summary showing the balance after purchase, a shortfall notice with a top-up action, and an activation card with a large OTP display, live expiry countdown, and status chips.
- Country selection uses a searchable bottom sheet instead of a dropdown listing about 190 countries.

## Notifications
- One helper (`notifications/SmsNotifications.kt`) builds every SMS notification. The OTP is kept off the lock screen (`VISIBILITY_PRIVATE`). Tapping a notification opens the app.
- Foreground polling and FCM use the same notification ID per activation, so they replace each other instead of stacking.
- The push title and body are written in the app's language. The server's English body is ignored, so no backend redeploy is needed.
- The FCM token is registered on every Home visit (after sign-in), not only when the VN screen opens. Token rotation is still handled in the service.
- `PushTokenRegistrar` is a no-op when Firebase is not configured, so the app runs without `google-services.json`.

## Strings
- Added Arabic `please_wait` and `retry`. Previously Arabic users saw English on loading and error states.

## Still needed from you
1. Place your Firebase `app/google-services.json` (for package `com.numberone.numberone`). It was not in the upload. Background push stays off until it's there.
2. Build with `./gradlew assembleDebug` (or Android Studio sync and run). This review was static: no Gradle, Android SDK, or Kotlin compiler was available in the review environment.

## Known product limits (unchanged)
- The Binance top-up flow does not show the platform deposit address; the data layer does not expose one.
- Sham Cash is not purchasable through the store until a payment setting exists.
