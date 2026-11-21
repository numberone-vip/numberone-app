-- APPLIED MANUALLY on 2026-11-21. Owner auto-approval: DISABLED by default, cap 15 USD, RLS enabled.
create table if not exists private.owner_auto_approve_config (
  singleton boolean primary key default true check (singleton),
  enabled boolean not null default false,
  max_amount_usdt numeric(20,8) not null default 15 check (max_amount_usdt > 0),
  updated_at timestamptz not null default now()
);
insert into private.owner_auto_approve_config (singleton) values (true) on conflict (singleton) do nothing;
revoke all on private.owner_auto_approve_config from public, anon, authenticated;
alter table private.owner_auto_approve_config enable row level security;

create or replace function private.owner_auto_approve_allowed(p_user_id uuid, p_amount numeric)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
      select 1 from private.owner_auto_approve_config c
      where c.singleton and c.enabled
        and p_amount is not null and p_amount > 0 and p_amount <= c.max_amount_usdt
    )
    and exists (select 1 from public.admin_users a where a.user_id = p_user_id);
$$;
revoke all on function private.owner_auto_approve_allowed(uuid, numeric) from public, anon, authenticated;

-- Enable/disable (run manually only):
--   update private.owner_auto_approve_config set enabled = true where singleton;
--   update private.owner_auto_approve_config set enabled = false where singleton;
