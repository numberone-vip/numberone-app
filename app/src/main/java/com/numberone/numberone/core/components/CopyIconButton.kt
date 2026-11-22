package com.numberone.numberone.core.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.numberone.numberone.core.localization.tr

/**
 * Compact one-tap copy button for list rows (phone numbers, order IDs, OTP codes).
 * Copies [value] and shows the shared "تم النسخ" / "Copied" toast.
 * Uses the same clipboard and toast helpers as [CopyField], so behavior matches across screens.
 */
@Composable
fun CopyIconButton(
    value: String,
    contentDescription: String = tr("نسخ"),
    tint: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    IconButton(
        onClick = {
            clipboard.setText(AnnotatedString(value))
            copiedToast(context)
        },
        enabled = value.isNotBlank(),
        modifier = modifier.size(40.dp)
    ) {
        Icon(Icons.Filled.ContentCopy, contentDescription = contentDescription, tint = tint)
    }
}
