package com.numberone.numberone.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.numberone.numberone.core.components.dangerColor
import com.numberone.numberone.core.components.successColor
import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.presentation.util.OrderStage

/**
 * Four-step progress bar: payment pending, receipt received, under review, completed.
 * A rejected order stops at the review step, which is shown in the error colour.
 */
@Composable
fun OrderProgress(stage: OrderStage, modifier: Modifier = Modifier) {
    val labels = listOf(
        tr("بانتظار الدفع"),
        tr("تم استلام الإيصال"),
        tr("قيد المراجعة"),
        tr("مكتمل")
    )
    val current = stage.step
    val completed = stage == OrderStage.COMPLETED
    val rejected = stage == OrderStage.REJECTED
    Row(modifier = modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            val isDone = index < current || (completed && index == current)
            val isCurrent = index == current && !completed
            val accent = when {
                isDone -> successColor()
                isCurrent && rejected -> dangerColor()
                isCurrent -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier.size(22.dp).clip(CircleShape).background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    } else {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDone || isCurrent) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}
