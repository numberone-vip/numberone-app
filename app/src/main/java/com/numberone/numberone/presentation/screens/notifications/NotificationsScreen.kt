package com.numberone.numberone.presentation.screens.notifications
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.numberone.numberone.core.components.*
import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.presentation.viewmodel.EngagementViewModel

@Composable
fun NotificationsScreen(vm: EngagementViewModel, onBack: () -> Unit) {
    LaunchedEffect(Unit) { vm.loadNotifications() }
    NOScaffold(title = tr("الإشعارات"), onBack = onBack) {
        FeedbackBanner(loading = vm.loading, error = vm.error, message = vm.message)
        if (vm.unreadCount > 0) {
            OutlinedButton(onClick = { vm.markAllNotificationsRead() }, enabled = !vm.loading, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(tr("تحديد الكل كمقروء (${vm.unreadCount})"), fontWeight = FontWeight.Bold)
            }
        }
        if (vm.notifications.isEmpty() && !vm.loading) EmptyState(tr("لا توجد إشعارات حالياً"))
        vm.notifications.forEach { item ->
            val unread = item.isUnread
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (unread) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                border = BorderStroke(if (unread) 1.5.dp else 1.dp, if (unread) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().clickable(enabled = unread) { vm.markNotificationRead(item.id) }
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(if (unread) Icons.Filled.NotificationsActive else Icons.Filled.Notifications, size = 40.dp)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(item.title.ifBlank { tr("إشعار") }, style = MaterialTheme.typography.titleSmall, fontWeight = if (unread) FontWeight.Black else FontWeight.Bold, modifier = Modifier.weight(1f))
                            if (unread) Text(tr("جديد"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        if (item.body.isNotBlank()) Text(item.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (item.createdAt.isNotBlank()) Text(formatDateTime(item.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f))
                    }
                }
            }
        }
    }
}
