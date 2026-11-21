-- Number One: DRAFT ONLY (cap = 15 USDT; amounts above cap stay for manual approval). NOT a migration. Do NOT apply until reviewed.
-- Purpose: groundwork for owner auto-approval (admin's own top-ups and purchases).
-- It creates a config table and a checker function, and changes NOTHING existing.
-- The existing approval RPCs (Sham Cash approval, manual order payment, wallet purchase,
-- virtual-number manual_review) were NOT in the archive, so they are not modified here.
-- Hooks must be added after exporting their live definitions (supabase/scripts/EXPORT_LIVE_SCHEMA.sql).

create table if not exists private.owner_auto_approve_config (
  singleton boolean primary key default true check (singleton),
  enabled boolean not null default false,            -- start OFF; enable deliberately
  max_amount_usdt numeric(20,8) not null default 15 check (max_amount_usdt > 0),
  updated_at timestamptz not null default now()
);
insert into private.owner_auto_approve_config (singleton) values (true)
  on conflict (singleton) do nothing;
revoke all on private.owner_auto_approve_config from public, anon, authenticated;

-- Returns true only when: feature enabled, user is listed in public.admin_users,
-- and the amount is within the cap. Never trusts client input for admin status.
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
-- Intentionally NOT granted to any client role: only server-side functions (SECURITY DEFINER) call it.

-- TODO (after reviewing live definitions):
-- 1. Sham Cash approval RPC: if owner_auto_approve_allowed(user_id, amount) then run the same credit path.
-- 2. Manual order payment: if allowed, approve order and grant access as the normal path does.
-- 3. virtual_numbers_commit_purchase: if allowed and status would be manual_review, finalize the difference.
-- Every auto approval must still write the same wallet_transactions row with a clear description.
