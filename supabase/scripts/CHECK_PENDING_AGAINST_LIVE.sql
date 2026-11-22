-- READ-ONLY. Run each SELECT separately in the Supabase SQL Editor and send the results back.
-- No INSERT/UPDATE/DELETE/DDL. Purpose: learn which "pending_review" migrations are already live.

-- 1) The live migration history (exact versions and names). This is the list the local folder must match.
select version, name
from supabase_migrations.schema_migrations
order by version;

-- 2) Which functions referenced by the pending files already exist live, and with which arguments.
select n.nspname as schema_name, p.proname as function_name,
       pg_get_function_identity_arguments(p.oid) as arguments,
       p.prosecdef as security_definer
from pg_proc p
join pg_namespace n on n.oid = p.pronamespace
where n.nspname in ('public', 'private')
  and p.proname in (
    'is_admin',
    'admin_adjust_wallet_balance', 'admin_list_admin_ids', 'admin_list_wallet_transactions',
    'admin_reset_subscription_access', 'admin_set_admin_role',
    'admin_log_action', 'admin_list_audit_log',
    'admin_upsert_service', 'admin_delete_service', 'admin_list_services', 'admin_update_payment_config'
  )
order by n.nspname, p.proname;

-- 3) admin_users: does it exist and what columns does it have? (needed by admin_set_admin_role / admin_list_admin_ids)
select table_schema, table_name, column_name, data_type, is_nullable, column_default
from information_schema.columns
where table_name in ('admin_users', 'admin_audit_log')
  and table_schema in ('public', 'private')
order by table_schema, table_name, ordinal_position;

-- 4) Does the unique TXID index (draft_orders_txid_unique migration) already exist?
select schemaname, tablename, indexname, indexdef
from pg_indexes
where schemaname = 'public' and indexname = 'orders_transaction_ref_unique';
