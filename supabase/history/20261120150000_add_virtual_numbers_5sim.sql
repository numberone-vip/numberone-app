-- Number One: one-time 5SIM virtual number activations.
-- Uses the existing public.wallets + public.wallet_transactions ledger.
-- All operational tables are kept in the non-exposed private schema. The Edge
-- Function authenticates the caller and invokes these RPCs with service_role.

create schema if not exists private;

create table if not exists private.virtual_number_settings (
  singleton boolean primary key default true check (singleton),
  wallet_currency text not null default 'USDT' check (wallet_currency = 'USDT'),
  provider_to_wallet_rate numeric(20,8) not null default 1 check (provider_to_wallet_rate > 0),
  margin_mode text not null default 'percent' check (margin_mode in ('percent', 'fixed')),
  margin_percent numeric(20,8) not null default 0 check (margin_percent >= 0),
  margin_fixed numeric(20,8) not null default 0 check (margin_fixed >= 0),
  updated_at timestamptz not null default now()
);
insert into private.virtual_number_settings(singleton) values (true)
on conflict (singleton) do nothing;

create table if not exists private.virtual_number_services (
  service_code text primary key,
  display_name text not null,
  is_enabled boolean not null default true,
  sort_order integer not null default 100,
  updated_at timestamptz not null default now()
);
insert into private.virtual_number_services(service_code, display_name, sort_order)
values ('whatsapp','WhatsApp',10), ('telegram','Telegram',20),
       ('facebook','Facebook',30), ('google','Google',40)
on conflict (service_code) do nothing;

create table if not exists private.virtual_number_requests (
  request_id uuid primary key,
  user_id uuid not null references auth.users(id) on delete restrict,
  country_code text not null,
  service_code text not null,
  operator text not null,
  reserved_base_price numeric(20,8) not null check (reserved_base_price > 0),
  reserved_final_price numeric(20,8) not null check (reserved_final_price > 0),
  actual_base_price numeric(20,8),
  actual_final_price numeric(20,8),
  provider_activation_id bigint unique,
  provider_price numeric(20,8),
  phone text,
  provider_status text,
  provider_expires_at timestamptz,
  status text not null check (status in ('provider_started','provider_attached','committed','refunded','manual_review')),
  reason text,
  activation_id uuid,
  attempt_count integer not null default 1 check (attempt_count > 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists virtual_number_requests_user_status_idx
  on private.virtual_number_requests(user_id, status, created_at desc);

create table if not exists private.virtual_number_activations (
  activation_id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete restrict,
  request_id uuid not null unique references private.virtual_number_requests(request_id) on delete restrict,
  provider_activation_id bigint not null unique,
  phone text not null,
  country_code text not null,
  service_code text not null,
  operator text not null,
  status text not null default 'PENDING',
  sms_code text,
  sms_text text,
  last_sms_code text,
  expires_at timestamptz,
  base_price numeric(20,8) not null,
  charged_price numeric(20,8) not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists virtual_number_activations_user_created_idx
  on private.virtual_number_activations(user_id, created_at desc);
create index if not exists virtual_number_activations_active_idx
  on private.virtual_number_activations(status, expires_at)
  where status in ('PENDING','WAITING','ACTIVE','RECEIVED');

create table if not exists private.virtual_number_fcm_tokens (
  fcm_token text primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  platform text not null default 'android',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists virtual_number_fcm_tokens_user_idx
  on private.virtual_number_fcm_tokens(user_id);

alter table private.virtual_number_settings enable row level security;
alter table private.virtual_number_services enable row level security;
alter table private.virtual_number_requests enable row level security;
alter table private.virtual_number_activations enable row level security;
alter table private.virtual_number_fcm_tokens enable row level security;
revoke all on private.virtual_number_settings, private.virtual_number_services,
  private.virtual_number_requests, private.virtual_number_activations,
  private.virtual_number_fcm_tokens from public, anon, authenticated;

create or replace function private.virtual_number_activation_json(p_activation_id uuid)
returns jsonb
language sql
stable
set search_path = ''
as $$
  select pg_catalog.jsonb_build_object(
    'activation_id', a.activation_id,
    'request_id', a.request_id,
    'provider_activation_id', a.provider_activation_id,
    'phone', a.phone,
    'status', a.status,
    'sms_code', a.sms_code,
    'sms_text', a.sms_text,
    'last_sms_code', a.last_sms_code,
    'expires_at', a.expires_at,
    'charged_price', a.charged_price,
    'base_price', a.base_price,
    'product_code', a.service_code,
    'country_code', a.country_code,
    'operator', a.operator
  )
  from private.virtual_number_activations a
  where a.activation_id = p_activation_id;
$$;
revoke all on function private.virtual_number_activation_json(uuid) from public, anon, authenticated;

create or replace function public.virtual_numbers_get_pricing_config()
returns jsonb
language sql
stable
security definer
set search_path = ''
as $$
  select pg_catalog.jsonb_build_object(
    'wallet_currency', wallet_currency,
    'provider_to_wallet_rate', provider_to_wallet_rate,
    'margin_mode', margin_mode,
    'margin_percent', margin_percent,
    'margin_fixed', margin_fixed
  ) from private.virtual_number_settings where singleton = true;
$$;

create or replace function public.virtual_numbers_get_enabled_services()
returns table(code text, name text)
language sql
stable
security definer
set search_path = ''
as $$
  select s.service_code, s.display_name
  from private.virtual_number_services s
  where s.is_enabled = true
  order by s.sort_order, s.display_name;
$$;

create or replace function public.virtual_numbers_reserve_wallet(
  p_user_id uuid,
  p_request_id uuid,
  p_country_code text,
  p_service_code text,
  p_operator text,
  p_base_price numeric,
  p_final_price numeric
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_request private.virtual_number_requests%rowtype;
  v_balance numeric(20,8);
  v_new_balance numeric(20,8);
  v_existing boolean := false;
  v_retry_refunded boolean := false;
begin
  if p_user_id is null or p_request_id is null then raise exception 'User and request IDs are required'; end if;
  if p_country_code is null or p_service_code is null or p_operator is null
     or p_base_price is null or p_final_price is null
     or p_base_price <= 0 or p_final_price < p_base_price then
    raise exception 'Invalid virtual number quote';
  end if;

  perform pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(p_user_id::text, 0));
  select * into v_request from private.virtual_number_requests r
   where r.request_id = p_request_id for update;
  v_existing := found;
  if v_existing then
    if v_request.user_id <> p_user_id then
      raise exception 'Request UUID belongs to another user';
    end if;
    v_retry_refunded := v_request.status = 'refunded'
      and v_request.provider_activation_id is null
      and v_request.activation_id is null;
    if not v_retry_refunded and (
       v_request.country_code <> p_country_code
       or v_request.service_code <> p_service_code
       or v_request.operator <> p_operator
       or v_request.reserved_base_price <> p_base_price
       or v_request.reserved_final_price <> p_final_price
    ) then
      raise exception 'Request UUID was already used with different purchase details';
    end if;
    if v_request.status = 'committed' then
      return pg_catalog.jsonb_build_object('activation', private.virtual_number_activation_json(v_request.activation_id));
    end if;
    if v_request.provider_activation_id is not null then
      return pg_catalog.jsonb_build_object('provider_order', pg_catalog.jsonb_build_object(
        'id', v_request.provider_activation_id, 'phone', v_request.phone,
        'status', coalesce(v_request.provider_status, 'PENDING'),
        'expires', v_request.provider_expires_at, 'product', v_request.service_code,
        'country', v_request.country_code, 'operator', v_request.operator,
        'price', coalesce(v_request.provider_price, 0)
      ));
    end if;
    if not v_retry_refunded then
      return pg_catalog.jsonb_build_object('should_call_provider', false, 'status', v_request.status);
    end if;
  end if;

  if exists (
    select 1 from private.virtual_number_requests r
    where r.user_id = p_user_id and r.status in ('provider_started','provider_attached','manual_review')
      and r.request_id <> p_request_id
  ) then
    raise exception 'A previous 5SIM request is still pending reconciliation';
  end if;

  insert into public.wallets(user_id) values (p_user_id) on conflict (user_id) do nothing;
  select w.balance into v_balance from public.wallets w where w.user_id = p_user_id for update;
  if v_balance < p_final_price then raise exception 'Insufficient wallet balance'; end if;
  v_new_balance := pg_catalog.round(v_balance - p_final_price, 8);
  if v_new_balance < 0 then raise exception 'Insufficient wallet balance'; end if;

  update public.wallets set balance = v_new_balance, updated_at = now() where user_id = p_user_id;
  insert into public.wallet_transactions(
    user_id, transaction_type, direction, amount, balance_after, currency,
    reference_type, reference_id, description
  ) values (
    p_user_id, 'purchase', 'debit', p_final_price, v_new_balance, 'USDT',
    'virtual_number_request', p_request_id, '5SIM virtual number wallet reservation'
  );

  if v_retry_refunded then
    update private.virtual_number_requests set
      country_code = p_country_code, service_code = p_service_code, operator = p_operator,
      reserved_base_price = p_base_price, reserved_final_price = p_final_price,
      actual_base_price = null, actual_final_price = null, provider_activation_id = null,
      provider_price = null, phone = null, provider_status = null, provider_expires_at = null,
      status = 'provider_started', reason = null, activation_id = null,
      attempt_count = attempt_count + 1, updated_at = now()
    where request_id = p_request_id;
  else
    insert into private.virtual_number_requests(
      request_id, user_id, country_code, service_code, operator,
      reserved_base_price, reserved_final_price, status
    ) values (
      p_request_id, p_user_id, p_country_code, p_service_code, p_operator,
      p_base_price, p_final_price, 'provider_started'
    );
  end if;
  return pg_catalog.jsonb_build_object('should_call_provider', true, 'reserved_price', p_final_price);
end;
$$;

create or replace function public.virtual_numbers_attach_provider_activation(
  p_user_id uuid,
  p_request_id uuid,
  p_provider_activation_id bigint,
  p_provider_price numeric,
  p_phone text,
  p_status text,
  p_expires_at timestamptz
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_request private.virtual_number_requests%rowtype;
begin
  select * into v_request from private.virtual_number_requests r
   where r.request_id = p_request_id and r.user_id = p_user_id for update;
  if not found then raise exception 'Reserved request not found'; end if;
  if v_request.provider_activation_id is not null
     and v_request.provider_activation_id <> p_provider_activation_id then
    raise exception 'Request is already linked to a different provider activation';
  end if;
  if v_request.status = 'refunded' then raise exception 'Request was already refunded'; end if;
  update private.virtual_number_requests set
    provider_activation_id = p_provider_activation_id,
    provider_price = nullif(p_provider_price, 0),
    phone = p_phone,
    provider_status = p_status,
    provider_expires_at = p_expires_at,
    status = case when status = 'committed' then status else 'provider_attached' end,
    updated_at = now()
  where request_id = p_request_id;
  return pg_catalog.jsonb_build_object(
    'id', p_provider_activation_id, 'phone', p_phone, 'status', p_status,
    'expires', p_expires_at, 'product', v_request.service_code,
    'country', v_request.country_code, 'operator', v_request.operator,
    'price', coalesce(nullif(p_provider_price, 0), 0)
  );
end;
$$;

create or replace function public.virtual_numbers_commit_purchase(
  p_user_id uuid,
  p_request_id uuid,
  p_provider_activation_id bigint,
  p_phone text,
  p_status text,
  p_expires_at timestamptz,
  p_base_price numeric,
  p_final_price numeric,
  p_country_code text,
  p_service_code text,
  p_operator text
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_request private.virtual_number_requests%rowtype;
  v_activation_id uuid;
  v_balance numeric(20,8);
  v_new_balance numeric(20,8);
  v_delta numeric(20,8);
begin
  select * into v_request from private.virtual_number_requests r
   where r.request_id = p_request_id and r.user_id = p_user_id for update;
  if not found then raise exception 'Reserved request not found'; end if;
  if v_request.status = 'committed' then
    return private.virtual_number_activation_json(v_request.activation_id);
  end if;
  if v_request.status = 'refunded' then raise exception 'Request was already refunded'; end if;
  if v_request.provider_activation_id is distinct from p_provider_activation_id then
    raise exception 'Provider activation ID does not match the reservation';
  end if;
  if p_phone is null or p_phone = '' or p_base_price <= 0 or p_final_price < p_base_price then
    raise exception 'Invalid final provider activation details';
  end if;

  v_delta := pg_catalog.round(p_final_price - v_request.reserved_final_price, 8);
  if v_delta > 0 then
    select w.balance into v_balance from public.wallets w where w.user_id = p_user_id for update;
    if v_balance < v_delta then
      update private.virtual_number_requests set
        status = 'manual_review', actual_base_price = p_base_price,
        actual_final_price = p_final_price,
        reason = 'Provider final price exceeded reservation; additional wallet balance required',
        updated_at = now()
      where request_id = p_request_id;
      return pg_catalog.jsonb_build_object('manual_review', true, 'reason', 'Wallet balance cannot cover final provider price');
    end if;
    v_new_balance := pg_catalog.round(v_balance - v_delta, 8);
    update public.wallets set balance = v_new_balance, updated_at = now() where user_id = p_user_id;
    insert into public.wallet_transactions(
      user_id, transaction_type, direction, amount, balance_after, currency,
      reference_type, reference_id, description
    ) values (
      p_user_id, 'purchase', 'debit', v_delta, v_new_balance, 'USDT',
      'virtual_number_price_adjustment', p_request_id, '5SIM final-price adjustment'
    );
  elsif v_delta < 0 then
    v_delta := abs(v_delta);
    select w.balance into v_balance from public.wallets w where w.user_id = p_user_id for update;
    v_new_balance := pg_catalog.round(v_balance + v_delta, 8);
    update public.wallets set balance = v_new_balance, updated_at = now() where user_id = p_user_id;
    insert into public.wallet_transactions(
      user_id, transaction_type, direction, amount, balance_after, currency,
      reference_type, reference_id, description
    ) values (
      p_user_id, 'refund', 'credit', v_delta, v_new_balance, 'USDT',
      'virtual_number_price_adjustment', p_request_id, '5SIM final-price adjustment refund'
    );
  end if;

  insert into private.virtual_number_activations(
    user_id, request_id, provider_activation_id, phone, country_code,
    service_code, operator, status, expires_at, base_price, charged_price
  ) values (
    p_user_id, p_request_id, p_provider_activation_id, p_phone, p_country_code,
    p_service_code, p_operator, coalesce(p_status, 'PENDING'), p_expires_at,
    p_base_price, p_final_price
  ) returning activation_id into v_activation_id;

  update private.virtual_number_requests set
    status = 'committed', activation_id = v_activation_id,
    actual_base_price = p_base_price, actual_final_price = p_final_price,
    phone = p_phone, provider_status = p_status, updated_at = now()
  where request_id = p_request_id;
  return private.virtual_number_activation_json(v_activation_id);
end;
$$;

create or replace function public.virtual_numbers_refund_wallet(
  p_user_id uuid,
  p_request_id uuid,
  p_reason text
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_request private.virtual_number_requests%rowtype;
  v_amount numeric(20,8);
  v_balance numeric(20,8);
  v_new_balance numeric(20,8);
begin
  select * into v_request from private.virtual_number_requests r
   where r.request_id = p_request_id and r.user_id = p_user_id for update;
  if not found then raise exception 'Virtual number wallet reservation not found'; end if;
  if v_request.status = 'refunded' then
    return pg_catalog.jsonb_build_object('refunded', true, 'already_refunded', true);
  end if;
  if v_request.status not in ('provider_started','provider_attached','committed','manual_review') then
    raise exception 'Request cannot be refunded in its current state';
  end if;
  v_amount := case
    when v_request.status = 'manual_review' then v_request.reserved_final_price
    else coalesce(v_request.actual_final_price, v_request.reserved_final_price)
  end;
  select w.balance into v_balance from public.wallets w where w.user_id = p_user_id for update;
  v_new_balance := pg_catalog.round(v_balance + v_amount, 8);
  update public.wallets set balance = v_new_balance, updated_at = now() where user_id = p_user_id;
  insert into public.wallet_transactions(
    user_id, transaction_type, direction, amount, balance_after, currency,
    reference_type, reference_id, description
  ) values (
    p_user_id, 'refund', 'credit', v_amount, v_new_balance, 'USDT',
    'virtual_number_refund', p_request_id,
    left(coalesce(p_reason, '5SIM virtual number refund'), 250)
  );
  update private.virtual_number_requests set status = 'refunded',
    reason = left(coalesce(p_reason, 'Refunded'), 300), updated_at = now()
  where request_id = p_request_id;
  return pg_catalog.jsonb_build_object('refunded', true, 'amount', v_amount, 'balance_after', v_new_balance);
end;
$$;

create or replace function public.virtual_numbers_get_my_activation(
  p_user_id uuid,
  p_activation_id uuid
)
returns jsonb
language sql
stable
security definer
set search_path = ''
as $$
  select private.virtual_number_activation_json(a.activation_id)
  from private.virtual_number_activations a
  where a.activation_id = p_activation_id and a.user_id = p_user_id;
$$;

create or replace function public.virtual_numbers_sync_activation(
  p_user_id uuid,
  p_activation_id uuid,
  p_status text,
  p_sms_code text,
  p_sms_text text,
  p_expires_at timestamptz
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_claimed boolean;
  v_activation_id uuid;
  v_previous_code text;
  v_result jsonb;
begin
  -- Lock before comparing/updating so concurrent foreground and scheduler polls
  -- can claim a newly arrived code exactly once.
  select a.activation_id, a.last_sms_code
    into v_activation_id, v_previous_code
  from private.virtual_number_activations a
  where a.activation_id = p_activation_id and a.user_id = p_user_id
  for update;
  if v_activation_id is null then raise exception 'Activation not found'; end if;
  v_claimed := p_sms_code is not null and p_sms_code is distinct from v_previous_code;

  update private.virtual_number_activations a set
    status = coalesce(p_status, a.status),
    sms_code = coalesce(p_sms_code, a.sms_code),
    sms_text = coalesce(p_sms_text, a.sms_text),
    last_sms_code = case when v_claimed then p_sms_code else a.last_sms_code end,
    expires_at = coalesce(p_expires_at, a.expires_at),
    updated_at = now()
  where a.activation_id = v_activation_id;
  v_result := private.virtual_number_activation_json(v_activation_id);
  return v_result || pg_catalog.jsonb_build_object('notification_claimed', coalesce(v_claimed, false));
end;
$$;

create or replace function public.virtual_numbers_get_my_latest_activation(p_user_id uuid)
returns jsonb
language sql
stable
security definer
set search_path = ''
as $$
  select private.virtual_number_activation_json(a.activation_id)
  from private.virtual_number_activations a
  where a.user_id = p_user_id
  order by a.created_at desc
  limit 1;
$$;

create or replace function public.virtual_numbers_register_fcm_token(
  p_user_id uuid,
  p_fcm_token text,
  p_platform text
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  if p_user_id is null or p_fcm_token is null or pg_catalog.length(p_fcm_token) < 32 then
    raise exception 'Invalid FCM token';
  end if;
  insert into private.virtual_number_fcm_tokens(fcm_token, user_id, platform)
  values (p_fcm_token, p_user_id, coalesce(nullif(p_platform, ''), 'android'))
  on conflict (fcm_token) do update set
    user_id = excluded.user_id, platform = excluded.platform, updated_at = now();
end;
$$;

create or replace function public.virtual_numbers_get_active_for_push()
returns setof jsonb
language sql
stable
security definer
set search_path = ''
as $$
  select private.virtual_number_activation_json(a.activation_id)
    || pg_catalog.jsonb_build_object('user_id', a.user_id)
  from private.virtual_number_activations a
  where a.status in ('PENDING','WAITING','ACTIVE','RECEIVED')
    and (a.expires_at is null or a.expires_at > now() - interval '15 minutes');
$$;

create or replace function public.virtual_numbers_get_user_fcm_tokens(p_user_id uuid)
returns setof jsonb
language sql
stable
security definer
set search_path = ''
as $$
  select pg_catalog.jsonb_build_object('fcm_token', t.fcm_token)
  from private.virtual_number_fcm_tokens t
  where t.user_id = p_user_id;
$$;

-- RPCs are only called from the authenticated/service-role Edge Function, never Android.
do $$
declare
  fn record;
begin
  for fn in
    select p.oid::regprocedure as signature
    from pg_proc p join pg_namespace n on n.oid = p.pronamespace
    where n.nspname = 'public' and p.proname like 'virtual_numbers_%'
  loop
    execute pg_catalog.format('revoke all on function %s from public, anon, authenticated', fn.signature);
    execute pg_catalog.format('grant execute on function %s to service_role', fn.signature);
  end loop;
end;
$$;
