package com.numberone.numberone.data.remote

import com.numberone.numberone.domain.model.HexzCatalogResponse
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.put

class HexzCatalogDataSource {
    private val supabase = SupabaseClientProvider.client

    suspend fun checkSandboxBalance(): String {
        val response = supabase.functions.invoke(
            function = "hexz-orders-test",
            body = buildJsonObject { put("action", "balance") }
        )
        val payload = response.body<JsonObject>()
        check(payload["ok"]?.toString() == "true") { "تعذر الاتصال برصيد Hexz التجريبي: $payload" }
        return payload["result"]?.toString() ?: "تم الاتصال، لكن المزود لم يُرجع تفاصيل الرصيد."
    }

    suspend fun getCatalog(): List<com.numberone.numberone.domain.model.HexzCatalogItem> {
        val response = supabase.functions.invoke(
            function = "hexz-catalog",
            body = buildJsonObject { }
        )
        val payload = response.body<HexzCatalogResponse>()
        check(payload.ok) { payload.error ?: "تعذر تحميل كتالوج Hexz" }
        return payload.result?.items.orEmpty()
    }
}
