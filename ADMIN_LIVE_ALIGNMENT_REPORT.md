# Number One V4 — Live Supabase Alignment

## Scope

This revision was prepared against the live Supabase project `rlxcsnposxvsyhwtmgbz` using read-only inspection. No INSERT, UPDATE, DELETE, DDL, migration, or Edge Function deployment was performed.

## Fixes made in this project copy

1. `admin_update_payment_config`
   - Android now calls the live signature exactly:
     - `p_binance_address`
     - `p_sham_cash_account`
   - Removed unsupported parameters from the RPC call.
   - The admin payment card no longer presents fields that the live RPC cannot persist.

2. `admin_upsert_service`
   - Android now matches the live signature:
     - `p_id`
     - `p_name`
     - `p_description`
     - `p_country_code`
     - `p_price`
     - `p_currency`
     - `p_is_active`
   - Removed unsupported `p_country`.
   - Service IDs are treated as text, matching the live `services.id` type.

3. Subscription assignment
   - Android no longer calls the missing `admin_assign_subscription` RPC.
   - It now calls the existing live `admin_add_subscription_for_user` RPC.
   - Client validation was aligned with the live function's access-code and phone requirements.
   - The live function hashes the access code before storing it.

4. Subscription access reset
   - Added a reviewed, not-yet-applied migration:
     `supabase/migrations/20261122120000_admin_reset_subscription_access.sql`
   - It stores a SHA-256 hash in `subscriptions.access_code_hash`, matching the existing live subscription-creation behavior.
   - It is admin-only and does not store plaintext access codes.

5. Wallet admin features
   - Kept the existing V4 migrations for:
     - `admin_adjust_wallet_balance`
     - `admin_list_admin_ids`
     - `admin_list_wallet_transactions`
   - These remain pending deployment to Live.

6. TXID protection
   - Kept the V4 draft unique index migration.
   - It uses normalized TXIDs and `lower(status)`, matching the live uppercase `PAYMENT_REVIEW` value.
   - Live data check found no conflicting active TXIDs before preparation.

7. Edge Functions
   - Restored the live `verify-binance-deposit` version 4 source into:
     `supabase/functions/verify-binance-deposit/index.ts`
   - Existing `fivesim-api` and `five-sim-push` directories remain in the project.

## Still pending on Live Supabase

The following project migrations have NOT been applied:

- `20261121120000_admin_wallet_adjust.sql`
- `20261122100000_admin_wallet_ledger.sql`
- `20261122110000_draft_orders_txid_unique.sql`
- `20261122120000_admin_reset_subscription_access.sql`

They require a final review before deployment.

## Build verification

A local Gradle compile was attempted, but the environment could not download Gradle 9.6.1 because outbound network access was unavailable (`UnknownHostException: services.gradle.org`). This is an environment limitation, not a reported Kotlin compiler error.
