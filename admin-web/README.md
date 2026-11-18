# Number One Control Center

Standalone web dashboard for Number One. Uses the existing Supabase project and does not create, delete, or migrate customer data.

## Local setup
- Copy .env.example to .env.local and set the Supabase URL and publishable/anon key.
- Run npm install, then npm run dev.

## Security
- Authentication uses Supabase Auth.
- Admin access is checked against public.admin_users.
- Staff roles are checked against public.staff_members and existing RLS.
- Browser/server clients use only the public key; no service-role key is included.
- Database RLS and existing RPCs remain authoritative.
