# Virtual Numbers (5SIM) integration

## Added
- `app/src/main/java/com/numberone/numberone/domain/model/VirtualNumberModels.kt` — typed country, service, operator quote, pricing and activation DTOs.
- `domain/repository/VirtualNumberRepository.kt` and `data/repository/VirtualNumberRepositoryImpl.kt` — repository contract and implementation.
- `data/remote/FiveSimDataSource.kt` — authenticated caller for the existing `fivesim-api` Edge Function; the 5SIM token is never in Android.
- `presentation/screens/virtualnumbers/VirtualNumbersScreen.kt` — Glacier Mint Compose screen with country name/ISO/dial-code search, popular service shortcuts, live quotes, delivery-rate auto operator choice, percentage/fixed markup, wallet-funded purchase, activation polling, copy actions, same-activation next-SMS refresh and cancellation.
- `notifications/VirtualNumberMessagingService.kt` — FCM data-message notification renderer and token-rotation registration.
- `supabase/functions/fivesim-api/index.ts` — extended source for the existing `fivesim-api` function; retains legacy `countries`, `prices`, `products`, and admin `profile` actions and adds activation/purchase actions.
- `supabase/functions/five-sim-push/index.ts` — scheduled 5SIM poller, FCM sender, and wallet timeout/cancellation refund settlement.
- `supabase/history/20261120150000_add_virtual_numbers_5sim.sql` — new private operation/settings/token tables and service-role-only RPCs integrated with existing `wallets` / `wallet_transactions`; tracks active activations through `RECEIVED` until expiry and avoids crediting uncollected manual-review deltas.
- `backend/SUPABASE_VIRTUAL_NUMBERS.md` — live Supabase findings, Android Edge Function payloads, RPC contracts, deployment/secrets, 5SIM endpoints, and verification notes.

## Updated
- `di/AppContainer.kt` — registers `VirtualNumberRepository`.
- `presentation/navigation/AppRoutes.kt`, `AppNavGraph.kt`, `HomeScreen.kt` — navigation route and home tile.
- `app/src/main/AndroidManifest.xml` — notification permission and Firebase Messaging service registration.
- `app/build.gradle.kts`, root `build.gradle.kts`, and `gradle/libs.versions.toml` — Firebase Messaging and conditional Google Services plugin support.
- `supabase/config.toml` — function JWT settings for the existing `fivesim-api` and the scheduled poller.

## Live backend inspection

Read-only inspection of the connected Supabase project identified an active `fivesim-api` v1 Edge Function that currently has only `countries`, `prices`, `products`, and admin `profile` actions. The `public.wallets` and `public.wallet_transactions` tables and current wallet RPCs exist. No `virtual_numbers_*` RPCs or virtual-number operational tables were present at inspection time. The new migration and updated Edge Function source are therefore included for review/deployment; they have **not** been applied to or deployed over the live project.

The deployed `fivesim-api` source expects the `FIVESIM_API_KEY` Edge Function secret. The value is not exposed or included in this archive. The 5SIM provider key is not read through an app-callable RPC.

## Validation

- Supabase read-only inspection: confirmed existing `fivesim-api`, wallet table columns, wallet ledger columns, and current wallet RPCs. Confirmed no `virtual_numbers_*` RPCs in the live function catalog before this update.
- Edge Function TypeScript check: passed using TypeScript 5.8.3 with ambient Deno/URL-module declarations (Deno itself is not installed here).
- `gradle/libs.versions.toml`: TOML parse passed.
- Android manifest: XML validation passed.
- Kotlin parser/static review: no Kotlin syntax diagnostics in changed sources using the available Kotlin 1.9 parser; a complete type-check still requires the project's Kotlin 2.3.21 Gradle/Android/Compose/Supabase/Firebase dependency classpaths.
- `:app:assembleDebug`: attempted but could not complete because the environment has no configured Android SDK and the Gradle wrapper could not download Gradle 9.6.1 (`UnknownHostException: services.gradle.org`). This archive is source-ready, but an APK build was not verified here.

## Important deployment prerequisites

Apply the included SQL migration before enabling purchases. It uses the existing wallet and ledger, reserves balances atomically, uses an idempotency key before calling 5SIM, and keeps uncertain provider outcomes pending for reconciliation rather than risking a duplicate purchase or blind refund. Configure the provider-to-wallet conversion and markup in `private.virtual_number_settings`. Deploy the extended `fivesim-api` source only after reviewing the migration; it preserves the legacy API actions.

Background FCM requires your Firebase `google-services.json`, a service-account secret, and the `five-sim-push` scheduled function. Because the provider is polled once per minute rather than pushing webhooks, background delivery is best-effort, not truly instantaneous.
