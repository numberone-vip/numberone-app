-- APPLIED MANUALLY on 2026-11-21. Service-role only: links a matched Binance deposit to its request.
create or replace function public.wallet_attach_matched_deposit(p_request_id uuid, p_transaction_hash text)
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
begin
  if coalesce((auth.jwt() ->> 'role'), '') <> 'service_role' then
    raise exception 'Service role required';
  end if;
  if p_transaction_hash is null or p_transaction_hash !~* '^[0-9a-f]{64}$' then
    raise exception 'Invalid transaction hash';
  end if;
  update public.wallet_deposit_requests
  set transaction_hash = lower(p_transaction_hash), status = 'submitted', submitted_at = now()
  where id = p_request_id and status = 'pending' and transaction_hash is null
    and created_at > now() - interval '24 hours';
  if not found then
    raise exception 'Deposit request not found, expired, or already matched';
  end if;
  return true;
exception
  when unique_violation then
    raise exception 'This transaction hash is already linked to another request';
end;
$$;

revoke all on function public.wallet_attach_matched_deposit(uuid, text) from public, anon, authenticated;
grant execute on function public.wallet_attach_matched_deposit(uuid, text) to service_role;
