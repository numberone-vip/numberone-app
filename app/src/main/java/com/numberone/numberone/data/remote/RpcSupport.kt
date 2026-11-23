package com.numberone.numberone.data.remote

import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Null-safe decoder for RPC payloads.
 * - ignoreUnknownKeys: new server columns never break the app.
 * - coerceInputValues: a JSON null for a non-null field falls back to its declared default.
 * - explicitNulls = false: absent nullable fields decode as null instead of failing.
 */
val rpcJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
}

/** Calls an RPC and returns the raw JSON body. Parameters are always a JsonObject (supabase-kt 3.x). */
internal suspend fun Postgrest.rpcRaw(function: String, params: JsonObject? = null): String =
    (if (params == null) rpc(function) else rpc(function, params)).data

internal suspend inline fun <reified T> Postgrest.rpcDecode(function: String, params: JsonObject? = null): T =
    rpcJson.decodeFromString<T>(rpcRaw(function, params))
