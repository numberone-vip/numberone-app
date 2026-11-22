-- Number One: promote/demote an administrator. REVIEW BEFORE APPLYING - NOT applied to Live.
-- NEW function only (no "or replace"): fails loudly if the name already exists.
-- Assumption to verify against the live schema: public.admin_users has a user_id uuid column
-- (the same assumption as admin_list_admin_ids in 20261121120000_admin_wallet_adjust.sql).
-- If admin_users has other NOT NULL columns without defaults, add them to the INSERT below.
--
-- Safety rules enforced server-side (the app UI is only a convenience):
--   * caller must pass private.is_admin()
--   * target must exist in auth.users
--   * an admin cannot change their own role (prevents accidental self-lockout)
--   * the last remaining admin can never be removed
--   * idempotent: promoting an admin or demoting a non-admin changes nothing

create function public.admin_set_admin_role(p_user_id uuid, p_make_admin boolean)
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare v_count integer;
begin
  if not private.is_admin() then raise exception 'not authorized'; end if;
  if p_user_id is null or p_make_admin is null then raise exception 'invalid arguments'; end if;
  if p_user_id = auth.uid() then raise exception 'cannot change your own role'; end if;
  if not exists (select 1 from auth.users u where u.id = p_user_id) then
    raise exception 'user not found';
  end if;

  if p_make_admin then
    insert into public.admin_users(user_id) values (p_user_id) on conflict (user_id) do nothing;
  else
    -- Lock the admin rows so two concurrent demotions cannot both pass the "last admin" check.
    perform 1 from public.admin_users a for update;
    select pg_catalog.count(*) into v_count from public.admin_users a;
    if v_count <= 1 then raise exception 'cannot remove the last admin'; end if;
    delete from public.admin_users where user_id = p_user_id;
  end if;
  return p_make_admin;
end;
$$;

revoke all on function public.admin_set_admin_role(uuid, boolean) from public, anon, authenticated;
grant execute on function public.admin_set_admin_role(uuid, boolean) to authenticated;
