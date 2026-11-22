-- Number One: read-only admin ledger over public.wallet_transactions.
-- NEW function only (no "or replace"). Admin-only via private.is_admin(). Read-only: no writes.
-- Not applied automatically: review, then run in the Supabase SQL editor (or supabase db push).
-- Columns used match the existing admin_adjust_wallet_balance migration and the WalletTransaction model.
-- Text casts keep the return shape stable even if an id or timestamp column is uuid or timestamptz.

create function public.admin_list_wallet_transactions(p_limit integer default 100)
returns table(
  id text, user_id text, transaction_type text, direction text, amount numeric,
  balance_after numeric, currency text, reference_type text, reference_id text,
  transaction_hash text, description text, created_at text
)
language plpgsql
stable
security definer
set search_path = ''
as $$
#variable_conflict use_column
begin
  if not private.is_admin() then raise exception 'not authorized'; end if;
  return query
    select t.id::text, t.user_id::text, t.transaction_type::text, t.direction::text, t.amount,
           t.balance_after, t.currency::text, t.reference_type::text, t.reference_id::text,
           t.transaction_hash::text, t.description::text, t.created_at::text
    from public.wallet_transactions t
    order by t.created_at desc
    limit greatest(1, least(coalesce(p_limit, 100), 500));
end;
$$;

revoke all on function public.admin_list_wallet_transactions(integer) from public, anon, authenticated;
grant execute on function public.admin_list_wallet_transactions(integer) to authenticated;
