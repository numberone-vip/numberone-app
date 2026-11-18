@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.numberone.app.presentation.checkout

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.numberone.app.core.localization.AppLanguage
import com.numberone.app.core.localization.Strings
import com.numberone.app.core.theme.TealAccent
import com.numberone.app.domain.model.VirtualService

@Composable
fun CheckoutScreen(
    service: VirtualService,
    strings: Strings,
    language: AppLanguage,
    onSubmitOrder: (transactionRef: String, receiptUri: Uri?) -> Unit,
    modifier: Modifier = Modifier
) {
    var transactionRef by remember { mutableStateOf("") }
    var selectedReceiptUri by remember { mutableStateOf<Uri?>(null) }

    val receiptPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedReceiptUri = uri }

    val countryName = if (language == AppLanguage.ARABIC) service.countryNameAr else service.countryNameEn

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "${service.countryFlag} ${service.appName} - $countryName",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "$${String.format("%.2f", service.priceUsd)}",
            style = MaterialTheme.typography.titleLarge,
            color = TealAccent,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = if (language == AppLanguage.ARABIC)
                "الدفع غير متاح مؤقتاً. لن يتم إرسال طلب أو طلب تحويل حتى يتم تفعيل عنوان الدفع الرسمي."
            else
                "Payments are temporarily unavailable. No transfer should be made until the official payment address is enabled.",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = transactionRef,
            onValueChange = { transactionRef = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (language == AppLanguage.ARABIC) "رقم المرجع / الحوالة (اختياري)" else "Transaction Reference (Optional)") },
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        OutlinedButton(
            onClick = { receiptPicker.launch("image/*") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = if (selectedReceiptUri != null) "✓ ${strings.uploadReceipt}" else strings.uploadReceipt
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { onSubmitOrder(transactionRef, selectedReceiptUri) },
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = strings.submitOrder,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
