import { createClient } from "npm:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

function reply(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

function required(name: string): string {
  const value = Deno.env.get(name);
  if (!value) throw new Error(`Missing server secret: ${name}`);
  return value;
}

async function sign(query: string, secret: string): Promise<string> {
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(query));
  return Array.from(new Uint8Array(signature)).map((b) => b.toString(16).padStart(2, "0")).join("");
}

const toMinor = (n: number) => Math.round(n * 1e8);

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return reply({ error: "Method not allowed" }, 405);

  try {
    const authHeader = req.headers.get("Authorization");
    if (!authHeader?.startsWith("Bearer ")) return reply({ error: "Authentication required" }, 401);
    const accessToken = authHeader.slice(7);

    const url = required("SUPABASE_URL");
    const publishableMap = JSON.parse(required("SUPABASE_PUBLISHABLE_KEYS"));
    const publishableKey = publishableMap.default;
    if (!publishableKey) throw new Error("Default Supabase publishable key is missing");

    const userClient = createClient(url, publishableKey, {
      global: { headers: { Authorization: authHeader } },
      auth: { persistSession: false, autoRefreshToken: false },
    });
    const { data: userData, error: userError } = await userClient.auth.getUser(accessToken);
    if (userError || !userData.user) return reply({ error: "Invalid user session" }, 401);

    const body = await req.json();
    const requestId = String(body.request_id ?? "");
    if (!/^[0-9a-f-]{36}$/i.test(requestId)) {
      return reply({ error: "Invalid request_id" }, 400);
    }

    // Optional legacy path: if the app still sends a TXID, it is checked the old way.
    const legacyHash = String(body.transaction_hash ?? "").trim().toLowerCase();
    if (legacyHash && !/^[0-9a-f]{64}$/.test(legacyHash)) {
      return reply({ error: "Invalid transaction_hash" }, 400);
    }

    const { data: depositRequest, error: requestError } = await userClient
      .from("wallet_deposit_requests")
      .select("id,user_id,requested_amount,currency,network,status,transaction_hash,created_at")
      .eq("id", requestId)
      .eq("user_id", userData.user.id)
      .maybeSingle();
    if (requestError) throw requestError;
    if (!depositRequest) return reply({ error: "Deposit request not found" }, 404);

    if (depositRequest.status === "credited") {
      return reply({ status: "already_credited" }, 200);
    }
    if (depositRequest.currency !== "USDT" || depositRequest.network !== "BEP20") {
      return reply({ error: "Only USDT on BEP20 is supported" }, 400);
    }
    if (depositRequest.status !== "pending" && depositRequest.status !== "submitted") {
      return reply({ error: "Deposit request is not open" }, 409);
    }

    const createdAt = new Date(depositRequest.created_at).getTime();
    if (Date.now() - createdAt > 24 * 60 * 60 * 1000) {
      return reply({ error: "Deposit request expired" }, 409);
    }

    const apiKey = required("BINANCE_API_KEY");
    const apiSecret = required("BINANCE_API_SECRET");
    const depositAddress = required("BINANCE_DEPOSIT_ADDRESS").toLowerCase();

    const now = Date.now();
    const params = new URLSearchParams({
      coin: "USDT",
      startTime: String(Math.max(0, createdAt - 10 * 60 * 1000)),
      endTime: String(now + 60_000),
      limit: "1000",
      recvWindow: "10000",
      timestamp: String(now),
    });
    const query = params.toString();
    const signature = await sign(query, apiSecret);
    const binanceResponse = await fetch(
      `https://api.binance.com/sapi/v1/capital/deposit/hisrec?${query}&signature=${signature}`,
      { headers: { "X-MBX-APIKEY": apiKey } },
    );
    if (!binanceResponse.ok) {
      console.error("Binance API error", binanceResponse.status, (await binanceResponse.text()).slice(0, 500));
      return reply({ error: "Binance verification service is temporarily unavailable" }, 502);
    }
    const deposits = await binanceResponse.json();
    if (!Array.isArray(deposits)) throw new Error("Unexpected Binance deposit-history response");

    const requiredMinor = toMinor(Number(depositRequest.requested_amount));
    const candidates = deposits.filter((d: any) =>
      String(d.coin ?? "").toUpperCase() === "USDT" &&
      Number(d.status) === 1 &&
      String(d.network ?? "").toUpperCase() === "BSC" &&
      String(d.address ?? "").toLowerCase() === depositAddress &&
      toMinor(Number(d.amount)) === requiredMinor &&
      (!legacyHash || String(d.txId ?? "").toLowerCase() === legacyHash)
    );

    if (candidates.length !== 1) {
      return reply({
        status: "not_verified",
        message: candidates.length === 0
          ? `لم نجد إيداعاً مطابقاً. تأكد من إرسال المبلغ بالضبط: ${depositRequest.requested_amount} USDT عبر شبكة BEP20.`
          : "تم العثور على أكثر من إيداع مطابق. يرجى التواصل مع الدعم.",
      }, 200);
    }

    const deposit = candidates[0];
    const txHash = String(deposit.txId ?? "").toLowerCase();
    if (!/^[0-9a-f]{64}$/.test(txHash)) throw new Error("Binance returned an invalid txId");

    // Prefer the non-deprecated secret key; fall back to the legacy service role key.
    const secretMap = Deno.env.get("SUPABASE_SECRET_KEYS")
      ? JSON.parse(required("SUPABASE_SECRET_KEYS"))
      : null;
    const serviceRoleKey = secretMap?.default ?? required("SUPABASE_SERVICE_ROLE_KEY");
    const adminClient = createClient(url, serviceRoleKey, {
      auth: { persistSession: false, autoRefreshToken: false },
    });

    if (depositRequest.status === "pending") {
      const { error: attachError } = await adminClient.rpc("wallet_attach_matched_deposit", {
        p_request_id: requestId,
        p_transaction_hash: txHash,
      });
      if (attachError) {
        console.error("Attach failed", attachError.message);
        return reply({ status: "not_verified", message: "هذا الإيداع مرتبط بطلب آخر بالفعل. يرجى التواصل مع الدعم." }, 200);
      }
    }

    const { data: credited, error: creditError } = await adminClient.rpc("wallet_apply_verified_deposit", {
      p_request_id: requestId,
      p_transaction_hash: txHash,
      p_verified_amount: Number(depositRequest.requested_amount),
      p_binance_deposit_id: String(deposit.id),
    });
    if (creditError) {
      console.error("Wallet credit RPC failed", creditError.message);
      // A concurrent or repeated check may have credited the request already: report success, not an error.
      const { data: current } = await adminClient
        .from("wallet_deposit_requests")
        .select("status")
        .eq("id", requestId)
        .maybeSingle();
      if (current?.status === "credited") {
        return reply({ status: "already_credited", amount: Number(depositRequest.requested_amount), currency: "USDT" });
      }
      // The submitted transaction hash does not match the verified deposit: the customer must fix it.
      if (current && String(current.status) === "submitted") {
        return reply({
          status: "not_verified",
          message: "رقم المعاملة المُرسَل لا يطابق الإيداع المؤكد. تحقق من الرقم وأعد الإرسال، أو تواصل مع الدعم مع رقم الطلب.",
        }, 200);
      }
      return reply({
        status: "not_verified",
        message: "تعذّر إضافة الإيداع الآن. تواصل مع الدعم مع رقم الطلب، ولن يُضاف الرصيد مرتين.",
      }, 200);
    }

    return reply({
      status: credited ? "credited" : "already_credited",
      amount: Number(depositRequest.requested_amount),
      currency: "USDT",
    });
  } catch (error) {
    console.error("verify-binance-deposit error", error);
    return reply({ error: error instanceof Error ? error.message : "Unexpected server error" }, 500);
  }
});
