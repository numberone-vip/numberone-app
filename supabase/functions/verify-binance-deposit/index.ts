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
  const signature = await crypto.subtle.sign(
    "HMAC",
    key,
    new TextEncoder().encode(query),
  );
  return Array.from(new Uint8Array(signature))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

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
    const txHash = String(body.transaction_hash ?? "").trim().toLowerCase();
    if (!/^[0-9a-f]{64}$/.test(txHash) || !/^[0-9a-f-]{36}$/i.test(requestId)) {
      return reply({ error: "Invalid request_id or transaction_hash" }, 400);
    }

    const { data: depositRequest, error: requestError } = await userClient
      .from("wallet_deposit_requests")
      .select("id,user_id,requested_amount,currency,network,status,transaction_hash,created_at")
      .eq("id", requestId)
      .eq("user_id", userData.user.id)
      .maybeSingle();

    if (requestError) throw requestError;
    if (!depositRequest) return reply({ error: "Deposit request not found" }, 404);
    if (depositRequest.status !== "submitted" ||
        String(depositRequest.transaction_hash ?? "").toLowerCase() !== txHash) {
      return reply({ error: "Request is not submitted with this transaction hash" }, 409);
    }
    if (depositRequest.currency !== "USDT" || depositRequest.network !== "BEP20") {
      return reply({ error: "Only USDT on BEP20 is supported" }, 400);
    }

    const apiKey = required("BINANCE_API_KEY");
    const apiSecret = required("BINANCE_API_SECRET");
    const depositAddress = required("BINANCE_DEPOSIT_ADDRESS").toLowerCase();
    const now = Date.now();
    const createdAt = new Date(depositRequest.created_at).getTime();
    const startTime = Math.max(0, createdAt - 10 * 60 * 1000);
    const params = new URLSearchParams({
      coin: "USDT",
      startTime: String(startTime),
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
      const detail = await binanceResponse.text();
      console.error("Binance API error", binanceResponse.status, detail.slice(0, 500));
      return reply({ error: "Binance verification service is temporarily unavailable" }, 502);
    }

    const deposits = await binanceResponse.json();
    if (!Array.isArray(deposits)) throw new Error("Unexpected Binance deposit-history response");
    const matches = deposits.filter((d: any) =>
      String(d.txId ?? "").toLowerCase() === txHash &&
      String(d.coin ?? "").toUpperCase() === "USDT"
    );
    if (matches.length !== 1) {
      return reply({ status: "not_verified", message: "No unique matching Binance deposit was found yet. Check the TXID and try again later." }, 200);
    }

    const deposit = matches[0];
    const amount = Number(deposit.amount);
    const network = String(deposit.network ?? "").toUpperCase();
    const address = String(deposit.address ?? "").toLowerCase();
    const depositStatus = Number(deposit.status);
    if (depositStatus !== 1 || network !== "BSC" || address !== depositAddress ||
        !Number.isFinite(amount) || amount <= 0 ||
        Math.round(amount * 1e8) !== Math.round(Number(depositRequest.requested_amount) * 1e8)) {
      return reply({
        status: "not_verified",
        message: "The deposit is pending or does not exactly match the required asset, BSC network, receiving address, and amount.",
      }, 200);
    }

    const serviceRoleKey = required("SUPABASE_SERVICE_ROLE_KEY");
    const adminClient = createClient(url, serviceRoleKey, {
      auth: { persistSession: false, autoRefreshToken: false },
    });
    const { data: credited, error: creditError } = await adminClient.rpc(
      "wallet_apply_verified_deposit",
      {
        p_request_id: requestId,
        p_transaction_hash: txHash,
        p_verified_amount: amount,
        p_binance_deposit_id: String(deposit.id),
      },
    );
    if (creditError) {
      console.error("Wallet credit RPC failed", creditError.message);
      throw new Error("Verified deposit could not be credited; support review is required");
    }
    return reply({ status: credited ? "credited" : "already_credited", amount, currency: "USDT" });
  } catch (error) {
    console.error("verify-binance-deposit error", error);
    return reply({ error: error instanceof Error ? error.message : "Unexpected server error" }, 500);
  }
});
