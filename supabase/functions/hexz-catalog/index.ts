import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, x-client-info, content-type",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
};
const HEXZ_BASE = "https://distribution.hexzrecargas.com";
const encoder = new TextEncoder();
const FRESH_MS = 10 * 60 * 1000;
const UPSTREAM_TIMEOUT_MS = 7000;

function toHex(bytes: Uint8Array): string {
  return Array.from(bytes, (b) => b.toString(16).padStart(2, "0")).join("");
}
async function sha256Hex(value: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", encoder.encode(value));
  return toHex(new Uint8Array(digest));
}
async function hmacHex(secret: string, value: string): Promise<string> {
  const key = await crypto.subtle.importKey("raw", encoder.encode(secret),
    { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
  const signature = await crypto.subtle.sign("HMAC", key, encoder.encode(value));
  return toHex(new Uint8Array(signature));
}
async function signedCatalog(keyId: string, secret: string): Promise<Response> {
  const path = "/api/v1/catalog";
  const timestamp = String(Math.floor(Date.now() / 1000));
  const nonce = crypto.randomUUID().replace(/-/g, "");
  const bodyHash = await sha256Hex("");
  const signingText = [timestamp, nonce, "GET", path, bodyHash].join("\n");
  const signature = await hmacHex(secret, signingText);
  return await fetch(HEXZ_BASE + path, {
    method: "GET",
    headers: {
      "Accept": "application/json",
      "X-Hexz-Key": keyId,
      "X-Hexz-Timestamp": timestamp,
      "X-Hexz-Nonce": nonce,
      "X-Hexz-Signature": signature,
    },
    signal: AbortSignal.timeout(UPSTREAM_TIMEOUT_MS),
  });
}
function compactCatalog(payload: unknown): { items: Record<string, unknown>[] } {
  const root = payload && typeof payload === "object" ? payload as Record<string, unknown> : {};
  const rawItems = Array.isArray(root.items) ? root.items : [];
  const items = rawItems.flatMap((raw): Record<string, unknown>[] => {
    if (!raw || typeof raw !== "object") return [];
    const item = raw as Record<string, unknown>;
    const name = item.name ?? item.product_name ?? item.title;
    if (typeof name !== "string" || !name.trim()) return [];

    const rawSchema = Array.isArray(item.target_schema) ? item.target_schema : [];
    const targetSchema = rawSchema.flatMap((field): Record<string, unknown>[] => {
      if (!field || typeof field !== "object") return [];
      const f = field as Record<string, unknown>;
      const key = typeof f.key === "string" ? f.key : "";
      if (!key) return [];
      return [{
        key,
        label: typeof f.label === "string" ? f.label : key,
        type: typeof f.type === "string" ? f.type : "text",
        format: typeof f.format === "string" ? f.format : null,
        min_length: typeof f.min_length === "number" ? f.min_length : null,
        max_length: typeof f.max_length === "number" ? f.max_length : null,
        required: f.required !== false,
        options: Array.isArray(f.options) ? f.options.flatMap((option): Record<string, string>[] => {
          if (!option || typeof option !== "object") return [];
          const entry = option as Record<string, unknown>;
          if (typeof entry.value !== "string") return [];
          return [{
            value: entry.value,
            label: typeof entry.label === "string" ? entry.label : entry.value,
          }];
        }) : [],
      }];
    });
    const fallbackFields = Array.isArray(item.target_fields)
      ? item.target_fields.filter((field): field is string => typeof field === "string") : [];
    const stock = typeof item.in_stock === "boolean"
      ? item.in_stock
      : typeof item.available_stock === "number"
        ? item.available_stock > 0
        : null;

    return [{
      product_id: item.product_id ?? item.sku ?? item.id ?? name,
      name,
      category: item.category ?? item.family_kind ?? item.type ?? item.section ?? item.family_name ?? null,
      family_id: item.family_id ?? null,
      family_name: item.family_name ?? null,
      family_kind: item.family_kind ?? null,
      section: item.section ?? null,
      type: item.type ?? item.product_type ?? null,
      sku: item.sku ?? null,
      region: item.region ?? item.country ?? null,
      price: item.price ?? item.sell_price ?? item.amount ?? null,
      currency: item.currency ?? null,
      required_data: (targetSchema.map((field) => String(field.label ?? field.key)).filter(Boolean).join(", ") ||
        fallbackFields.join(", ") ||
        (typeof item.required_data === "string" ? item.required_data : "")) || null,
      logo_url: item.logo_url ?? item.image_url ?? item.image ?? item.icon_url ?? null,
      description: typeof item.description === "string" ? item.description : null,
      in_stock: stock,
      requires_target: item.requires_target === true || targetSchema.length > 0 || fallbackFields.length > 0,
      target_schema: targetSchema,
    }];
  });
  return { items };
}
function json(data: unknown, status = 200): Response {
  return Response.json(data, {
    status,
    headers: { ...corsHeaders, "Cache-Control": "no-store" },
  });
}
async function refreshCatalog(admin: ReturnType<typeof createClient>, keyId: string, secret: string): Promise<void> {
  try {
    const started = Date.now();
    const response = await signedCatalog(keyId, secret);
    const raw = await response.text();
    if (!response.ok) {
      console.error("hexz_upstream_rejected", { status: response.status, elapsed_ms: Date.now() - started });
      return;
    }
    let catalog: unknown;
    try { catalog = JSON.parse(raw); }
    catch {
      console.error("hexz_invalid_json", { response_bytes: raw.length });
      return;
    }
    const compacted = compactCatalog(catalog);
    const { error } = await admin.from("hexz_catalog_cache").upsert({
      cache_key: "catalog",
      payload: compacted,
      fetched_at: new Date().toISOString(),
    }, { onConflict: "cache_key" });
    if (error) console.error("hexz_cache_write_failed", error);
    else console.info("hexz_catalog_refreshed", {
      elapsed_ms: Date.now() - started,
      response_bytes: raw.length,
      item_count: Array.isArray((catalog as Record<string, unknown>)?.items)
        ? ((catalog as Record<string, unknown>).items as unknown[]).length : 0,
    });
  } catch (error) {
    console.error("hexz_catalog_refresh_failed", {
      error: error instanceof Error ? error.message : "Unknown upstream error",
    });
  }
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "GET" && req.method !== "POST") return json({ ok: false, error: "Method not allowed" }, 405);

  const fullKey = Deno.env.get("HEXZ_SANDBOX4")?.trim();
  if (!fullKey) return json({ ok: false, stage: "configuration", error: "Missing provider API secret" }, 503);
  const separator = fullKey.lastIndexOf("_");
  if (separator < 1 || !(fullKey.startsWith("hxd_test_") || fullKey.startsWith("hxd_live_")) ||
      separator <= fullKey.indexOf("_", 4) || separator === fullKey.length - 1) {
    return json({ ok: false, stage: "configuration", error: "Provider API secret format is invalid" }, 503);
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !serviceRoleKey) {
    return json({ ok: false, stage: "cache_configuration", error: "Supabase service credentials are unavailable" }, 503);
  }
  const keyId = fullKey.slice(0, separator);
  const secret = fullKey.slice(separator + 1);
  const admin = createClient(supabaseUrl, serviceRoleKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });

  let cachedPayload: unknown = null;
  let cacheAgeMs = Number.POSITIVE_INFINITY;
  try {
    const { data, error } = await admin.from("hexz_catalog_cache")
      .select("payload,fetched_at").eq("cache_key", "catalog").maybeSingle();
    if (error) throw error;
    if (data) {
      cachedPayload = data.payload;
      cacheAgeMs = Math.max(0, Date.now() - new Date(data.fetched_at).getTime());
    }
  } catch (error) {
    console.error("hexz_cache_read_failed", error);
  }

  if (cachedPayload !== null) {
    if (cacheAgeMs >= FRESH_MS) {
      EdgeRuntime.waitUntil(refreshCatalog(admin, keyId, secret));
      return json({
        ok: true, stage: "stale_cache",
        result: compactCatalog(cachedPayload),
      });
    }
    return json({
      ok: true, stage: "cache",
      result: compactCatalog(cachedPayload),
    });
  }

  try {
    const response = await signedCatalog(keyId, secret);
    const raw = await response.text();
    if (!response.ok) {
      console.error("hexz_upstream_rejected", { status: response.status });
      return json({ ok: false, stage: "hexz_catalog", http_status: response.status, error: raw.slice(0, 500) }, 502);
    }
    let catalog: unknown;
    try { catalog = JSON.parse(raw); }
    catch { return json({ ok: false, stage: "hexz_catalog", error: "Provider returned invalid JSON" }, 502); }
    const compacted = compactCatalog(catalog);
    const { error } = await admin.from("hexz_catalog_cache").upsert({
      cache_key: "catalog", payload: compacted, fetched_at: new Date().toISOString(),
    }, { onConflict: "cache_key" });
    if (error) console.error("hexz_cache_write_failed", error);
    return json({ ok: true, stage: "hexz_catalog", result: compactCatalog(catalog) });
  } catch (error) {
    console.error("hexz_catalog_request_failed", {
      error: error instanceof Error ? error.message : "Unknown upstream error",
    });
    return json({ ok: false, stage: "network", error: "Catalog provider timed out or is unavailable" }, 502);
  }
});
