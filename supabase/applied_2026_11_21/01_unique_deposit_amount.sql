-- APPLIED MANUALLY in Supabase SQL Editor (PRODUCTION) on 2026-11-21.
-- Unique amount per USDT deposit request (base + 4-decimal suffix), avoiding open-request collisions.
create or replace function private.unique_deposit_amount(p_base numeric)
returns numeric
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_candidate numeric;
  v_try integer;
begin
  if p_base is null or p_base <= 0 then raise exception 'invalid base amount'; end if;
  for v_try in 1..50 loop
    v_candidate := round(p_base + (1 + floor(random() * 9999))::numeric / 10000, 4);
    if not exists (
      select 1 from public.wallet_deposit_requests r
      where r.requested_amount = v_candidate
        and r.created_at > now() - interval '24 hours'
    ) then
      return v_candidate;
    end if;
  end loop;
  raise exception 'could not allocate a unique deposit amount; try again';
end;
$$;

revoke all on function private.unique_deposit_amount(numeric) from public, anon, authenticated;

create or replace function public.wallet_create_deposit_request_v2(p_amount numeric)
returns table(request_id uuid, amount_to_send numeric)
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_user_id uuid := auth.uid();
  v_amount numeric;
  v_id uuid;
begin
  if v_user_id is null then raise exception 'Authentication required'; end if;
  if p_amount is null or p_amount <= 0 or p_amount > 100000 or p_amount <> trunc(p_amount) then
    raise exception 'Invalid deposit amount';
  end if;
  v_amount := private.unique_deposit_amount(p_amount);
  insert into public.wallet_deposit_requests(user_id, requested_amount)
  values (v_user_id, v_amount)
  returning id into v_id;
  return query select v_id, v_amount;
end;
$$;

revoke all on function public.wallet_create_deposit_request_v2(numeric) from public, anon;
grant execute on function public.wallet_create_deposit_request_v2(numeric) to authenticated;
