package com.numberone.numberone.presentation.screens.hexz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.numberone.numberone.core.components.EmptyState
import com.numberone.numberone.core.components.FeedbackBanner
import com.numberone.numberone.core.components.NOCard
import com.numberone.numberone.core.components.NOScaffold
import com.numberone.numberone.core.components.SectionTitle
import com.numberone.numberone.core.localization.tr
import coil3.compose.AsyncImage
import com.numberone.numberone.data.remote.HexzProduct
import com.numberone.numberone.di.AppContainer
import kotlinx.coroutines.launch

@Composable
fun HexzScreen(onBack: () -> Unit, onTopUp: () -> Unit) {
    var products by remember { mutableStateOf<List<HexzProduct>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun loadCatalog() {
        loading = true
        error = null
        try {
            products = AppContainer.hexzDataSource.catalog()
        } catch (e: Exception) {
            error = e.message ?: tr("تعذر تحميل منتجات HEXZ")
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { loadCatalog() }

    val filtered = remember(products, query) {
        val term = query.trim()
        if (term.isEmpty()) products
        else products.filter {
            it.name.contains(term, ignoreCase = true) ||
                it.category.orEmpty().contains(term, ignoreCase = true) ||
                it.region.orEmpty().contains(term, ignoreCase = true)
        }
    }

    NOScaffold(title = tr("HEXZ"), onBack = onBack) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            NOCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(tr("المنتجات الرقمية"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text(
                            tr("تصفح منتجات HEXZ واختر الخدمة المناسبة لك."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { scope.launch { loadCatalog() } }, enabled = !loading) {
                        Icon(Icons.Filled.Refresh, contentDescription = tr("تحديث المنتجات"))
                    }
                }
                Button(onClick = onTopUp, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("شحن المحفظة"))
                }
            }

            FeedbackBanner(loading = loading, error = error, message = null)

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(tr("ابحث عن منتج أو دولة أو تصنيف")) },
                shape = RoundedCornerShape(14.dp)
            )

            SectionTitle(
                tr("كتالوج HEXZ"),
                trfCount(filtered.size)
            )

            if (!loading && filtered.isEmpty()) {
                EmptyState(
                    if (products.isEmpty()) tr("لا توجد منتجات متاحة حالياً.")
                    else tr("لا توجد نتائج مطابقة للبحث.")
                )
            }

            filtered.forEach { product ->
                HexzProductCard(
                    product = product,
                    expanded = selectedId == product.id,
                    onToggle = { selectedId = if (selectedId == product.id) null else product.id }
                )
            }

            NOCard {
                Text(
                    tr("الشراء من HEXZ سيُفعّل بعد اكتمال ربطه بخصم المحفظة وتسجيل الطلب بشكل آمن. لن يتم إرسال طلب مدفوع قبل ذلك."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HexzProductCard(
    product: HexzProduct,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!product.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = product.logoUrl,
                        contentDescription = product.name,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Filled.ShoppingBag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f).padding(start = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    val subtitle = listOfNotNull(product.category, product.region).joinToString(" • ")
                    if (subtitle.isNotBlank()) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(product.price ?: tr("السعر غير متاح"), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    product.currency?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                }
            }
            product.description?.takeIf(String::isNotBlank)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            product.inStock?.let { inStock ->
                Text(
                    if (inStock) tr("متوفر") else tr("غير متوفر حالياً"),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (inStock) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            OutlinedButton(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
                Text(if (expanded) tr("إخفاء التفاصيل") else tr("تفاصيل المنتج"))
            }
            if (expanded) {
                if (product.targetFields.isNotEmpty()) {
                    Text(tr("البيانات المطلوبة عند الطلب"), fontWeight = FontWeight.Bold)
                    product.targetFields.forEach { field ->
                        Text(
                            "• ${field.label}" + if (field.required) " (${tr("مطلوب")})" else " (${tr("اختياري")})",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (field.options.isNotEmpty()) {
                            Text(
                                field.options.joinToString("، ") { it.label },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text(tr("لا يتطلب هذا المنتج بيانات إضافية."), style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("الشراء غير متاح مؤقتاً"))
                }
            }
        }
    }
}

private fun trfCount(count: Int): String =
    if (count == 1) tr("منتج واحد") else tr("عدد المنتجات: ") + count.toString()
