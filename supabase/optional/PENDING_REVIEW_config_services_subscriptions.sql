-- NOT A MIGRATION. Held back for review: it may overlap functions that already exist in your database
-- (get_payment_config, admin_list_services, ...), and its subscription functions use PLAIN-TEXT access_code,
-- which conflicts with access_code_hash. Do not apply until reconciled with the live definitions.
-- Number One: Admin Command Center RPCs.
-- Backs the five-tab AdminScreen: live payment config, services catalog, user wallets, subscriptions.
--
-- ASSUMPTIONS (verify against your live schema before applying):
--   * private.is_admin() exists (the app already calls it) and uses auth.uid().
--   * public.services(id uuid pk, name, country, country_code, price, icon_url, available_count, created_at).
--   * public.wallets(user_id pk, balance, updated_at) and public.wallet_transactions as used by the 5SIM migration.
--   * public.subscriptions(id, user_id, service_id, phone_number, access_code, expiry_date, status, admin_note).
--   * Admin role lookup in admin_list_users() uses public.admin_users(user_id); adjust that one line if your
--     role storage differs. If it errors, the app falls back to admin_list_customers + admin_list_wallet_balances
--     and simply hides the role chip.

create schema if not exists private;

-- ---------------------------------------------------------------------------
-- Payment + app config (single row)
-- ---------------------------------------------------------------------------
create table if not exists private.app_config (
  singleton boolean primary key default true check (singleton),
  binance_wallet_address text not null default '',
  binance_network text not null default 'BEP20',
  sham_cash_account text not null default '',
  sham_cash_name text not null default '',
  syp_usd_rate numeric(20,4) not null default 0 check (syp_usd_rate >= 0),
  promo_banner_text text not null default '' check (char_length(promo_banner_text) <= 300),
  updated_at timestamptz not null default now()
);
insert into private.app_config(singleton) values (true) on conflict (singleton) do nothing;
alter table private.app_config enable row level security;

-- Readable by every signed-in customer (payment screens need it).
create or replace function public.get_payment_config()
returns jsonb
language sql
stable
security definer
set search_path = ''
as $$
  select pg_catalog.jsonb_build_object(
    'binance_wallet_address', c.binance_wallet_address,
    'binance_network', c.binance_network,
    'sham_cash_account', c.sham_cash_account,
    'sham_cash_name', c.sham_cash_name,
    'syp_usd_rate', c.syp_usd_rate,
    'promo_banner_text', c.promo_banner_text
  ) from private.app_config c where c.singleton = true;
$$;

create or replace function public.admin_update_payment_config(
  p_binance_wallet_address text,
  p_sham_cash_account text,
  p_sham_cash_name text,
  p_syp_usd_rate numeric,
  p_promo_banner_text text
) returns boolean
language plpgsql
security definer
set search_path = ''
as $$
begin
  if not private.is_admin() then raise exception 'not authorized'; end if;
  if coalesce(p_binance_wallet_address, '') <> '' and p_binance_wallet_address !~ '^0x[0-9a-fA-F]{40}$' then
    raise exception 'invalid BEP20 address';
  end if;
  if p_syp_usd_rate is null or p_syp_usd_rate < 0 then raise exception 'invalid rate'; end if;
  update private.app_config set
    binance_wallet_address = coalesce(trim(p_binance_wallet_address), ''),
    sham_cash_account = coalesce(trim(p_sham_cash_account), ''),
    sham_cash_name = coalesce(trim(p_sham_cash_name), ''),
    syp_usd_rate = p_syp_usd_rate,
    promo_banner_text = left(coalesce(trim(p_promo_banner_text), ''), 300),
    updated_at = now()
  where singleton = true;
  return true;
end;
$$;

-- ---------------------------------------------------------------------------
-- Services catalog
-- ---------------------------------------------------------------------------
alter table public.services add column if not exists description text;
alter table public.services add column if not exists is_active boolean not null default true;

create or replace function public.admin_list_services()
returns jsonb
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
  if not private.is_admin() then raise exception 'not authorized'; end if;
  return coalesce((select pg_catalog.jsonb_agg(pg_catalog.to_jsonb(s) order by s.name) from public.services s), '[]'::jsonb);
end;
$$;

create or replace function public.admin_upsert_service(
  p_id uuid default null,
  p_name text default null,
  p_country text default '',
  p_country_code text default '',
  p_price numeric default 0,
  p_description text default '',
  p_is_active boolean default true
) returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare v_id uuid;
begin
  if not private.is_admin() then raise exception 'not authorized'; end if;
  if coalesce(trim(p_name), '') = '' then raise exception 'name required'; end if;
  if p_price is null or p_price < 0 then raise exception 'invalid price'; end if;
  if p_id is null then
    insert into public.services(name, country, country_code, price, description, is_active)
    values (trim(p_name), coalesce(p_country, ''), coalesce(p_country_code, ''), p_price, coalesce(p_description, ''), coalesce(p_is_active, true))
    returning id into v_id;
  else
    update public.services set
      name = trim(p_name), country = coalesce(p_country, ''), country_code = coalesce(p_country_code, ''),
      price = p_price, description = coalesce(p_description, ''), is_active = coalesce(p_is_active, true)
    where id = p_id
    returning id into v_id;
    if v_id is null then raise exception 'service not found'; end if;
  end if;
  return v_id;
end;
$$;

create or replace function public.admin_delete_service(p_id uuid)
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
begin
  if not private.is_admin() then raise exception 'not authorized'; end if;
  delete from public.services where id = p_id;  -- FK violation if referenced: deactivate instead
  return found;
end;
$$;

-- ---------------------------------------------------------------------------
-- Users, wallets, subscriptions
-- ---------------------------------------------------------------------------
create or replace function public.admin_list_users()
returns table(user_id uuid, email text, role text, balance numeric, currency text, created_at timestamptz)
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
  if not private.is_admin() then raise exception 'not authorized'; end if;
  return query
    select u.id, u.email::text,
           case when exists (select 1 from public.admin_users a where a.user_id = u.id) then 'Admin' else 'User' end,
           coalesce(w.balance, 0)::numeric, 'USDT'::text, u.created_at
    from auth.users u
    left join public.wallets w on w.user_id = u.id
    order by u.created_at desc;
end;
$$;

create or replace function public.admin_adjust_wallet_balance(p_user_id uuid, p_amount numeric, p_note text)
returns numeric
language plpgsql
security definer
set search_path = ''
as $$
declare v_balance numeric; v_new numeric;
begin
  if not private.is_admin() then raise exception 'not authorized'; end if;
  if p_amount is null or p_amount = 0 or abs(p_amount) > 1000000 then raise exception 'invalid amount'; end if;
  if coalesce(trim(p_note), '') = '' then raise exception 'audit note required'; end if;
  insert into public.wallets(user_id) values (p_user_id) on conflict (user_id) do nothing;
  select w.balance into v_balance from public.wallets w where w.user_id = p_user_id for update;
  v_new := pg_catalog.round(v_balance + p_amount, 8);
  if v_new < 0 then raise exception 'Insufficient wallet balance'; end if;
  update public.wallets set balance = v_new, updated_at = now() where user_id = p_user_id;
  insert into public.wallet_transactions(
    user_id, transaction_type, direction, amount, balance_after, currency,
    reference_type, reference_id, description
  ) values (
    p_user_id, 'admin_adjustment', case when p_amount > 0 then 'credit' else 'debit' end,
    abs(p_amount), v_new, 'USDT', 'admin_adjustment', null, trim(p_note)
  );
  return v_new;
end;
$$;

create or replace function public.admin_assign_subscription(
  p_user_id uuid, p_service_id uuid, p_phone_number text, p_access_code text, p_expiry_date date
) returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare v_id uuid;
begin
  if not private.is_admin() then raise exception 'not authorized'; end if;
  if coalesce(trim(p_phone_number), '') = '' or coalesce(trim(p_access_code), '') = '' then
    raise exception 'phone and access code required';
  end if;
  insert into public.subscriptions(user_id, service_id, phone_number, access_code, expiry_date, status)
  values (p_user_id, p_service_id, trim(p_phone_number), trim(p_access_code), p_expiry_date, 'active')
  returning id into v_id;
  return v_id;
end;
$$;

create or replace function public.admin_reset_subscription_access(
  p_subscription_id uuid, p_access_code text default null, p_expiry_date date default null
) returns boolean
language plpgsql
security definer
set search_path = ''
as $$
begin
  if not private.is_admin() then raise exception 'not authorized'; end if;
  if p_access_code is null and p_expiry_date is null then raise exception 'nothing to update'; end if;
  update public.subscriptions set
    access_code = coalesce(nullif(trim(p_access_code), ''), access_code),
    expiry_date = coalesce(p_expiry_date, expiry_date)
  where id = p_subscription_id;
  return found;
end;
$$;

-- Execution rights: signed-in users only; each admin RPC re-checks private.is_admin().
revoke all on function public.get_payment_config() from public, anon;
revoke all on function public.admin_update_payment_config(text, text, text, numeric, text) from public, anon;
revoke all on function public.admin_list_services() from public, anon;
revoke all on function public.admin_upsert_service(uuid, text, text, text, numeric, text, boolean) from public, anon;
revoke all on function public.admin_delete_service(uuid) from public, anon;
revoke all on function public.admin_list_users() from public, anon;
revoke all on function public.admin_adjust_wallet_balance(uuid, numeric, text) from public, anon;
revoke all on function public.admin_assign_subscription(uuid, uuid, text, text, date) from public, anon;
revoke all on function public.admin_reset_subscription_access(uuid, text, date) from public, anon;
grant execute on function public.get_payment_config() to authenticated;
grant execute on function public.admin_update_payment_config(text, text, text, numeric, text) to authenticated;
grant execute on function public.admin_list_services() to authenticated;
grant execute on function public.admin_upsert_service(uuid, text, text, text, numeric, text, boolean) to authenticated;
grant execute on function public.admin_delete_service(uuid) to authenticated;
grant execute on function public.admin_list_users() to authenticated;
grant execute on function public.admin_adjust_wallet_balance(uuid, numeric, text) to authenticated;
grant execute on function public.admin_assign_subscription(uuid, uuid, text, text, date) to authenticated;
grant execute on function public.admin_reset_subscription_access(uuid, text, date) to authenticated;
