package com.numberone.numberone.data.remote

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull

/*
 * Pure helpers that read scalar results from RPC responses whose exact shape is not fixed
 * (a bare value, a one-row array, or an object). Kept free of Android and Supabase types so they
 * can be unit-tested on the JVM.
 */

private fun pickNumber(el: JsonElement, preferredKeyParts: List<String>): Double? = when (el) {
    is JsonPrimitive -> el.doubleOrNull
    is JsonArray -> el.firstNotNullOfOrNull { pickNumber(it, preferredKeyParts) }
    is JsonObject -> el.entries
        .filter { entry -> preferredKeyParts.any { it in entry.key } }
        .firstNotNullOfOrNull { pickNumber(it.value, preferredKeyParts) }
        ?: el.values.firstNotNullOfOrNull { pickNumber(it, preferredKeyParts) }
    else -> null
}

/** Numeric result (new balance, credited amount). Objects prefer a key mentioning "balance". Throws if none. */
internal fun parseNumberResult(rpc: String, raw: String): Double {
    val parsed = runCatching { rpcJson.parseToJsonElement(raw) }.getOrNull()
    return parsed?.let { pickNumber(it, listOf("balance")) }
        ?: throw IllegalStateException("$rpc: unexpected response: ${raw.take(200)}")
}

/** Margin percent. Objects prefer a key mentioning "percent" or "margin". Null when no number is present. */
internal fun parseMarginOrNull(raw: String): Double? {
    val parsed = runCatching { rpcJson.parseToJsonElement(raw) }.getOrNull() ?: return null
    return pickNumber(parsed, listOf("percent", "margin"))
}

/** Boolean result. A successful call with no boolean body counts as true. */
internal fun parseBooleanResult(raw: String): Boolean {
    val parsed = runCatching { rpcJson.parseToJsonElement(raw) }.getOrNull()
    return (parsed as? JsonPrimitive)?.booleanOrNull ?: true
}
