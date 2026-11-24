package com.numberone.numberone.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HexzCatalogResponse(
    val ok: Boolean = false,
    val stage: String? = null,
    val error: String? = null,
    val result: HexzCatalogResult? = null
)

@Serializable
data class HexzCatalogResult(
    val items: List<HexzCatalogItem> = emptyList()
)

@Serializable
data class HexzCatalogItem(
    @SerialName("product_id") val productId: String = "",
    val name: String = "",
    val category: String? = null,
    val type: String? = null,
    @SerialName("family_name") val familyName: String? = null,
    @SerialName("family_kind") val familyKind: String? = null,
    val region: String? = null,
    val price: kotlinx.serialization.json.JsonElement? = null,
    val currency: String? = null,
    @SerialName("required_data") val requiredData: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    val description: String? = null,
    @SerialName("in_stock") val inStock: Boolean? = null,
    @SerialName("requires_target") val requiresTarget: Boolean = false,
    @SerialName("target_schema") val targetSchema: List<HexzTargetField>? = null,
    @SerialName("family_id") val familyId: String? = null,
    val sku: String? = null,
    val section: String? = null
)

@Serializable
data class HexzTargetField(
    val key: String = "",
    val label: String = "",
    val type: String = "text",
    val format: String? = null,
    @SerialName("min_length") val minLength: Int? = null,
    @SerialName("max_length") val maxLength: Int? = null,
    val required: Boolean = true
)
