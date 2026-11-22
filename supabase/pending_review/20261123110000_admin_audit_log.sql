-- Number One: admin audit log. REVIEW BEFORE APPLYING - NOT applied to Live.
-- NEW objects only (no "or replace"): fails loudly if a name already exists.
-- Requires the existing private.is_admin() function.
--
-- Design:
--   * The table lives in the non-exposed `private` schema with RLS on and no policies,
--     and all privileges revoked from client roles: it is reachable only through the two RPCs below.
--   * The actor (admin_id) is always auth.uid() on the server; the app cannot choose it.
--   * Append-only from the app's point of view: no RPC updates or deletes rows.
--   * Entries are reported by the app AFTER a successful admin action (admin_log_action).
--     This covers existing live RPCs without modifying them, but it is not tamper-proof against a
--     modified client and an entry can be missed if the app dies between the action and the log call.
--     For stronger guarantees, call private.admin_audit_log inserts from inside each admin RPC later.
--   * Do not put secrets (access codes, passwords) in p_details; the app never does.

create table private.admin_audit_log (
  id bigint generated always as identity primary key,
  admin_id uuid not null,
  action text not null check (char_length(action) between 1 and 64),
  target_id text check (target_id is null or char_length(target_id) <= 100),
  details text check (details is null or char_length(details) <= 500),
  created_at timestamptz not null default now()
);
create index admin_audit_log_created_idx on private.admin_audit_log (created_at desc, id desc);
alter table private.admin_audit_log enable row level security;
revoke all on table private.admin_audit_log from public, anon, authenticated;

create function public.admin_log_action(p_action text, p_target_id text default null, p_details text default null)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  if not private.is_admin() then raise exception 'not authorized'; end if;
  if coalesce(pg_catalog.btrim(p_action), '') = '' then raise exception 'action required'; end if;
  insert into private.admin_audit_log(admin_id, action, target_id, details)
  values (
    auth.uid(),
    pg_catalog.left(pg_catalog.btrim(p_action), 64),
    pg_catalog.left(p_target_id, 100),
    pg_catalog.left(p_details, 500)
  );
end;
$$;

create function public.admin_list_audit_log(p_limit integer default 100)
returns table(id bigint, admin_id uuid, admin_email text, action text, target_id text, details text, created_at timestamptz)
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
  if not private.is_admin() then raise exception 'not authorized'; end if;
  if p_limit is null or p_limit < 1 or p_limit > 500 then raise exception 'invalid limit'; end if;
  return query
    select l.id, l.admin_id, u.email::text, l.action, l.target_id, l.details, l.created_at
    from private.admin_audit_log l
    left join auth.users u on u.id = l.admin_id
    order by l.created_at desc, l.id desc
    limit p_limit;
end;
$$;

revoke all on function public.admin_log_action(text, text, text) from public, anon, authenticated;
revoke all on function public.admin_list_audit_log(integer) from public, anon, authenticated;
grant execute on function public.admin_log_action(text, text, text) to authenticated;
grant execute on function public.admin_list_audit_log(integer) to authenticated;
