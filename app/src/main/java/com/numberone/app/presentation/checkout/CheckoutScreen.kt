package com.numberone.app.presentation.checkout

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    loading: Boolean,
    error: String?,
    onConfirmWalletPurchase: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            text = "$${String.format("%.2f", service.priceUsd)} USDT",
            style = MaterialTheme.typography.titleLarge,
            color = TealAccent,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (language == AppLanguage.ARABIC)
                "سيتم خصم السعر من رصيد محفظتك USDT فقط. تأكد من تفاصيل الخدمة قبل تأكيد الطلب."
            else
                "The amount will be deducted from your USDT wallet balance only. Review the service before confirming.",
            style = MaterialTheme.typography.bodyMedium
        )
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = onConfirmWalletPurchase,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (loading) CircularProgressIndicator()
            else Text(
                if (language == AppLanguage.ARABIC) "تأكيد الشراء من المحفظة" else "Confirm wallet purchase",
                fontWeight = FontWeight.Bold
            )
        }
        OutlinedButton(
            onClick = onCancel,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (language == AppLanguage.ARABIC) "رجوع" else "Back")
        }
    }
}
