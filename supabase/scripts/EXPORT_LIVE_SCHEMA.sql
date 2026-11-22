-- READ-ONLY live schema export for Number One.
-- Run each SELECT in Supabase SQL Editor and save the complete results.
-- This script performs no INSERT, UPDATE, DELETE, DDL, migration, or function call.

-- 1) Function definitions, SECURITY DEFINER flag, search_path, and current ACL.
select n.nspname as schema_name,
       p.proname as function_name,
       pg_get_function_identity_arguments(p.oid) as arguments,
       p.prosecdef as security_definer,
       p.proconfig as function_config,
       p.proacl as acl,
       pg_get_functiondef(p.oid) as definition
from pg_proc p
join pg_namespace n on n.oid = p.pronamespace
where n.nspname in ('public', 'private')
order by n.nspname, p.proname, pg_get_function_identity_arguments(p.oid);

-- 2) Every column in the application schemas (public and private).
select table_schema, table_name, ordinal_position, column_name,
       data_type, udt_name, is_nullable, column_default
from information_schema.columns
where table_schema in ('public', 'private')
order by table_schema, table_name, ordinal_position;

-- 3) Index definitions in the application schemas.
select schemaname, tablename, indexname, indexdef
from pg_indexes
where schemaname in ('public', 'private')
order by schemaname, tablename, indexname;

-- 4) Table constraints in the application schemas.
select n.nspname as schema_name,
       c.relname as table_name,
       con.conname as constraint_name,
       con.contype as constraint_type,
       pg_get_constraintdef(con.oid, true) as definition
from pg_constraint con
join pg_class c on c.oid = con.conrelid
join pg_namespace n on n.oid = c.relnamespace
where n.nspname in ('public', 'private')
order by n.nspname, c.relname, con.conname;

-- 5) RLS state for every application table.
select n.nspname as schema_name,
       c.relname as table_name,
       c.relrowsecurity as rls_enabled,
       c.relforcerowsecurity as force_rls
from pg_class c
join pg_namespace n on n.oid = c.relnamespace
where c.relkind in ('r', 'p')
  and n.nspname in ('public', 'private')
order by n.nspname, c.relname;

-- 6) Every RLS policy and its expressions.
select schemaname, tablename, policyname, permissive, roles,
       cmd, qual as using_expression, with_check as check_expression
from pg_policies
where schemaname in ('public', 'private')
order by schemaname, tablename, policyname;
