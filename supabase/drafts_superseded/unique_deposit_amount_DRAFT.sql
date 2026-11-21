-- Number One: DRAFT ONLY. NOT a migration. Do NOT apply until reviewed against the live definitions.
-- Idea: each USDT deposit request gets a unique amount (base + small cents suffix),
-- e.g. 15.0437. The server matches Binance deposits by exact amount + BEP20 + address.
-- Users no longer type a 64-character TXID for matching.
-- ASSUMPTIONS (verify against live schema before applying):
--   * public.wallet_deposit_requests has requested_amount numeric, currency, network, status, created_at.
--   * "open" statuses below are assumptions; confirm the real values (verify-binance-deposit uses 'submitted').

-- 1) Picks a suffix not used by any open request in the last 24 hours.
create or replace function private.unique_deposit_amount(p_base numeric)
returns numeric
language plpgsql
volatile
security definer
set search_path = ''
as $$
declare
  v_candidate numeric;
  v_try integer;
begin
  if p_base is null or p_base <= 0 then raise exception 'invalid base amount'; end if;
  for v_try in 1..50 loop
    -- suffix between 0.0001 and 0.9999 (4 decimals), so the amount stays readable
    v_candidate := pg_catalog.round(p_base + (1 + floor(random() * 9999))::numeric / 10000, 4);
    if not exists (
      select 1 from public.wallet_deposit_requests r
      where r.requested_amount = v_candidate
        and r.currency = 'USDT' and r.network = 'BEP20'
        and r.created_at > now() - interval '24 hours'
        and lower(r.status) in ('pending', 'submitted')  -- ASSUMPTION: confirm open statuses
    ) then
      return v_candidate;
    end if;
  end loop;
  raise exception 'could not allocate a unique deposit amount; try again';
end;
$$;

revoke all on function private.unique_deposit_amount(numeric) from public, anon, authenticated;

-- 2) TODO after live definitions are provided:
--    In wallet_create_deposit_request: requested_amount := private.unique_deposit_amount(p_amount);
--    Return the exact requested_amount so the app shows it with a copy button.
--    Keep the existing verify-binance-deposit check (exact amount, BSC, address, status = 1) unchanged.
--    Remove the TXID requirement from the user flow only after matching works end to end.
--
-- Known limits to handle in the app:
--   * A user who pays a different amount will not match; show a clear "amount must be exactly X" message.
--   * Keep the TXID as a fallback for support review, not as the main path.
