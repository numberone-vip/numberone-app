// Scheduled Supabase Edge Function. Invoke every minute with Authorization: Bearer <CRON_SECRET>.
// It polls the user's active 5SIM orders and sends an FCM data message only for a new SMS code.
import { createClient } from "npm:@supabase/supabase-js@2";
import { importPKCS8, SignJWT } from "npm:jose@5.9.6";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";
const SERVICE_ROLE = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
let runtimeFiveSimApiKey = Deno.env.get("FIVESIM_API_KEY") ?? "";
const CRON_SECRET = Deno.env.get("FIVE_SIM_PUSH_CRON_SECRET") ?? "";
const SERVICE_ACCOUNT_JSON = Deno.env.get("FCM_SERVICE_ACCOUNT_JSON") ?? "";
const BASE = "https://5sim.net/v1";

async function getFcmAccessToken(account: any): Promise<string> {
  const privateKey = await importPKCS8(String(account.private_key).replace(/\\n/g, "\n"), "RS256");
  const assertion = await new SignJWT({ scope: "https://www.googleapis.com/auth/firebase.messaging" })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" })
    .setIssuer(account.client_email)
    .setSubject(account.client_email)
    .setAudience("https://oauth2.googleapis.com/token")
    .setIssuedAt()
    .setExpirationTime("1h")
    .sign(privateKey);
  const tokenResponse = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  const payload = await tokenResponse.json();
  if (!tokenResponse.ok || !payload.access_token) throw new Error(`FCM OAuth error: ${JSON.stringify(payload)}`);
  return payload.access_token as string;
}

Deno.serve(async (request) => {
  if (request.method !== "POST") return new Response("POST required", { status: 405 });
  if (!CRON_SECRET || request.headers.get("Authorization") !== `Bearer ${CRON_SECRET}`) {
    return new Response("Unauthorized", { status: 401 });
  }
  if (!SUPABASE_URL || !SERVICE_ROLE || !SERVICE_ACCOUNT_JSON) {
    return Response.json({ error: "Missing Supabase or FCM secrets" }, { status: 503 });
  }

  try {
    const admin = createClient(SUPABASE_URL, SERVICE_ROLE, { auth: { persistSession: false } });
    if (!runtimeFiveSimApiKey) throw new Error("FIVESIM_API_KEY Edge Function secret is missing");
    const account = JSON.parse(SERVICE_ACCOUNT_JSON);
    const accessToken = await getFcmAccessToken(account);
    const { data: active, error } = await admin.rpc("virtual_numbers_get_active_for_push");
    if (error) throw error;

    let notificationsSent = 0;
    for (const row of (active ?? [])) {
      const providerResponse = await fetch(`${BASE}/user/check/${encodeURIComponent(String(row.provider_activation_id))}`, {
        headers: { Authorization: `Bearer ${runtimeFiveSimApiKey}`, Accept: "application/json" },
      });
      if (!providerResponse.ok) continue;
      const provider = await providerResponse.json();
      const sms = Array.isArray(provider.sms) && provider.sms.length ? provider.sms[provider.sms.length - 1] : null;
      const code = sms?.code ? String(sms.code) : null;
      const providerStatus = String(provider.status ?? "").toUpperCase();
      if (["CANCELED", "CANCELLED", "TIMEOUT"].includes(providerStatus) && !(provider.sms?.length)) {
        const { error: refundError } = await admin.rpc("virtual_numbers_refund_wallet", {
          p_user_id: row.user_id,
          p_request_id: row.request_id,
          p_reason: `5SIM activation ${providerStatus.toLowerCase()} without SMS`,
        });
        // Leave activation state unchanged so the next scheduler pass can retry a failed credit.
        if (refundError) continue;
      }

      const { data: updated, error: updateError } = await admin.rpc("virtual_numbers_sync_activation", {
        p_user_id: row.user_id,
        p_activation_id: row.activation_id,
        p_status: provider.status,
        p_sms_code: code,
        p_sms_text: sms?.text ?? null,
        p_expires_at: provider.expires ?? null,
      });
      if (updateError) continue;
      // sync_activation atomically claims a new code. It also persists terminal statuses
      // and lets timeout/cancellation refunds settle even when the user has closed the app.
      if (updated?.notification_claimed !== true) continue;

      const { data: tokens, error: tokenError } = await admin.rpc("virtual_numbers_get_user_fcm_tokens", {
        p_user_id: row.user_id,
      });
      if (tokenError) continue;
      const tokenRows = Array.isArray(tokens) ? tokens : [];
      for (const tokenRow of tokenRows) {
        const token = typeof tokenRow === "string" ? tokenRow : tokenRow.fcm_token ?? tokenRow.token;
        if (!token) continue;
        const message = {
          message: {
            token,
            data: {
              title: "وصل رمز تفعيل جديد",
              body: `A new SMS arrived for ${row.phone}. Open Number One to view it.`,
              phone: String(row.phone),
              activation_id: String(row.activation_id),
            },
            android: { priority: "high" },
          },
        };
        const sent = await fetch(`https://fcm.googleapis.com/v1/projects/${account.project_id}/messages:send`, {
          method: "POST",
          headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
          body: JSON.stringify(message),
        });
        if (sent.ok) notificationsSent++;
      }
    }

    return Response.json({ ok: true, notifications_sent: notificationsSent });
  } catch (error) {
    return Response.json({ error: error instanceof Error ? error.message : "Push poll failed" }, { status: 500 });
  }
});
