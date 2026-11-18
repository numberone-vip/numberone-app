package com.numberone.app.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.numberone.app.core.localization.Strings
import com.numberone.app.core.theme.StatusApproved
import com.numberone.app.core.theme.StatusApprovedBg
import com.numberone.app.core.theme.StatusPending
import com.numberone.app.core.theme.StatusPendingBg
import com.numberone.app.core.theme.StatusRejected
import com.numberone.app.core.theme.StatusRejectedBg
import com.numberone.app.domain.model.OrderStatus

@Composable
fun StatusBadge(
    status: OrderStatus,
    strings: Strings,
    modifier: Modifier = Modifier
) {
    val (label, textColor, bgColor) = when (status) {
        OrderStatus.PENDING -> Triple(strings.statusPending, StatusPending, StatusPendingBg)
        OrderStatus.APPROVED -> Triple(strings.statusApproved, StatusApproved, StatusApprovedBg)
        OrderStatus.SMS_READY -> Triple(strings.statusSmsReady, StatusApproved, StatusApprovedBg)
        OrderStatus.REJECTED -> Triple(strings.statusRejected, StatusRejected, StatusRejectedBg)
    }

    Text(
        text = label,
        color = textColor,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}
