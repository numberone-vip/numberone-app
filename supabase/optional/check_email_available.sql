-- OPTIONAL. Not applied automatically. Run it yourself in the Supabase SQL editor if you want
-- the sign-up screen to reject an already-registered email BEFORE creating the account.
--
-- Why it exists: when "Confirm email" is enabled, Supabase deliberately hides whether an email is
-- taken, so the client alone cannot detect duplicates. The app calls this RPC first and simply
-- skips the check if the function is not deployed.
--
-- Trade-off: it lets anyone test whether an email is registered (account enumeration).
-- Skip it if that is not acceptable for you.
create or replace function public.check_email_available(p_email text)
returns boolean
language sql
stable
security definer
set search_path = public, auth
as $$
  select not exists (
    select 1 from auth.users where lower(email) = lower(trim(p_email))
  );
$$;

revoke all on function public.check_email_available(text) from public;
grant execute on function public.check_email_available(text) to anon, authenticated;
