-- Number One: keep all virtual-number database RPCs server-only.
-- The Android app calls fivesim-api, which validates the signed-in user and then
-- invokes these RPCs through the server-side service_role client.
-- Review before deployment. This file does not change any table data.

do $$
declare
  v_function record;
begin
  for v_function in
    select p.oid,
           n.nspname,
           p.proname,
           pg_catalog.oidvectortypes(p.proargtypes) as identity_args
    from pg_proc p
    join pg_namespace n on n.oid = p.pronamespace
    where n.nspname = 'public'
      and p.proname like 'virtual_numbers_%'
  loop
    execute format(
      'revoke all on function %I.%I(%s) from public, anon, authenticated',
      v_function.nspname,
      v_function.proname,
      v_function.identity_args
    );
    execute format(
      'grant execute on function %I.%I(%s) to service_role',
      v_function.nspname,
      v_function.proname,
      v_function.identity_args
    );
  end loop;
end;
$$;
