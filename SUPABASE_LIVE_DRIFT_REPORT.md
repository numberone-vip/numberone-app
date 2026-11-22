# Number One — Live Supabase source alignment notes

Audit date: 2026-11-21  
Project ref: `rlxcsnposxvsyhwtmgbz`  
Mode: read-only inspection; no SQL migrations or Edge Functions deployed during this review.

## Confirmed alignment

- The `verify-binance-deposit` source in this archive is the source returned by the live Edge Function deployment (version 4).
- `five-sim-push` is present in the archive and the live deployment is active (version 3, `verify_jwt=false`). Its handler independently checks the `FIVE_SIM_PUSH_CRON_SECRET` bearer token.
- `fivesim-api` is active in Live at version 5. The local source was behind the deployed code in reservation-margin handling. The local `commitPurchase` and purchase handler have been updated to use the margin percentage captured when the wallet reservation is created, matching the live version-5 behavior.
- The live database currently restricts the internal `virtual_numbers_*` RPCs to `service_role`; the 5SIM Edge Functions call them with the server-side service-role client after validating the user's session.
- The three reviewed admin functions (`admin_adjust_wallet_balance`, `admin_list_wallet_transactions`, `admin_reset_subscription_access`) have `anon` execution disabled and `authenticated` execution enabled; each checks `private.is_admin()` inside the function.

## Important repository/database drift

The live Supabase migration history contains 41 entries, while the original archive contains only five SQL migration files. The local migration set is therefore not a complete replayable history. In particular, the local file `20261120150000_add_virtual_numbers_5sim.sql` does not have the same version as the applied live migration `20261120181738_add_virtual_numbers_5sim`; do not run `supabase db push` from this archive until the history is reconciled. The live migration history also contains these later migrations that are not present as individual files in this archive:

- `20261120182503_enable_pg_cron_pg_net_for_five_sim_push`
- `20261120234505_number_one_pricing_and_manual_service_wallet`
- `20261120234607_number_one_fivesim_attempt_pricing_hardening`
- `20261120234722_number_one_fix_sql_special_expressions`
- `20261120234901_number_one_services_admin_read_policy`

The live `private.virtual_number_requests` table contains `margin_percent_at_purchase`; the live database also contains `private.virtual_number_request_attempts`. Those additions are not represented in the archived initial migration `20261120150000_add_virtual_numbers_5sim.sql`. The deployed live `virtual_numbers_reserve_wallet` and `virtual_numbers_commit_purchase` definitions include attempt tracking and reservation-time margin handling that the initial migration does not contain.

**Do not use the archived initial 5SIM migration alone to recreate or reset the live database.** It is a historical starting point, not a complete schema snapshot. Use `supabase/scripts/EXPORT_LIVE_SCHEMA.sql` and reconcile the missing migration source before any fresh database deployment.

## Security advisor findings (read-only)

- Seven RLS-enabled tables have no policies: six are in the non-exposed `private` schema (default-deny for client roles); `public.app_settings` also has no policies (client access is denied unless an intended server-side RPC is used).
- `pg_net` is installed in `public` (advisor warning).
- Auth leaked-password protection is disabled (advisor warning; enable in Supabase Auth settings after review).
- Six `SECURITY DEFINER` functions are executable by `authenticated`. The three admin RPCs and the two virtual-number margin admin RPCs check `private.is_admin()`. `purchase_manual_service_with_wallet` derives the user from `auth.uid()` and operates on that user's wallet/request; still review its idempotency and the handling/retention of its `p_access_code` value.

## Build / packaging

- ZIP integrity check passed.
- The Gradle wrapper script is now marked executable.
- `:app:assembleDebug` could not be completed in this environment because Gradle 9.6.1 could not be downloaded (`UnknownHostException: services.gradle.org`). No APK is claimed as built or tested.
- Release signing files are not included. `google-services.json` is included for this Firebase project but is ignored by Git; if this file is already tracked in a Git repository, remove it from the Git index before pushing.

## No production deployment performed

This archive contains source/report updates only. No migration was applied, no Edge Function was deployed, and no production table/data was modified during this review.

A review-only SQL draft, `supabase/optional/HARDEN_VIRTUAL_NUMBER_RPC_ACL_REVIEW_ONLY.sql`, is included to make the server-only EXECUTE grants explicit after the historical 5SIM RPCs are created. It is deliberately outside the migrations folder and has not been applied. The live database already showed these RPCs restricted to `service_role`.
