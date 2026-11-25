package com.numberone.numberone.data.remote

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonObject

/** Raw RPC access used by the admin repository. Fakeable in tests. */
interface AdminRpc {
    suspend fun rpcRaw(function: String, params: JsonObject? = null): String
}

class SupabaseAdminRpc : AdminRpc {
    override suspend fun rpcRaw(function: String, params: JsonObject?): String =
        SupabaseClientProvider.client.postgrest.rpcRaw(function, params)
}
