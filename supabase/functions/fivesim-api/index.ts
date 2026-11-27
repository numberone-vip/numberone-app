// Supabase Edge Function: fivesim-api (extends the existing v1 deployment)
// Android sends only an action and user-selected product identifiers. 5SIM credentials
// and all wallet operations remain server-side. See backend/SUPABASE_VIRTUAL_NUMBERS.md.
import { createClient, type SupabaseClient } from "npm:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};
const json = (value: unknown, status = 200) => new Response(JSON.stringify(value), {
  status, headers: { ...corsHeaders, "Content-Type": "application/json" },
});
const FIVE_SIM_BASE = "https://5sim.net/v1";
let runtimeFiveSimApiKey = Deno.env.get("FIVESIM_API_KEY") ?? "";
const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";
const directAnonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
const publishableKeyMap = (() => {
  try { return JSON.parse(Deno.env.get("SUPABASE_PUBLISHABLE_KEYS") ?? "{}"); }
  catch { return {}; }
})();
const SUPABASE_ANON_KEY = directAnonKey || publishableKeyMap.default || publishableKeyMap.anon || "";
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";

class HttpError extends Error {
  constructor(message: string, readonly status = 400) { super(message); }
}

type ProviderOrder = {
  id: number;
  phone: string;
  operator?: string;
  product: string;
  price: number;
  status: string;
  expires?: string;
  sms?: Array<{ code?: string; text?: string; sender?: string }> | null;
  country?: string;
};

type PricingConfig = {
  wallet_currency: string;
  provider_to_wallet_rate: number;
  margin_mode: "percent" | "fixed";
  margin_percent: number;
  margin_fixed: number;
};

function requiredString(value: unknown, label: string): string {
  if (typeof value !== "string" || !value.trim()) throw new HttpError(`${label} is required`);
  return value.trim();
}

function roundMoney(value: number): number {
  return Math.round((value + Number.EPSILON) * 10000) / 10000;
}

function calculatePrice(basePrice: number, config: PricingConfig): number {
  const markup = config.margin_mode === "fixed"
    ? config.margin_fixed
    : basePrice * (config.margin_percent / 100);
  return roundMoney(basePrice + markup);
}

async function providerRequest(path: string, authenticated = true): Promise<any> {
  const headers: Record<string, string> = { Accept: "application/json" };
  if (authenticated) {
    if (!runtimeFiveSimApiKey) throw new HttpError("5SIM API key is not configured in Supabase", 503);
    headers.Authorization = `Bearer ${runtimeFiveSimApiKey}`;
  }
  const response = await fetch(`${FIVE_SIM_BASE}/${path.replace(/^\//, "")}`, { headers, signal: AbortSignal.timeout(20000) });
  const raw = await response.text();
  let payload: any = raw;
  try { payload = raw ? JSON.parse(raw) : {}; } catch { /* keep provider text */ }
  if (!response.ok) {
    const detail = typeof payload === "string" ? payload : JSON.stringify(payload);
    // Preserve provider status: a definitive 4xx can be released/refunded; a 5xx is
    // ambiguous because the provider may have processed the purchase before failing.
    throw new HttpError(`5SIM request failed (${response.status}): ${detail.slice(0, 350)}`, response.status);
  }
  return payload;
}

async function getPricingConfig(admin: SupabaseClient): Promise<PricingConfig> {
  const { data, error } = await admin.rpc("virtual_numbers_get_pricing_config");
  if (error || !data) throw new HttpError(`Pricing configuration is unavailable: ${error?.message ?? "empty result"}`, 503);
  const config = (Array.isArray(data) ? data[0] : data) as PricingConfig;
  if (!(Number(config.provider_to_wallet_rate) > 0)) throw new HttpError("Invalid provider-to-wallet conversion rate", 503);
  return {
    wallet_currency: config.wallet_currency || "USDT",
    provider_to_wallet_rate: Number(config.provider_to_wallet_rate),
    margin_mode: config.margin_mode === "fixed" ? "fixed" : "percent",
    margin_percent: Number(config.margin_percent ?? 0),
    margin_fixed: Number(config.margin_fixed ?? 0),
  };
}

async function requireUser(request: Request) {
  const authorization = request.headers.get("Authorization") ?? "";
  if (!authorization.startsWith("Bearer ")) throw new HttpError("Authentication required", 401);
  const userClient = createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
    global: { headers: { Authorization: authorization } },
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data, error } = await userClient.auth.getUser();
  if (error || !data.user) throw new HttpError("Invalid or expired session", 401);
  const admin = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  return { user: data.user, admin, userClient };
}

async function quoteFor(admin: SupabaseClient, countryCode: string, serviceCode: string) {
  const [rawPrices, config] = await Promise.all([
    providerRequest(`guest/prices?country=${encodeURIComponent(countryCode)}&product=${encodeURIComponent(serviceCode)}`, false),
    getPricingConfig(admin),
  ]);
  const byCountry = rawPrices?.[countryCode];
  const byProduct = byCountry?.[serviceCode] ?? {};
  const operators = Object.entries(byProduct as Record<string, any>).map(([name, data]) => {
    const raw = data as { cost?: number; count?: number; rate?: number };
    const basePrice = roundMoney(Number(raw.cost ?? 0) * config.provider_to_wallet_rate);
    return {
      name,
      success_rate: Number(raw.rate ?? 0),
      available_qty: Number(raw.count ?? 0),
      base_price: basePrice,
    };
  }).filter((operator) => operator.available_qty > 0 && operator.base_price > 0);
  return {
    quote: {
      country_code: countryCode,
      service_code: serviceCode,
      currency: config.wallet_currency,
      margin_mode: config.margin_mode,
      margin_percent: config.margin_percent,
      margin_fixed: config.margin_fixed,
      operators,
    },
    config,
  };
}

async function rpc(admin: SupabaseClient, name: string, args: Record<string, unknown>) {
  const { data, error } = await admin.rpc(name, args);
  if (error) throw new HttpError(`${name} failed: ${error.message}`, 500);
  return data;
}

function latestSms(providerOrder: ProviderOrder) {
  const sms = providerOrder.sms ?? [];
  const latest = sms.length ? sms[sms.length - 1] : null;
  return { code: latest?.code || null, text: latest?.text || null };
}

function normalizeOrder(row: any, providerOrder?: ProviderOrder) {
  const sms = providerOrder ? latestSms(providerOrder) : { code: row.sms_code ?? null, text: row.sms_text ?? null };
  return {
    activation_id: String(row.activation_id ?? row.id),
    phone: String(providerOrder?.phone ?? row.phone),
    status: String(providerOrder?.status ?? row.status ?? "PENDING"),
    sms_code: sms.code,
    sms_text: sms.text,
    expires_at: providerOrder?.expires ?? row.expires_at ?? null,
    charged_price: Number(row.charged_price ?? row.final_price ?? 0),
    base_price: Number(row.base_price ?? 0),
    product_code: String(row.product_code ?? row.service_code ?? providerOrder?.product ?? ""),
    country_code: String(row.country_code ?? providerOrder?.country ?? ""),
  };
}

async function getOwnedActivation(admin: SupabaseClient, userId: string, activationId: string) {
  return await rpc(admin, "virtual_numbers_get_my_activation", {
    p_user_id: userId,
    p_activation_id: activationId,
  });
}

async function reserveWallet(
  admin: SupabaseClient,
  userId: string,
  requestId: string,
  countryCode: string,
  serviceCode: string,
  operator: string,
  basePrice: number,
  finalPrice: number,
) {
  return await rpc(admin, "virtual_numbers_reserve_wallet", {
    p_user_id: userId,
    p_request_id: requestId,
    p_country_code: countryCode,
    p_service_code: serviceCode,
    p_operator: operator,
    p_base_price: basePrice,
    p_final_price: finalPrice,
  });
}

async function commitPurchase(
  admin: SupabaseClient,
  userId: string,
  requestId: string,
  providerOrder: ProviderOrder,
  countryCode: string,
  quotedBasePrice: number,
  config: PricingConfig,
  reservedMarginPercent: number,
) {
  // Use 5SIM's returned provider price when available, then reconcile the final wallet
  // amount against the reservation inside the commit RPC. Preserve the margin captured
  // at reservation time so a later admin price change cannot alter an in-flight order.
  const providerPrice = Number(providerOrder.price);
  const actualBasePrice = Number.isFinite(providerPrice) && providerPrice > 0
    ? roundMoney(providerPrice * config.provider_to_wallet_rate)
    : quotedBasePrice;
  const reservedPricing: PricingConfig = {
    ...config,
    margin_mode: "percent",
    margin_percent: reservedMarginPercent,
    margin_fixed: 0,
  };
  const actualFinalPrice = calculatePrice(actualBasePrice, reservedPricing);
  const result = await rpc(admin, "virtual_numbers_commit_purchase", {
    p_user_id: userId,
    p_request_id: requestId,
    p_provider_activation_id: providerOrder.id,
    p_phone: providerOrder.phone,
    p_status: providerOrder.status,
    p_expires_at: providerOrder.expires ?? null,
    p_base_price: actualBasePrice,
    p_final_price: actualFinalPrice,
    p_country_code: countryCode,
    p_service_code: providerOrder.product,
    p_operator: providerOrder.operator ?? "any",
  });
  if (result?.manual_review === true) {
    throw new HttpError("5SIM created the activation, but the final price exceeds the reserved amount and the wallet cannot cover the difference. The order is held for support reconciliation; it will not be purchased twice.", 409);
  }
  return normalizeOrder(result, providerOrder);
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (request.method !== "POST") return json({ error: "POST required" }, 405);

  try {
    if (!SUPABASE_URL || !SUPABASE_ANON_KEY || !SUPABASE_SERVICE_ROLE_KEY) {
      throw new HttpError("Supabase Edge Function secrets are incomplete", 503);
    }
    const { user, admin, userClient } = await requireUser(request);
    // The existing fivesim-api deployment already reads the 5SIM token from the
    // server-side FIVESIM_API_KEY secret. Keep this compatibility; never fetch it
    // through a client-callable database RPC or ship it to Android.
    if (!runtimeFiveSimApiKey) throw new HttpError("FIVESIM_API_KEY Edge Function secret is missing", 503);
    const body = await request.json();
    const action = requiredString(body.action, "action");

    // Backwards-compatible actions from the already deployed fivesim-api v1.
    // Existing admin and catalogue clients continue to work after this function is updated.
    if (action === "countries") {
      return json({ data: await providerRequest("guest/countries", false) });
    }
    if (action === "prices") {
      const country = body.country == null ? "" : String(body.country).trim().toLowerCase();
      const product = body.product == null ? "" : String(body.product).trim().toLowerCase();
      if (country && !/^[a-z0-9]+$/.test(country)) throw new HttpError("Invalid country", 400);
      if (product && !/^[a-z0-9_-]+$/.test(product)) throw new HttpError("Invalid product", 400);
      const query = new URLSearchParams();
      if (country) query.set("country", country);
      if (product) query.set("product", product);
      const queryString = query.toString();
      const suffix = queryString ? `?${queryString}` : "";
      return json({ data: await providerRequest(`guest/prices${suffix}`, false) });
    }
    if (action === "products") {
      const country = requiredString(body.country, "country").toLowerCase();
      const operator = String(body.operator ?? "any").trim().toLowerCase();
      if (!/^[a-z0-9]+$/.test(country) || !/^[a-z0-9_-]+$/.test(operator)) {
        throw new HttpError("Invalid country or operator", 400);
      }
      return json({ data: await providerRequest(`guest/products/${encodeURIComponent(country)}/${encodeURIComponent(operator)}`, false) });
    }
    if (action === "profile") {
      const { data: isAdmin, error: adminError } = await userClient.rpc("is_admin");
      if (adminError) throw new HttpError(`Admin check failed: ${adminError.message}`, 500);
      if (isAdmin !== true) throw new HttpError("Admin access required", 403);
      const profile = await providerRequest("user/profile", true);
      return json({ data: {
        id: profile.id, balance: profile.balance, rating: profile.rating,
        default_country: profile.default_country, default_operator: profile.default_operator,
      } });
    }

    if (action === "catalog") {
      const countryMap = await providerRequest("guest/countries", false);
      const countries = Object.entries(countryMap as Record<string, any>).map(([countryCode, value]) => {
        const iso = Object.keys(value?.iso ?? {})[0] ?? "";
        const dialCode = Object.keys(value?.prefix ?? {})[0] ?? "";
        return {
          name: String(value?.text_en ?? countryCode),
          country_code: countryCode,
          iso,
          dial_code: dialCode,
        };
      }).sort((a, b) => a.name.localeCompare(b.name));

      // One-time activation products requested in the screen's shortcuts.
      // Additional products can be added here or returned from the app's product config RPC.
      const popularProducts = [
        { code: "whatsapp", name: "WhatsApp" },
        { code: "telegram", name: "Telegram" },
        { code: "facebook", name: "Facebook" },
        { code: "google", name: "Google" },
      ];
      const { data: configuredProducts, error: productError } = await admin.rpc("virtual_numbers_get_enabled_services");
      if (productError) throw new HttpError(`virtual_numbers_get_enabled_services failed: ${productError.message}`, 503);
      const configured = Array.isArray(configuredProducts)
        ? configuredProducts.map((item: any) => ({ code: String(item.code), name: String(item.name ?? item.code) }))
        : [];
      const productsByCode = new Map<string, { code: string; name: string }>();
      for (const item of [...popularProducts, ...configured]) productsByCode.set(item.code.toLowerCase(), item);
      const products = [...productsByCode.values()];
      return json({ countries, services: products });
    }

    if (action === "quote") {
      const countryCode = requiredString(body.country_code, "country_code");
      const serviceCode = requiredString(body.service_code, "service_code");
      const { quote } = await quoteFor(admin, countryCode, serviceCode);
      return json(quote);
    }

    if (action === "purchase") {
      const countryCode = requiredString(body.country_code, "country_code");
      const serviceCode = requiredString(body.service_code, "service_code");
      const requestId = requiredString(body.request_id, "request_id");
      const requestedOperator = requiredString(body.operator, "operator");
      if (!/^[0-9a-f-]{36}$/i.test(requestId)) throw new HttpError("request_id must be a UUID");

      const { quote, config } = await quoteFor(admin, countryCode, serviceCode);
      const available = quote.operators as Array<any>;
      const chosen = requestedOperator === "auto"
        ? [...available].sort((a, b) => Number(b.success_rate) - Number(a.success_rate))[0]
        : available.find((item) => item.name === requestedOperator);
      if (!chosen) throw new HttpError("No stock for that country, service and operator", 409);
      const finalPrice = calculatePrice(Number(chosen.base_price), config);

      const reservation = await reserveWallet(
        admin, user.id, requestId, countryCode, serviceCode, chosen.name,
        Number(chosen.base_price), finalPrice,
      );
      if (reservation?.activation) return json(reservation.activation);

      let providerOrder: ProviderOrder;
      if (reservation?.provider_order?.id) {
        // A previous attempt reached 5SIM and stored the provider order before its final DB commit.
        providerOrder = reservation.provider_order as ProviderOrder;
      } else {
        if (reservation?.should_call_provider !== true) {
          throw new HttpError(
            "This wallet request is already reserved but has no saved 5SIM order. It was not purchased again; reconcile the pending request before retrying.",
            503,
          );
        }
        try {
          providerOrder = await providerRequest(
            `user/buy/activation/${encodeURIComponent(countryCode)}/${encodeURIComponent(chosen.name)}/${encodeURIComponent(serviceCode)}`
          ) as ProviderOrder;
          if (!providerOrder?.id || !providerOrder?.phone) {
            // A malformed 2xx response is ambiguous: do not refund and do not retry
            // a provider purchase automatically, because 5SIM may have issued a number.
            throw new HttpError("5SIM returned an incomplete activation; reservation retained for reconciliation", 503);
          }
        } catch (error) {
          // Refund only on a definitive provider 4xx/no-number response. A network error
          // or provider 5xx has an unknown outcome: the number may already have been issued.
          if (error instanceof HttpError && error.status >= 400 && error.status < 500 && ![408, 425].includes(error.status)) {
            await rpc(admin, "virtual_numbers_refund_wallet", {
              p_user_id: user.id,
              p_request_id: requestId,
              p_reason: error.message.slice(0, 300),
            });
            throw error;
          }
          throw new HttpError(
            "5SIM purchase outcome is not confirmed. Wallet reservation retained to prevent a duplicate purchase; reconcile this request before retrying.",
            503,
          );
        }
        await rpc(admin, "virtual_numbers_attach_provider_activation", {
          p_user_id: user.id,
          p_request_id: requestId,
          p_provider_activation_id: providerOrder.id,
          p_provider_price: Number(providerOrder.price ?? 0),
          p_phone: providerOrder.phone,
          p_status: providerOrder.status,
          p_expires_at: providerOrder.expires ?? null,
        });
      }
      // If this DB commit fails after provider success, keep the reservation pending. Refunding
      // here would give away a number already purchased from 5SIM; the request UUID is retryable.
      try {
        const reservedMarginPercent = Number(
          reservation?.margin_percent ??
          reservation?.provider_order?.margin_percent ??
          config.margin_percent
        );
        const normalized = await commitPurchase(
          admin, user.id, requestId, providerOrder, countryCode,
          Number(chosen.base_price), config, reservedMarginPercent,
        );
        return json(normalized);
      } catch (_commitError) {
        throw new HttpError("5SIM issued the number but activation persistence is pending. Retry this request or contact support; the wallet reservation has been retained.", 503);
      }
    }

    if (action === "poll" || action === "request_next_sms") {
      const activationId = requiredString(body.activation_id, "activation_id");
      const activation = await getOwnedActivation(admin, user.id, activationId);
      if (!activation) throw new HttpError("Activation not found", 404);
      const providerOrder = await providerRequest(`user/check/${encodeURIComponent(String(activation.provider_activation_id))}`) as ProviderOrder;
      const sms = latestSms(providerOrder);
      const providerStatus = String(providerOrder.status ?? "").toUpperCase();
      if (["CANCELED", "CANCELLED", "TIMEOUT"].includes(providerStatus) && !(providerOrder.sms?.length)) {
        // 5SIM confirms that no SMS was received and the activation has been canceled/timed out.
        // Credit the retail reservation exactly once through the same wallet ledger.
        await rpc(admin, "virtual_numbers_refund_wallet", {
          p_user_id: user.id,
          p_request_id: activation.request_id,
          p_reason: `5SIM activation ${providerStatus.toLowerCase()} without SMS`,
        });
      }
      const saved = await rpc(admin, "virtual_numbers_sync_activation", {
        p_user_id: user.id,
        p_activation_id: activationId,
        p_status: providerOrder.status,
        p_sms_code: sms.code,
        p_sms_text: sms.text,
        p_expires_at: providerOrder.expires ?? null,
      });
      return json(normalizeOrder(saved, providerOrder));
    }

    if (action === "cancel") {
      const activationId = requiredString(body.activation_id, "activation_id");
      const activation = await getOwnedActivation(admin, user.id, activationId);
      if (!activation) throw new HttpError("Activation not found", 404);
      const providerOrder = await providerRequest(`user/cancel/${encodeURIComponent(String(activation.provider_activation_id))}`) as ProviderOrder;
      const status = String(providerOrder.status ?? "").toUpperCase();
      const hasSms = Array.isArray(providerOrder.sms) && providerOrder.sms.length > 0;
      if ((status === "CANCELED" || status === "CANCELLED") && !hasSms) {
        await rpc(admin, "virtual_numbers_refund_wallet", {
          p_user_id: user.id,
          p_request_id: activation.request_id,
          p_reason: "Activation canceled at 5SIM",
        });
      }
      const sms = latestSms(providerOrder);
      const saved = await rpc(admin, "virtual_numbers_sync_activation", {
        p_user_id: user.id,
        p_activation_id: activationId,
        p_status: providerOrder.status,
        p_sms_code: sms.code,
        p_sms_text: sms.text,
        p_expires_at: providerOrder.expires ?? null,
      });
      return json(normalizeOrder(saved, providerOrder));
    }

    if (action === "latest_activation") {
      const activation = await rpc(admin, "virtual_numbers_get_my_latest_activation", { p_user_id: user.id });
      return json({ activation: activation ? normalizeOrder(activation) : null });
    }

    if (action === "register_device_token") {
      const token = requiredString(body.fcm_token, "fcm_token");
      const platform = String(body.platform ?? "android");
      await rpc(admin, "virtual_numbers_register_fcm_token", {
        p_user_id: user.id,
        p_fcm_token: token,
        p_platform: platform,
      });
      return json({ ok: true });
    }

    throw new HttpError("Unsupported action", 400);
  } catch (error) {
    const status = error instanceof HttpError ? error.status : 500;
    return json({ error: error instanceof Error ? error.message : "Unexpected error" }, status);
  }
});
