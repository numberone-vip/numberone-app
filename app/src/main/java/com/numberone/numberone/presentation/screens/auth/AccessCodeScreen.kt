package com.numberone.numberone.presentation.screens.auth

import com.numberone.numberone.core.localization.tr
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.numberone.numberone.core.theme.NumberOneColors
import com.numberone.numberone.presentation.viewmodel.AppViewModel

@Composable
fun AccessCodeScreen(vm: AppViewModel, onBack: () -> Unit, onDone: () -> Unit) {
    val palette = authPalette()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = tr("رجوع"),
                    tint = palette.onCard
                )
            }
            Text(
                text = tr("الدخول برمز الوصول"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = palette.onCard,
                modifier = Modifier.weight(1f)
            )
        }

        // Luxury card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = palette.card,
            contentColor = palette.onCard,
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 14.dp,
            border = BorderStroke(1.dp, palette.fieldBorder.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Gold-framed key crest
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(AuthHeaderBrush)
                        .border(BorderStroke(1.dp, AuthGoldBrush), RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Key,
                        contentDescription = null,
                        tint = NumberOneColors.Champagne,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = tr("رمز الوصول"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = palette.onCard
                )
                Text(
                    text = tr("أدخل الرمز الذي يزودك به فريق الخدمة لعرض اشتراكك ورسائلك."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.muted
                )

                AuthTextField(
                    palette = palette,
                    label = tr("رمز الوصول"),
                    hint = tr("رمز الوصول"),
                    value = vm.accessCode,
                    onValueChange = vm::setAccessCode,
                    icon = Icons.Filled.Key,
                    enabled = !vm.loading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { vm.access() })
                )

                vm.error?.let { AuthNotice(palette, it, isError = true) }
                vm.message?.let { AuthNotice(palette, it, isError = false) }

                AuthPrimaryButton(
                    palette = palette,
                    text = tr("التحقق من الرمز"),
                    onClick = { vm.access() },
                    modifier = Modifier.fillMaxWidth(),
                    loading = vm.loading
                )
            }
        }

        if (vm.subscriptions.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.accentSoft)
                    .border(1.dp, AuthGoldBrush, RoundedCornerShape(18.dp))
                    .clickable(role = Role.Button, onClick = onDone),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tr("عرض الاشتراك والرسائل"),
                    color = palette.accent,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
