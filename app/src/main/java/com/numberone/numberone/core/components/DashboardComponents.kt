package com.numberone.numberone.core.components

import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.core.localization.trf
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.numberone.numberone.core.util.parseInstantOrNull
import kotlinx.coroutines.delay

/** Live "expires in ..." pill. Ticks every second in the last hour, every 30 s otherwise. */
@Composable
fun ExpiryCountdown(expiresAt: String?, modifier: Modifier = Modifier) {
    val target = remember(expiresAt) { parseInstantOrNull(expiresAt)?.toEpochMilli() } ?: return
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(target) {
        while (true) {
            now = System.currentTimeMillis()
            val left = target - now
            if (left <= 0) break
            delay(if (left < 3_600_000L) 1_000L else 30_000L)
        }
    }
    val left = target - now
    val color = when {
        left <= 0 -> dangerColor()
        left < 10 * 60_000L -> dangerColor()
        left < 24 * 3_600_000L -> warningColor()
        else -> successColor()
    }
    val text = when {
        left <= 0 -> tr("انتهت الصلاحية")
        left >= 24 * 3_600_000L -> {
            val days = left / (24 * 3_600_000L)
            val hours = (left % (24 * 3_600_000L)) / 3_600_000L
            trf("ينتهي خلال {0} يوم {1} س", ltr("$days"), ltr("$hours"))
        }
        left >= 3_600_000L -> {
            val hours = left / 3_600_000L
            val minutes = (left % 3_600_000L) / 60_000L
            trf("ينتهي خلال {0} س {1} د", ltr("$hours"), ltr("$minutes"))
        }
        else -> {
            val minutes = left / 60_000L
            val seconds = (left % 60_000L) / 1_000L
            tr("ينتهي خلال ") + ltr("%02d:%02d".format(minutes, seconds))
        }
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(50), color = color.copy(alpha = 0.14f), contentColor = color) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Full-width navigation row: icon, title, subtitle and a chevron that mirrors in RTL. */
@Composable
fun ActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false
) {
    val accent = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, if (highlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconBadge(icon, tint = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary, size = 42.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = accent)
        }
    }
}
