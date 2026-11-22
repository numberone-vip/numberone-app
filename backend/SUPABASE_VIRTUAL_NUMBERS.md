# Number One — 5SIM Virtual Numbers backend contract

This update extends the **existing** Supabase Edge Function named `fivesim-api`; Android calls that authenticated function, not 5SIM directly. The 5SIM bearer key remains a server-side Edge Function secret. All wallet operations use the existing `public.wallets` and `public.wallet_transactions` tables; no second wallet is introduced.

## Live project inspection

A read-only inspection of the connected Supabase project found:

- An active Edge Function `fivesim-api` (deployed v1, `verify_jwt = true`). Its current source supports the existing `countries`, `prices`, `products`, and admin-only `profile` actions. The source expects the `FIVESIM_API_KEY` Edge Function secret.
- The existing wallet tables are `public.wallets` (`user_id`, `balance`, `currency`) and `public.wallet_transactions` (`transaction_type`, `direction`, `amount`, `balance_after`, `reference_type`, `reference_id`, etc.). The current wallet summary/transaction RPCs are already present and are left unchanged.
- No `virtual_numbers_*` RPCs or virtual-number operational tables were present in the inspected database catalog. The `vault.secrets` table had no rows at inspection time. The deployed Edge Function source reads `FIVESIM_API_KEY` from its environment; verify that secret in Edge Function secrets rather than exposing it in a client-callable table or RPC.

The archive therefore includes a SQL migration that adds the virtual-number tables and the exact RPCs below. **The migration is included but is not applied to your live project by this ZIP build.** The replacement Edge Function source is also included but is not deployed, to avoid replacing your existing `fivesim-api` until the database migration and secrets are configured.

## Android-to-Edge Function contract

All interactive requests are authenticated POST calls to `fivesim-api`. The request body is JSON:

| `action` | Required input | Response |
|---|---|---|
| `catalog` | `{ "action": "catalog" }` | `{ "countries": [{"name":"Canada","country_code":"canada","iso":"ca","dial_code":"+1"}], "services": [{"code":"whatsapp","name":"WhatsApp"}] }` |
| `quote` | `country_code`, `service_code` | `VirtualNumberQuote`: `country_code`, `service_code`, `currency`, `margin_mode`, `margin_percent`, `margin_fixed`, and `operators[]` with `name`, `success_rate`, `available_qty`, `base_price` |
| `purchase` | `country_code`, `service_code`, `operator` (`auto` or exact provider name), UUID `request_id` | `VirtualNumberOrder` |
| `poll` | UUID `activation_id` | Updated `VirtualNumberOrder`; ownership is checked server-side |
| `request_next_sms` | UUID `activation_id` | Refreshes the same active activation and returns its latest SMS; does not buy a new number or debit the wallet again |
| `cancel` | UUID `activation_id` | Updated `VirtualNumberOrder`; refund is credited only after a provider-confirmed cancellation |
| `latest_activation` | `{ "action": "latest_activation" }` | `{ "activation": VirtualNumberOrder or null }` |
| `register_device_token` | `fcm_token`, `platform: "android"` | `{ "ok": true }` |

`VirtualNumberOrder` contains `activation_id` (app-owned UUID), `phone`, `status`, nullable `sms_code`, nullable `sms_text`, nullable `expires_at`, `charged_price`, `base_price`, `product_code`, and `country_code`.

The legacy `fivesim-api` actions `countries`, `prices`, `products`, and admin-only `profile` are retained by the updated Edge Function source for compatibility with the already deployed function.

## Required database RPCs

These are the functions called by the Edge Function with the service-role client. The SQL definitions are in `supabase/migrations/20261120150000_add_virtual_numbers_5sim.sql`.

| RPC | Required signature | Purpose |
|---|---|---|
| `virtual_numbers_get_pricing_config` | `() returns jsonb` | Returns `wallet_currency`, `provider_to_wallet_rate`, `margin_mode`, `margin_percent`, and `margin_fixed` from the private pricing row. |
| `virtual_numbers_get_enabled_services` | `() returns table(code text, name text)` | Returns enabled service shortcuts; initial seed is WhatsApp, Telegram, Facebook, Google. |
| `virtual_numbers_reserve_wallet` | `(p_user_id uuid, p_request_id uuid, p_country_code text, p_service_code text, p_operator text, p_base_price numeric, p_final_price numeric) returns jsonb` | Idempotently locks and debits the existing wallet, writes a `purchase` debit to `wallet_transactions`, and returns `should_call_provider: true` exactly once. Retries return the saved provider order, the committed activation, or a blocked-pending state; they never authorize a second 5SIM purchase. It blocks a second unresolved provider call for the same user. |
| `virtual_numbers_attach_provider_activation` | `(p_user_id uuid, p_request_id uuid, p_provider_activation_id bigint, p_provider_price numeric, p_phone text, p_status text, p_expires_at timestamptz) returns jsonb` | Saves the provider activation immediately after 5SIM responds, before attempting the final DB commit. This is the key retry/idempotency guard. |
| `virtual_numbers_commit_purchase` | `(p_user_id uuid, p_request_id uuid, p_provider_activation_id bigint, p_phone text, p_status text, p_expires_at timestamptz, p_base_price numeric, p_final_price numeric, p_country_code text, p_service_code text, p_operator text) returns jsonb` | Creates the user activation and reconciles the final provider price against the reserved quote using the same wallet ledger. If the actual total is higher but the balance cannot cover the difference, it marks the request for manual reconciliation rather than allowing a negative wallet or silently charging less. |
| `virtual_numbers_refund_wallet` | `(p_user_id uuid, p_request_id uuid, p_reason text) returns jsonb` | Credits the committed/reserved retail amount once, with a `refund` ledger transaction. It is used only for a definitive provider failure or 5SIM-confirmed cancellation/timeout with no SMS. In manual-review state it refunds only the amount actually reserved. |
| `virtual_numbers_get_my_activation` | `(p_user_id uuid, p_activation_id uuid) returns jsonb` | Returns a user's activation, including provider id, request id, country, service, operator and phone; returns null when not owned. |
| `virtual_numbers_sync_activation` | `(p_user_id uuid, p_activation_id uuid, p_status text, p_sms_code text, p_sms_text text, p_expires_at timestamptz) returns jsonb` | Updates the activation and returns `notification_claimed`. A row lock ensures only one concurrent poll claims each newly observed code. |
| `virtual_numbers_get_my_latest_activation` | `(p_user_id uuid) returns jsonb` | Returns the most recent activation for the signed-in user. |
| `virtual_numbers_register_fcm_token` | `(p_user_id uuid, p_fcm_token text, p_platform text) returns void` | Associates an FCM registration token with the authenticated user. |
| `virtual_numbers_get_active_for_push` | `() returns setof jsonb` | Service-only list of active activations to poll from the scheduled job. |
| `virtual_numbers_get_user_fcm_tokens` | `(p_user_id uuid) returns setof jsonb` | Service-only FCM token projection for a user. |

The `virtual_numbers_*` operational tables live in the non-exposed `private` schema. The migration enables RLS and revokes client roles from those tables. The RPCs are `SECURITY DEFINER` with an empty `search_path`, and are executable by `service_role` only. The Android app never invokes the RPCs directly; it only invokes the authenticated Edge Function.

## Configuration

The migration seeds `private.virtual_number_settings` with `wallet_currency='USDT'`, `provider_to_wallet_rate=1`, and a default zero-percent margin. The connected production project was subsequently configured to `margin_mode='percent'` and `margin_percent=20`. In the current `fivesim-api` implementation this field is a **markup on provider cost** (`retail = cost × conversion_rate × 1.20`), not a 20% gross margin of the retail selling price; a 20% markup yields a 16.67% gross margin. Change the pricing policy in the private settings row when needed, for example:

```sql
update private.virtual_number_settings
set provider_to_wallet_rate = 1,
    margin_mode = 'percent',
    margin_percent = 20,
    margin_fixed = 0,
    updated_at = now()
where singleton = true;
```

Use `margin_mode = 'fixed'` and set `margin_fixed` for a fixed markup. The UI displays the live backend quote, but the server recalculates and reserves the final amount at purchase time. The conversion rate must reflect the actual conversion from 5SIM provider price units to wallet USDT.

The existing 5SIM Edge Function source requires the server-side `FIVESIM_API_KEY` secret. Do not place it in Android, app metadata, a client-readable settings table, or an RPC callable by `anon`/`authenticated`. Standard Edge Function secrets `SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY` are also required; the function accepts `SUPABASE_ANON_KEY` or the existing `SUPABASE_PUBLISHABLE_KEYS` JSON map for user JWT validation.

## 5SIM provider endpoints

The Edge Function uses:

- `GET /v1/guest/countries` for country ISO and prefix metadata.
- `GET /v1/guest/prices?country={country}&product={product}` for operator-level `cost`, `count`, and `rate`.
- `GET /v1/user/buy/activation/{country}/{operator}/{product}` for a new one-time activation. The app does not set `reuse=1` on a new purchase.
- `GET /v1/user/check/{id}` for activation status and SMS details.
- `GET /v1/user/cancel/{id}` to request cancellation.


5SIM documents country ISO/prefix metadata, operator quote fields, activation checks and cancellation in its API reference. The app’s “تحقق من SMS جديد” action calls `user/check/{id}` again for the **same active activation**, reads the newest item in its SMS array and does not charge the wallet again. The verification request itself must be initiated in the external service (WhatsApp/Telegram/etc.); this app refreshes the 5SIM inbox. More than one SMS may be delivered within a live activation depending on service/operator policy. The screen and background poller continue checking both `PENDING` and `RECEIVED` activations until expiry so later messages are also surfaced. 5SIM states that once an order is finished, the same number cannot be used to request another SMS; the separate 5SIM `GET /v1/user/reuse/{product}/{number}` endpoint re-buys a number as a new order and is deliberately not called by this button.

## Background notifications

The Android project includes Firebase Messaging. To activate background alerts:

1. Register Android package `com.numberone.numberone` in Firebase and add the matching `app/google-services.json` (not included because it is unique to your Firebase project).
2. Configure `FCM_SERVICE_ACCOUNT_JSON` and `FIVE_SIM_PUSH_CRON_SECRET` as Edge Function secrets. `FIVESIM_API_KEY` must also be available to the scheduled function.
3. Schedule `five-sim-push` every minute with `Authorization: Bearer <FIVE_SIM_PUSH_CRON_SECRET>` by POSTing to `https://<project-ref>.supabase.co/functions/v1/five-sim-push`. The function verifies that bearer secret itself (`verify_jwt = false` in `supabase/config.toml`).

The poller checks unexpired activations (and a short expiry grace window), claims a new code through `virtual_numbers_sync_activation`, and sends an FCM data message only when `notification_claimed` is true. It also credits the app wallet once if 5SIM confirms a canceled/timed-out activation with no SMS. The foreground screen also checks every five seconds. Since 5SIM is polled rather than calling a provider webhook in this implementation, a background notification is best-effort on the one-minute schedule plus provider/FCM latency; it is not literally instantaneous. Android 13+ notification permission is requested at runtime.

## Deployment / verification

1. Review and apply `supabase/migrations/20261120150000_add_virtual_numbers_5sim.sql` using your normal Supabase migration workflow (for example, `supabase db push` after linking the intended project). It creates private activation/idempotency/settings/service-catalog/FCM-token tables and the RPCs above; it does not alter existing wallet tables or wallet RPCs.
2. Confirm `FIVESIM_API_KEY` is configured as an Edge Function secret. The standard Supabase URL, anon/publishable key and service-role key must be available to `fivesim-api`. Configure the provider-to-wallet conversion and margin settings in `private.virtual_number_settings`.
3. Deploy the extended `fivesim-api` source with JWT verification enabled: `supabase functions deploy fivesim-api`.
4. Configure Firebase as described above, then deploy the scheduler: `supabase functions deploy five-sim-push`. Configure `FCM_SERVICE_ACCOUNT_JSON` and `FIVE_SIM_PUSH_CRON_SECRET`, and schedule a POST every minute to `/functions/v1/five-sim-push` with `Authorization: Bearer <FIVE_SIM_PUSH_CRON_SECRET>`.
5. Verify the RPC list in the section above appears in `public` with execute permission for `service_role` only. Test with a staging user and 5SIM balance: successful purchase, idempotent replay, definitive provider failure/refund, ambiguous provider timeout/manual reconciliation, multiple SMS on one active number, provider cancellation and timeout refunds, and FCM push.

No live wallet transaction, 5SIM purchase, SQL migration, or Edge Function deployment was performed during this project archive update. The live database inspection above was read-only.


## Live deployment record (2026-11-20)

- Applied migration `add_virtual_numbers_5sim` to the connected project.
- Deployed `fivesim-api` version 2 with JWT verification enabled.
- Deployed `five-sim-push` version 1 with JWT verification disabled; the function enforces its own `FIVE_SIM_PUSH_CRON_SECRET` bearer check.
- Verified all 12 `public.virtual_numbers_*` RPCs exist. All 12 are `SECURITY DEFINER`, executable by `service_role`, and not executable by `authenticated` or `anon`.
- Smoke-checked the pricing config and enabled-service catalogue RPCs. The initial config is `margin_mode=percent`, `margin_percent=0`, `margin_fixed=0`, and `provider_to_wallet_rate=1`; set the desired retail margin before enabling real purchases.
- FCM push payload intentionally contains a new-SMS alert and activation/phone identifiers, but not the SMS OTP itself. The user retrieves the OTP from the authenticated `fivesim-api` poll endpoint after opening the app.

### Production configuration update (2026-11-20)

- Enabled `pg_cron` 1.6.4 (extension namespace `pg_catalog`) and `pg_net` 0.20.4 (extension namespace `public`). Supabase Vault is enabled in schema `vault`.
- Updated the singleton row in `private.virtual_number_settings` to `margin_mode='percent'`, `margin_percent=20`, `margin_fixed=0`, `provider_to_wallet_rate=1`, and `wallet_currency='USDT'`.
- Created active cron job `five-sim-push-every-minute` (job ID 1), schedule `* * * * *`. It sends a POST to `https://rlxcsnposxvsyhwtmgbz.supabase.co/functions/v1/five-sim-push` and reads its `Authorization: Bearer ...` value from Vault secret `five_sim_push_cron_secret`.
- The Vault bearer secret was generated and stored in the project. The cron SQL execution is succeeding, but the latest HTTP response from the function was `401 Unauthorized`, confirming the Edge Function secret has not yet been synchronized. Copy the Vault value into the Edge Function secret `FIVE_SIM_PUSH_CRON_SECRET` by running this in the Supabase SQL Editor and copying the returned value directly into Edge Function Secrets:

```sql
select decrypted_secret
from vault.decrypted_secrets
where name = 'five_sim_push_cron_secret';
```

Do not put the secret in source control, the Android app, or this ZIP.

### Required Edge Function secrets / Firebase checklist

Manually verify or add these names in Supabase Dashboard -> Edge Functions -> Secrets:

- `FIVESIM_API_KEY` — 5SIM account API token; required by both `fivesim-api` and `five-sim-push`.
- `FCM_SERVICE_ACCOUNT_JSON` — the complete Firebase service-account JSON object as a single secret value; its `project_id` must match the Firebase project used by the Android app, and Firebase Cloud Messaging API must be enabled.
- `FIVE_SIM_PUSH_CRON_SECRET` — must exactly match the value returned by the Vault query above. The cron job already sends this value as a bearer token.

Supabase normally injects `SUPABASE_URL`, `SUPABASE_ANON_KEY`, and `SUPABASE_SERVICE_ROLE_KEY` into Edge Function runtimes. The API source accepts `SUPABASE_ANON_KEY` or, alternatively, `SUPABASE_PUBLISHABLE_KEYS` (a JSON map with `default` or `anon`) for validating user JWTs. Do not expose `SUPABASE_SERVICE_ROLE_KEY` in Android or use it as the cron bearer credential; the push function validates its dedicated cron secret itself.

For Firebase Android setup, register package `com.numberone.numberone` and include the matching `app/google-services.json` in the Android project. That file is specific to the Firebase project and is intentionally not included in this ZIP.

### Current readiness boundary

The database extensions, 20% margin, Vault credential, and active every-minute cron job are configured and verified. Background push delivery remains gated on adding the same Vault value as the Edge Function secret `FIVE_SIM_PUSH_CRON_SECRET`, and confirming `FIVESIM_API_KEY` and `FCM_SERVICE_ACCOUNT_JSON` are present. No real 5SIM purchase or wallet transaction was performed as part of this configuration.
