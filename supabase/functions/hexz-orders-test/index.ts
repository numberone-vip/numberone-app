import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, x-client-info, content-type, idempotency-key",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};
const BASE = "https://distribution.hexzrecargas.com";
const encoder = new TextEncoder();

function hex(bytes: Uint8Array): string {
  return Array.from(bytes, b => b.toString(16).padStart(2, "0")).join("");
}
async function sha256(value: string): Promise<string> {
  return hex(new Uint8Array(await crypto.subtle.digest("SHA-256", encoder.encode(value))));
}
async function hmac(secret: string, value: string): Promise<string> {
  const key = await crypto.subtle.importKey("raw", encoder.encode(secret),
    { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
  return hex(new Uint8Array(await crypto.subtle.sign("HMAC", key, encoder.encode(value))));
}
function json(data: unknown, status = 200): Response {
  return Response.json(data, { status, headers: { ...corsHeaders, "Cache-Control": "no-store" } });
}
async function signedRequest(path: string, method: "GET" | "POST", keyId: string,
  secret: string, bodyText = "", idempotencyKey?: string): Promise<Response> {
  const timestamp = String(Math.floor(Date.now() / 1000));
  const nonce = crypto.randomUUID().replace(/-/g, "");
  const bodyHash = await sha256(bodyText);
  const signature = await hmac(secret, [timestamp, nonce, method, path, bodyHash].join("\n"));
  const headers: Record<string, string> = {
    "Accept": "application/json",
    "Authorization": `Bearer ${keyId}_${secret}`,
    "X-Hexz-Key": keyId,
    "X-Hexz-Timestamp": timestamp,
    "X-Hexz-Nonce": nonce,
    "X-Hexz-Signature": signature,
  };
  if (method === "POST") headers["Content-Type"] = "application/json";
  if (idempotencyKey) headers["Idempotency-Key"] = idempotencyKey;
  return await fetch(BASE + path, {
    method, headers, ...(method === "POST" ? { body: bodyText } : {}),
    signal: AbortSignal.timeout(15000),
  });
}
async function readPayload(response: Response): Promise<unknown> {
  const raw = await response.text();
  try { return raw ? JSON.parse(raw) : null; }
  catch { return { response_text: raw.slice(0, 1000) }; }
}

type TargetField = {
  key: string; type?: string; format?: string; min_length?: number | null;
  max_length?: number | null; required?: boolean;
  options?: Array<{ value: string; label?: string }>;
};

function validateTarget(schema: TargetField[], target: Record<string, string>): string | null {
  const fields = new Map(schema.map(field => [field.key, field]));
  for (const key of Object.keys(target)) {
    if (!fields.has(key)) return `Unknown target field: ${key}`;
  }
  for (const field of schema) {
    const value = target[field.key];
    if (value === undefined || value === "") {
      if (field.required !== false) return `Missing required target field: ${field.key}`;
      continue;
    }
    if (typeof value !== "string") return `Target field must be text: ${field.key}`;
    const min = typeof field.min_length === "number" ? field.min_length : 0;
    const max = typeof field.max_length === "number" ? field.max_length : 200;
    if (value.length < min || value.length > max) {
      return `Target field length is invalid: ${field.key}`;
    }
    if (field.format === "digits" && !/^\d+$/.test(value)) {
      return `Target field must contain digits only: ${field.key}`;
    }
    if (field.type === "select") {
      if (!Array.isArray(field.options) || field.options.length === 0) {
        return `Target options are unavailable for field: ${field.key}`;
      }
      if (!field.options.some(option => option && option.value === value)) {
        return `Invalid selection for target field: ${field.key}`;
      }
    } else if (field.type && field.type !== "text") {
      return `Unsupported target field type: ${field.key}`;
    }
  }
  return null;
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ ok: false, error: "Method not allowed" }, 405);

  const auth = req.headers.get("Authorization") ?? "";
  const bearer = auth.replace(/^Bearer\s+/i, "");
  if (!bearer) return json({ ok: false, error: "Authentication required" }, 401);

  const fullKey = Deno.env.get("HEXZ_SANDBOX4")?.trim() ?? "";
  if (!fullKey.startsWith("hxd_live_")) {
    return json({ ok: false, stage: "configuration", error: "Live function: a valid hxd_live_ key is required" }, 503);
  }
  const separator = fullKey.lastIndexOf("_");
  if (separator <= fullKey.indexOf("_", 4) || separator === fullKey.length - 1) {
    return json({ ok: false, stage: "configuration", error: "Live API key format is invalid" }, 503);
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
  if (!supabaseUrl || !anonKey || !serviceRoleKey) {
    return json({ ok: false, error: "Authentication or catalog configuration unavailable" }, 503);
  }

  const authCheck = await fetch(`${supabaseUrl}/auth/v1/user`, {
    headers: { "Authorization": auth, "apikey": anonKey },
    signal: AbortSignal.timeout(8000),
  });
  if (!authCheck.ok) return json({ ok: false, error: "Valid user session required" }, 401);

  const keyId = fullKey.slice(0, separator);
  const secret = fullKey.slice(separator + 1);
  let input: Record<string, unknown>;
  try {
    const parsed = await req.json();
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) throw new Error("invalid");
    input = parsed as Record<string, unknown>;
  } catch {
    return json({ ok: false, error: "A valid JSON body is required" }, 400);
  }

  const action = input.action;
  if (action === "balance") {
    try {
      const response = await signedRequest("/api/v1/balance", "GET", keyId, secret);
      const result = await readPayload(response);
      return json({ ok: response.ok, environment: "live", stage: "balance", http_status: response.status, result },
        response.ok ? 200 : 502);
    } catch {
      return json({ ok: false, stage: "network", error: "Hexz live balance request failed" }, 502);
    }
  }

  if (action !== "create_order") {
    return json({ ok: false, error: "Supported actions: balance, create_order" }, 400);
  }

  // Fail closed until a server-side atomic wallet debit and durable order ledger
  // are integrated. Balance reads remain available; no billable provider order is sent.
  return json({
    ok: false,
    stage: "wallet_integration_required",
    error: "Hexz order creation is temporarily unavailable until wallet payment and transaction recording are enabled.",
  }, 503);
});