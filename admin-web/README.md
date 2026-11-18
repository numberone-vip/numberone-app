# Number One Control Center

Standalone responsive web administration app. Existing Supabase project, tables, customer records, and RPC contracts are preserved.

## Local setup
1. Copy .env.example to .env.local.
2. Set NEXT_PUBLIC_SUPABASE_URL to the existing Supabase project URL.
3. Set NEXT_PUBLIC_SUPABASE_ANON_KEY to the existing publishable/anon key.
4. Run npm install, then npm run dev.

## Deploy the web panel
Create a Vercel project from this GitHub repository and set the project Root Directory to admin-web. Add the two public environment variables above, then deploy. The dashboard uses Next.js server-side Supabase Auth cookies. Do not add a service-role or secret key to Vercel's NEXT_PUBLIC variables.

## Access and security
- Supabase Auth session required.
- Admins are verified against public.admin_users.
- Staff are verified against public.staff_members; pages and actions are role-filtered.
- RLS and existing security-definer RPCs remain the authorization authority.
- Only the public key is used by this app. No service-role key is included.
- Payment verification uses the existing finance_verify_order_payment RPC.
- Number assignment uses orders_assign_verified_order.
- Finance wallet visibility uses new role-checked read-only RPCs: admin_list_wallet_deposits and admin_list_wallet_balances. They do not modify balances.
- SMS delivery uses admin_add_sms, which also extracts activation codes using the existing database logic.
- Purchase requests are shown read-only until an atomic approval RPC exists. This avoids changing request status without creating the corresponding subscription.
- Wallet deposits are verified by the existing verify-binance-deposit Edge Function. It currently accepts USDT BEP20 and uses server-side Binance/Supabase secrets. The payment_settings address must match the server-side BINANCE_DEPOSIT_ADDRESS; never put those secrets in the app or web dashboard.
