# Changes in this revision
See the chat summary. Static review only: assembleDebug could not be run (no Android SDK / network in the review sandbox).
Signing files (release.keystore, release.properties) are intentionally excluded; .gitignore now blocks them.

---

# UI/UX redesign and stabilization pass

## Bug fixes
- **Admin summary decode failure.** `AdminWalletDeposit.requestedAmount` was a non-null `Double` with no default, so a null from the server failed the whole summary. All admin, wallet, order, service and sham-cash DTO fields now have safe defaults. `rpcJson` adds `explicitNulls = false` alongside `coerceInputValues` and `ignoreUnknownKeys`.
- **JVM declaration clash in `AppViewModel`.** `var accessCode ... private set` existed alongside `fun setAccessCode`, and the same pattern applied to `amount`, `txHash`, `shamMethod` and `shamAmount`. Inputs now use private `_backing` properties with public getters and explicit setter functions.
- **Raw error leaks.** `ErrorMapper` (`core/error/ErrorMapper.kt`) converts every throwable into a short Arabic or English message. Raw URLs, headers, JSON bodies and stack traces are never shown. `AppViewModel`, `LoginViewModel` and `AdminViewModel` route all errors through it. `CancellationException` is rethrown instead of being shown as an error.
- **Admin summary counts.** `getSummary()` now also reports pending deposits from the same response.

## Redesign
- Material 3 theme tokens for Glacier Mint: explicit `primaryContainer`, `surfaceVariant` and `outline` values replace Material's default purple tints.
- Shared kit in `core/components/NumberOneComponents.kt`: `NOScaffold`, `NOCard`, `NOButton`, `BalanceHeroCard`, `ActionTile`, `StatusChip` (green, amber and red tones), `CopyField`, `ExpiryBadge`, `ReceiptConfirmCard`, `NoticeBanner` and `FeedbackBanner`.
- Screens moved out of `AppNavGraph` into `presentation/screens/*`: Login, Signup and Access Code (auth), Home, Wallet (Binance and Sham Cash tabs, receipt preview before upload), Store (`CheckoutScreen` service cards), Orders, Subscriptions, SMS Inbox (one-tap copy of detected codes), Settings, Support and Admin.
- Admin dashboard: `PrimaryTabRow` with Summary (KPI cards), Sham Cash and Orders review cards, a zoomable receipt viewer (pinch to 5x), and approve and reject dialogs. Approve and reject are also confirmed by the dialog step.
- `MainActivity` calls `enableEdgeToEdge()` and keeps `AppLocale` in sync with the selected language.
- Removed the dead `LoginScreen` duplicate and the inline `PageFrame`-based pages from `AppNavGraph`.

## Not verified
- **Build not run.** The review environment has a JDK but no Gradle distribution, no Android SDK, no Kotlin compiler and no network. Compile correctness was checked only structurally: bracket balance across all 70 Kotlin files, a heuristic import resolution pass, and icon-import verification. Run `./gradlew assembleDebug` before shipping.
- **Wallet deposit address.** The data layer exposes no Binance deposit address. The wallet shows the BEP20 address only when a pending store order provides one. A dedicated RPC is needed to show it reliably.
- **Sham Cash in the store.** Still unavailable for purchases, per `AUDIT_REPORT.md`. It is shown as a top-up method only.
- **Language.** Most UI copy remains Arabic-only, as before. Error text and status labels follow the selected language.
