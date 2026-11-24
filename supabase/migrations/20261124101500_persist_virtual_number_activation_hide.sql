-- Persist the customer's hide action for terminal 5SIM activations.
alter table private.virtual_number_activations
  add column if not exists is_hidden boolean not null default false;

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
    'operator', a.operator,
    'is_hidden', a.is_hidden
  )
  from private.virtual_number_activations a
  where a.activation_id = p_activation_id;
$$;

-- The caller can hide only their own terminal activation.
create or replace function public.virtual_numbers_hide_my_activation(p_activation_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_user_id uuid := auth.uid();
  v_result jsonb;
begin
  if v_user_id is null then
    raise exception 'Authentication required';
  end if;

  update private.virtual_number_activations a
  set is_hidden = true, updated_at = now()
  where a.activation_id = p_activation_id
    and a.user_id = v_user_id
    and a.status not in ('PENDING', 'WAITING', 'ACTIVE', 'RECEIVED')
  returning private.virtual_number_activation_json(a.activation_id)
  into v_result;

  if v_result is null then
    raise exception 'Activation not found or still active';
  end if;

  return v_result;
end;
$$;

revoke all on function public.virtual_numbers_hide_my_activation(uuid) from public, anon;
grant execute on function public.virtual_numbers_hide_my_activation(uuid) to authenticated;

-- Hidden activations no longer appear as the customer's latest activation.
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
    and a.is_hidden = false
  order by a.created_at desc
  limit 1;
$$;
revoke all on function public.virtual_numbers_get_my_latest_activation(uuid) from public, anon, authenticated;
grant execute on function public.virtual_numbers_get_my_latest_activation(uuid) to service_role;

-- Keep hidden activations out of scheduled push notification polling.
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
    and a.is_hidden = false
    and (a.expires_at is null or a.expires_at > now() - interval '15 minutes');
$$;
revoke all on function public.virtual_numbers_get_active_for_push() from public, anon, authenticated;
grant execute on function public.virtual_numbers_get_active_for_push() to service_role;
