-- Number One: DRAFT ONLY. NOT a migration. Review, then run in SQL Editor (PRODUCTION) yourself.
-- Prerequisites already done: private.owner_auto_approve_config + owner_auto_approve_allowed(uuid, numeric)
-- (feature flag starts DISABLED, cap 15 USD), and public.app_settings.syp_usd_rate = 16000.
--
-- What changes: only the PUBLIC wrapper wallet_submit_sham_cash_topup_receipt.
-- The private submit function is NOT touched. After a successful submit, the wrapper may call the
-- existing official approval private.admin_approve_sham_cash_topup, using the same code path.
--
-- STEP 0 (run first, read-only): confirm the public wrapper's argument names and types.
--   select pg_get_function_arguments('public.wallet_submit_sham_cash_topup_receipt'::regproc);
-- If the result differs from (p_request_id uuid, p_receipt_path text), STOP and send it to me.

create or replace function public.wallet_submit_sham_cash_topup_receipt(
  p_request_id uuid,
  p_receipt_path text
)
returns boolean
language plpgsql
security invoker
set search_path = ''
as $$
declare
  v_user_id uuid := auth.uid();
  v_req public.wallet_deposit_requests%rowtype;
  v_rate numeric;
  v_usd numeric(20,8);
begin
  -- 1) Customer submit, exactly as before (same private function, same checks).
  if not private.wallet_submit_sham_cash_topup_receipt(p_request_id, p_receipt_path) then
    return false;
  end if;

  -- 2) Owner-only automatic approval. Any doubt => leave for manual review.
  begin
    select * into v_req
    from public.wallet_deposit_requests
    where id = p_request_id and user_id = v_user_id;

    if not found or v_req.status <> 'submitted' or v_req.paid_amount is null or v_req.paid_amount <= 0 then
      return true;
    end if;

    if v_req.payment_method = 'SHAM_CASH_USD' then
      v_usd := floor(v_req.paid_amount * 100) / 100;          -- already USD
    elsif v_req.payment_method = 'SHAM_CASH_SYP' then
      select s.syp_usd_rate into v_rate from public.app_settings s where s.singleton limit 1;
      if v_rate is null or v_rate <= 0 then
        return true;                                          -- no rate: manual review
      end if;
      v_usd := floor((v_req.paid_amount / v_rate) * 100) / 100; -- round DOWN, never overpay
    else
      return true;
    end if;

    -- Cap and admin check. Uses the flag and the cap (15 USD) defined in the draft config.
    if not private.owner_auto_approve_allowed(v_user_id, v_usd) then
      return true;
    end if;

    perform private.admin_approve_sham_cash_topup(p_request_id, v_usd);
  exception
    when others then
      -- Approval failed: the receipt stays submitted and goes to manual review.
      raise notice 'auto-approve skipped: %', sqlerrm;
  end;

  return true;
end;
$$;

revoke all on function public.wallet_submit_sham_cash_topup_receipt(uuid, text) from public, anon;
grant execute on function public.wallet_submit_sham_cash_topup_receipt(uuid, text) to authenticated;

-- STEP 3 (run only when you decide, NOT now): enable owner auto-approval.
--   update private.owner_auto_approve_config set enabled = true where singleton;
-- To disable instantly at any time:
--   update private.owner_auto_approve_config set enabled = false where singleton;
