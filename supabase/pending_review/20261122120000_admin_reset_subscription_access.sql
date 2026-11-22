-- Number One: admin-only reset of an existing subscription access code/expiry.
-- This matches the live subscriptions schema: access_code_hash is stored as SHA-256,
-- never as plaintext. Review before applying; this file is NOT applied automatically.

create function public.admin_reset_subscription_access(
  p_subscription_id uuid,
  p_access_code text default null,
  p_expiry_date date default null
) returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_hash text;
begin
  if not private.is_admin() then
    raise exception 'Not authorized';
  end if;

  if p_subscription_id is null then
    raise exception 'Subscription id is required';
  end if;

  if p_access_code is null and p_expiry_date is null then
    raise exception 'Nothing to update';
  end if;

  if p_access_code is not null then
    if length(pg_catalog.btrim(p_access_code)) not between 8 and 256 then
      raise exception 'Access code must be between 8 and 256 characters';
    end if;
    v_hash := encode(
      extensions.digest(convert_to(pg_catalog.btrim(p_access_code), 'UTF8'), 'sha256'),
      'hex'
    );
  end if;

  if p_expiry_date is not null and p_expiry_date < current_date then
    raise exception 'Expiry date cannot be in the past';
  end if;

  update public.subscriptions
     set access_code_hash = coalesce(v_hash, access_code_hash),
         expiry_date = coalesce(p_expiry_date, expiry_date)
   where id = p_subscription_id;

  return found;
end;
$$;

revoke all on function public.admin_reset_subscription_access(uuid, text, date) from public, anon, authenticated;
grant execute on function public.admin_reset_subscription_access(uuid, text, date) to authenticated;
