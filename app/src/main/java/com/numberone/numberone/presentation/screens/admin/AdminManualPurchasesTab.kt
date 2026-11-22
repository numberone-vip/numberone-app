package com.numberone.numberone.presentation.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.numberone.numberone.core.components.CopyRow
import com.numberone.numberone.core.components.NOCard
import com.numberone.numberone.core.components.SectionTitle
import com.numberone.numberone.core.components.formatAmount
import com.numberone.numberone.core.components.formatDateTime
import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.core.localization.trf
import com.numberone.numberone.domain.model.AdminPendingPurchase
import com.numberone.numberone.domain.model.AdminPhoneMatch
import com.numberone.numberone.presentation.viewmodel.AdminViewModel

private val phoneRegex = Regex("^\\+[1-9][0-9]{7,14}$")
private val dateInputRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")

/** Wallet-paid manual purchases waiting for the admin to hand over the number. Shown at the top of Orders. */
internal fun LazyListScope.manualPurchaseItems(vm: AdminViewModel) {
    if (vm.pendingPurchases.isEmpty()) return
    item { SectionTitle(tr("طلبات الشراء اليدوي"), trf("{0} طلب بانتظار الاعتماد", vm.pendingPurchases.size)) }
    items(vm.pendingPurchases, key = { "pending-" + it.id }) { p -> PendingPurchaseCard(vm, p) }
}

@Composable
private fun PendingPurchaseCard(vm: AdminViewModel, p: AdminPendingPurchase) {
    var open by remember(p.id) { mutableStateOf(false) }
    NOCard {
        Text(
            (p.serviceName.ifBlank { tr("خدمة") }) + if (p.country.isNotBlank()) " • ${p.country}" else "",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            vm.emailFor(p.userId, p.email) + " • " + formatAmount(p.price) + " USDT • " + formatDateTime(p.createdAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = { open = true },
            enabled = !vm.loading,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text(tr("اعتماد وتسليم الرقم"), fontWeight = FontWeight.Bold) }
    }
    if (open) {
        ManualApproveDialog(
            onDismiss = { open = false },
            onConfirm = { phone, expiry ->
                vm.approveManualPurchase(p.id, phone, expiry)
                open = false
            }
        )
    }
}

@Composable
private fun ManualApproveDialog(onDismiss: () -> Unit, onConfirm: (phone: String, expiry: String) -> Unit) {
    var phone by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    val phoneOk = phoneRegex.matches(phone.trim())
    val expiryOk = dateInputRegex.matches(expiry.trim())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("اعتماد الطلب"), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    tr("أدخل الرقم الذي اشتريته من Fanytel بصيغة دولية، مثل +963..."),
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    singleLine = true,
                    label = { Text(tr("رقم الهاتف")) },
                    isError = phone.isNotBlank() && !phoneOk,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    textStyle = ltrTextStyle(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = expiry,
                    onValueChange = { expiry = it },
                    singleLine = true,
                    label = { Text(tr("تاريخ الانتهاء (yyyy-MM-dd)")) },
                    isError = expiry.isNotBlank() && !expiryOk,
                    textStyle = ltrTextStyle(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(phone.trim(), expiry.trim()) },
                enabled = phoneOk && expiryOk
            ) { Text(tr("تأكيد"), fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("إلغاء")) } }
    )
}

/** Search customers by the phone number they were assigned. Shown at the top of Users. */
internal fun LazyListScope.phoneSearchItems(vm: AdminViewModel) {
    item { PhoneSearchCard(vm) }
}

@Composable
private fun PhoneSearchCard(vm: AdminViewModel) {
    var query by remember { mutableStateOf("") }
    NOCard {
        Text(tr("بحث برقم الهاتف"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            tr("اكتب الرقم الذي اشتراه العميل من عندنا لمعرفة صاحبه"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("+963…") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                textStyle = ltrTextStyle(),
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { vm.searchByPhone(query.trim()) },
                enabled = query.count { it.isDigit() } >= 6 && !vm.loading,
                shape = RoundedCornerShape(14.dp)
            ) { Text(tr("بحث"), fontWeight = FontWeight.Bold) }
        }
        vm.phoneSearchError?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        vm.phoneMatches?.let { matches ->
            if (matches.isEmpty()) {
                Text(tr("لا يوجد عميل بهذا الرقم"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            matches.forEach { m -> PhoneMatchRow(m) }
        }
    }
}

@Composable
private fun PhoneMatchRow(m: AdminPhoneMatch) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(m.email ?: "—", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            listOf(m.serviceName, m.status.orEmpty(), m.expiryDate.orEmpty()).filter { it.isNotBlank() }.joinToString(" • "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        CopyRow(label = tr("الرقم"), value = m.phoneNumber)
    }
}
