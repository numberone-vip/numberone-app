alter table public.payment_settings
  alter column network set default 'BEP20';
alter table public.payment_settings
  drop constraint if exists payment_settings_enabled_requires_wallet;
alter table public.payment_settings
  drop constraint if exists payment_settings_trc20_wallet_format;
alter table public.payment_settings
  add constraint payment_settings_bep20_wallet_format
  check (wallet_address is null or wallet_address ~ '^0x[a-fA-F0-9]{40}$');
alter table public.payment_settings
  add constraint payment_settings_enabled_requires_bep20_wallet
  check (not is_enabled or (wallet_address is not null and wallet_address ~ '^0x[a-fA-F0-9]{40}$' and currency = 'USDT' and network = 'BEP20'));
update public.payment_settings set currency='USDT',network='BEP20',is_enabled=false,updated_at=now() where id=1;

create or replace function public.admin_approve_purchase_request(p_request_id uuid,p_service_id text,p_phone_number text,p_access_code text,p_expiry_date date,p_admin_note text default null)
returns uuid language plpgsql set search_path='' as $function$
declare v_request public.purchase_requests%rowtype; v_service public.services%rowtype; v_subscription_id uuid;
begin
 if not private.is_admin() then raise exception 'Not authorized'; end if;
 select * into v_request from public.purchase_requests where id=p_request_id for update;
 if not found then raise exception 'Purchase request not found'; end if;
 if lower(coalesce(v_request.status,'pending'))<>'pending' then raise exception 'Purchase request is no longer pending'; end if;
 if v_request.user_id is null then raise exception 'Purchase request has no linked customer account'; end if;
 select * into v_service from public.services where id=p_service_id for share;
 if not found then raise exception 'Selected service not found'; end if;
 if lower(btrim(coalesce(v_request.country_name,v_request.country,'')))<>lower(btrim(v_service.country)) then raise exception 'Selected service country does not match the purchase request'; end if;
 v_subscription_id:=public.admin_add_subscription_for_user(v_request.user_id,p_service_id,p_access_code,p_phone_number,p_expiry_date,coalesce(nullif(btrim(p_admin_note),''),'Approved purchase request '||p_request_id::text));
 update public.purchase_requests set status='approved' where id=p_request_id and lower(coalesce(status,'pending'))='pending';
 if not found then raise exception 'Purchase request status changed during approval'; end if;
 return v_subscription_id;
end; $function$;

create or replace function public.admin_reject_purchase_request(p_request_id uuid)
returns boolean language plpgsql set search_path='' as $function$
begin
 if not private.is_admin() then raise exception 'Not authorized'; end if;
 update public.purchase_requests set status='rejected' where id=p_request_id and lower(coalesce(status,'pending'))='pending';
 if not found then raise exception 'Purchase request not found or no longer pending'; end if;
 return true;
end; $function$;

revoke all on function public.admin_approve_purchase_request(uuid,text,text,text,date,text) from public,anon;
revoke all on function public.admin_reject_purchase_request(uuid) from public,anon;
grant execute on function public.admin_approve_purchase_request(uuid,text,text,text,date,text) to authenticated;
grant execute on function public.admin_reject_purchase_request(uuid) to authenticated;
