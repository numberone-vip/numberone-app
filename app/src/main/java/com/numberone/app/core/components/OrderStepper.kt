package com.numberone.app.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.numberone.app.core.theme.StatusApproved
import com.numberone.app.core.theme.StatusRejected
import com.numberone.app.core.theme.TealAccent
import com.numberone.app.domain.model.OrderStatus

@Composable
fun OrderStepper(
    status: OrderStatus,
    modifier: Modifier = Modifier
) {
    val activeStep = when (status) {
        OrderStatus.PENDING -> 1
        OrderStatus.APPROVED -> 2
        OrderStatus.SMS_READY -> 3
        OrderStatus.REJECTED -> -1
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        for (step in 1..3) {
            val isCompleted = activeStep >= step
            val isRejected = status == OrderStatus.REJECTED && step == 2
            val circleColor = when {
                isRejected -> StatusRejected
                isCompleted -> if (status == OrderStatus.SMS_READY) StatusApproved else TealAccent
                else -> MaterialTheme.colorScheme.surfaceVariant
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(circleColor),
                contentAlignment = Alignment.Center
            ) {
                if (isRejected) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                } else if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (step < 3) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .background(
                            if (activeStep > step) TealAccent
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }
    }
}
