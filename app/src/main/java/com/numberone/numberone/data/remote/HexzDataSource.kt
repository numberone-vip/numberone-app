package com.numberone.numberone.data.remote

import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class HexzTargetOption(val value: String, val label: String)
data class HexzTargetField(
    val key: String,
    val label: String,
    val type: String,
    val required: Boolean,
    val format: String?,
    val minLength: Int?,
    val maxLength: Int?,
    val options: List<HexzTargetOption>
)
data class HexzProduct(
    val id: String,
    val name: String,
    val category: String?,
    val region: String?,
    val price: String?,
    val currency: String?,
    val description: String?,
    val logoUrl: String?,
    val inStock: Boolean?,
    val requiresTarget: Boolean,
    val targetFields: List<HexzTargetField>
)

/** Public catalog data only. Provider credentials and all purchase operations stay server-side. */
class HexzDataSource {
    private val supabase = SupabaseClientProvider.client

    suspend fun catalog(): List<HexzProduct> {
        val response = supabase.functions.invoke(
            function = "hexz-catalog",
            body = kotlinx.serialization.json.buildJsonObject { }
        )
        val root = response.body<JsonObject>()
        if (root["ok"]?.jsonPrimitive?.contentOrNull != "true") {
            val reason = root["error"]?.jsonPrimitive?.contentOrNull
            throw IllegalStateException(reason ?: "تعذر تحميل كتالوج HEXZ")
        }
        val items = root["result"]?.jsonObject?.get("items")?.jsonArray ?: JsonArray(emptyList())
        return items.mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val id = item.text("product_id") ?: item.text("sku") ?: return@mapNotNull null
            val name = item.text("name")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val fields = item["target_schema"]?.let { value ->
                runCatching { value.jsonArray.mapNotNull { fieldElement ->
                    val field = fieldElement as? JsonObject ?: return@mapNotNull null
                    val key = field.text("key")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                    val options = field["options"]?.let { raw ->
                        runCatching { raw.jsonArray.mapNotNull { optionElement ->
                            val option = optionElement as? JsonObject ?: return@mapNotNull null
                            val optionValue = option.text("value") ?: return@mapNotNull null
                            HexzTargetOption(optionValue, option.text("label") ?: optionValue)
                        } }.getOrDefault(emptyList())
                    }.orEmpty()
                    HexzTargetField(
                        key = key,
                        label = field.text("label") ?: key,
                        type = field.text("type") ?: "text",
                        required = field.text("required") != "false",
                        format = field.text("format"),
                        minLength = field.text("min_length")?.toIntOrNull(),
                        maxLength = field.text("max_length")?.toIntOrNull(),
                        options = options
                    )
                } }.getOrDefault(emptyList())
            }.orEmpty()
            HexzProduct(
                id = id,
                name = name,
                category = item.text("category"),
                region = item.text("region"),
                price = item.text("price"),
                currency = item.text("currency"),
                description = item.text("description"),
                logoUrl = item.text("logo_url"),
                inStock = item.text("in_stock")?.toBooleanStrictOrNull(),
                requiresTarget = item.text("requires_target") == "true" || fields.isNotEmpty(),
                targetFields = fields
            )
        }
    }

    private fun JsonObject.text(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull
}
