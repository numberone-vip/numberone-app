# Number One Control Center

Standalone responsive web administration app. Existing Supabase project, tables, data and RPC contracts are preserved.

## Local setup
Copy .env.example to .env.local, set the existing Supabase URL and publishable/anon key, then run npm install and npm run dev.

## Access and security
- Supabase Auth session required.
- Admins are verified against public.admin_users.
- Staff are verified against public.staff_members; pages are role-filtered.
- RLS and existing security-definer RPCs remain the authorization authority.
- Only the public key is used. Never add service_role or secret keys to this app.
- Purchase-request approval and wallet-deposit review are intentionally read-only until dedicated atomic, audited database workflows are available. No unsafe direct balance or subscription mutation is performed.
