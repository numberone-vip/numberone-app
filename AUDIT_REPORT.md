# Number One Android project — audit and change report

Audit date: 2026-11-20

## Preserved project constraints

- Preserved the original project files and directory tree; no existing files were deleted or renamed.
- Kept the Gradle wrapper at 9.6.1, Kotlin at 2.3.21, and Supabase-kt at 3.9.0.
- Kept the Glacier Mint theme and `AppPreferences` Arabic/English language and theme settings.
- Supabase client uses the connected project's URL and active publishable key. No service-role key is embedded in Android source.

## Main Android changes

- Replaced placeholder Supabase configuration with the connected project configuration and installed the Supabase v3 Auth, PostgREST, Storage, Realtime, and Functions modules.
- Aligned wallet models and RPC calls with the live database responses (`wallet_get_my_summary`, `wallet_get_my_transactions`, `wallet_create_deposit_request`, `wallet_get_my_deposit_requests`, and `wallet_submit_deposit_tx`).
- Added Binance deposit verification through the existing authenticated `verify-binance-deposit` Edge Function.
- Added service listing, wallet purchases, manual USDT checkout, receipt uploads to `payment-receipts`, and payment submission using the live RPC contracts.
- Added authenticated order history, sign-up, login, access-code lookup, SMS inbox, subscriptions, settings, support, and an admin data overview.
- Replaced duplicate wallet use-case declarations and corrected repository/data-source interface alignment.
- Added user-facing Arabic loading, empty, and error states in the connected flows.

## Live Supabase changes executed

1. Granted `authenticated` execute permission on the existing `public.create_order(text)` wrapper.
2. Removed the redundant broad `Customers upload to own payment receipt folder` Storage INSERT policy. The stricter `Customers upload own payment receipts` policy remains and checks the authenticated user's folder, matching order ID, order ownership, `AWAITING_PAYMENT` status, and unexpired order.
3. Updated the existing `private.create_order(text)` SECURITY DEFINER function so the stored payment method is `USDT_BEP20`, matching the live `payment_settings` row (`USDT`, `BEP20`, enabled). Verified the function still has `search_path = ''`, is SECURITY DEFINER, uses `USDT_BEP20`, and no longer contains `USDT_TRC20`.
4. No tables were created or dropped. Existing wallet, order, subscription, and receipt data were not modified.

## Verification performed

- Inspected the live public schema (11 public tables), RPC definitions, SECURITY DEFINER/search_path settings, Storage bucket, and Storage policies.
- Verified the `payment-receipts` bucket is private, has a 5 MiB file limit, and permits JPEG, PNG, and WebP.
- Verified wallet DataSource and Repository interface methods have corresponding implementations.
- Ran a Kotlin compiler parser pass over the project sources; no Kotlin syntax diagnostics (`expecting`, unexpected tokens, redeclarations) were emitted. This is not a full type-check because AndroidX/Compose/Supabase dependencies are not available locally.
- A Gradle build could not be completed: the environment could not resolve `services.gradle.org` to download the pinned Gradle 9.6.1 distribution, and no Android SDK is configured. Therefore this archive is **not represented as a successfully built APK**.

## Known limitations requiring product/backend follow-up

- Sham Cash SYP/USD is not configured in the live `payment_settings` data or exposed payment RPCs. The UI therefore marks it unavailable rather than fabricating a working payment method.
- The admin screen currently provides a live data overview. Administrative approval/rejection and account-management actions are not implemented in this pass.
- Full runtime verification requires an Android SDK/Gradle environment and a test account. No live end-user purchase, wallet debit, Binance deposit, or receipt upload was simulated against production data.

## Additional backend findings

- The existing `public.purchase_with_wallet(p_country_name, p_duration, p_amount, p_access_code)` RPC is retained and wired to the legacy purchase-request use case. Its implementation is SECURITY DEFINER with `search_path = ''`, derives the customer from `auth.uid()`, locks and checks that user's wallet, and writes the debit and request atomically.
- Supabase's security advisor still reports that RPC as an authenticated SECURITY DEFINER function (intentional for the wallet transaction path) and reports Auth leaked-password protection as disabled. The RPC itself has the checks above; leaked-password protection was not changed as part of this app/database alignment task.
