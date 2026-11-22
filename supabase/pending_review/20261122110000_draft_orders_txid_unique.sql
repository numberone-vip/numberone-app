-- Number One: DRAFT. Prevents the same transaction hash from being used by two live orders.
-- NOT applied automatically. Before applying, run supabase/scripts/EXPORT_LIVE_SCHEMA.sql and confirm
-- that public.orders has columns transaction_ref and status with these exact names.
-- The DO block refuses to continue if the column is missing or if duplicates already exist.
-- Rejected/cancelled/expired/failed/declined orders are excluded, so a hash from a rejected order can be reused.
-- Status is compared with lower(status): live values are uppercase (e.g. PAYMENT_REVIEW).

do $$
begin
  if not exists (select 1 from information_schema.columns
                 where table_schema = 'public' and table_name = 'orders' and column_name = 'transaction_ref') then
    raise exception 'public.orders.transaction_ref not found: confirm the column name before applying';
  end if;
  if exists (select 1 from information_schema.columns
             where table_schema = 'public' and table_name = 'orders' and column_name = 'status') is not true then
    raise exception 'public.orders.status not found: confirm the column name before applying';
  end if;
  if exists (
    select 1 from public.orders
    where transaction_ref is not null and btrim(transaction_ref) <> ''
      and lower(status) not in ('rejected', 'cancelled', 'canceled', 'expired', 'failed', 'declined')
    group by lower(regexp_replace(btrim(transaction_ref), '^0[xX]', ''))
    having count(*) > 1
  ) then
    raise exception 'duplicate transaction refs already exist on live orders: resolve them before applying';
  end if;
end;
$$;

create unique index orders_transaction_ref_unique
  on public.orders (lower(regexp_replace(btrim(transaction_ref), '^0[xX]', '')))
  where transaction_ref is not null and btrim(transaction_ref) <> ''
    and lower(status) not in ('rejected', 'cancelled', 'canceled', 'expired', 'failed', 'declined');
