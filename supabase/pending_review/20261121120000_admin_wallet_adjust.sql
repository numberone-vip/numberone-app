-- Number One: admin wallet adjustment + admin id list. NEW functions only (no "or replace"),
-- so nothing existing can be overwritten; the migration fails loudly if a name already exists.
-- Authorization: private.is_admin() only (single administrator; finance/orders staff roles are NOT accepted).

create function public.admin_adjust_wallet_balance(
  p_user_id uuid, p_amount numeric, p_note text, p_request_id uuid
) returns numeric
language plpgsql
security definer
set search_path = ''
as $$
declare v_balance numeric; v_new numeric; v_prev_result numeric;
begin
  if not private.is_admin() then raise exception 'not authorized'; end if;
  if p_request_id is null then raise exception 'request id required'; end if;
  if p_amount is null or p_amount = 0 or abs(p_amount) > 1000000 then raise exception 'invalid amount'; end if;
  if coalesce(pg_catalog.btrim(p_note), '') = '' then raise exception 'audit note required'; end if;

  insert into public.wallets(user_id) values (p_user_id) on conflict (user_id) do nothing;
  -- Row lock first: concurrent calls for the same wallet are serialized, so the duplicate check below is race-safe.
  select w.balance into v_balance from public.wallets w where w.user_id = p_user_id for update;

  select t.balance_after into v_prev_result
  from public.wallet_transactions t
  where t.user_id = p_user_id and t.reference_type = 'admin_adjustment' and t.reference_id = p_request_id
  limit 1;
  if found then return v_prev_result; end if;  -- same request already applied: no second change

  v_new := pg_catalog.round(v_balance + p_amount, 8);
  if v_new < 0 then raise exception 'Insufficient wallet balance'; end if;

  update public.wallets set balance = v_new, updated_at = now() where user_id = p_user_id;
  insert into public.wallet_transactions(
    user_id, transaction_type, direction, amount, balance_after, currency,
    reference_type, reference_id, description
  ) values (
    p_user_id, 'admin_adjustment', case when p_amount > 0 then 'credit' else 'debit' end,
    abs(p_amount), v_new, 'USDT', 'admin_adjustment', p_request_id,
    pg_catalog.btrim(p_note) || ' | previous balance: ' || v_balance::text || ' | new balance: ' || v_new::text
  );
  return v_new;
end;
$$;

create function public.admin_list_admin_ids()
returns table(user_id uuid)
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
  if not private.is_admin() then raise exception 'not authorized'; end if;
  return query select a.user_id from public.admin_users a;
end;
$$;

revoke all on function public.admin_adjust_wallet_balance(uuid, numeric, text, uuid) from public, anon, authenticated;
revoke all on function public.admin_list_admin_ids() from public, anon, authenticated;
grant execute on function public.admin_adjust_wallet_balance(uuid, numeric, text, uuid) to authenticated;
grant execute on function public.admin_list_admin_ids() to authenticated;
