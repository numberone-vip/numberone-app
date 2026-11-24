package com.numberone.numberone.data.remote

import com.numberone.numberone.domain.model.HexzCatalogResponse
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.serialization.json.buildJsonObject

class HexzCatalogDataSource {
    private val supabase = SupabaseClientProvider.client

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
