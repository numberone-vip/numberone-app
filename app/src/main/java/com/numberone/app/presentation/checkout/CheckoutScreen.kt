package com.numberone.app.presentation.checkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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

/**
 * Checkout remains read-only until the official payment configuration is enabled in Supabase.
 * This avoids collecting receipts or suggesting transfers while payment_settings.is_enabled=false.
 */
@Composable
fun CheckoutScreen(
    service: VirtualService,
    strings: Strings,
    language: AppLanguage,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val countryName = if (language == AppLanguage.ARABIC) service.countryNameAr else service.countryNameEn

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = strings.orderNow,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "${service.countryFlag} ${service.appName} - $countryName",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "$" + String.format("%.2f", service.priceUsd),
            style = MaterialTheme.typography.headlineMedium,
            color = TealAccent,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = strings.availableCount + ": ${service.availableCount}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = strings.paymentNotReady,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(strings.backToStore)
        }
    }
}
