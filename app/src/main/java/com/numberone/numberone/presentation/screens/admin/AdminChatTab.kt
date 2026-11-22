package com.numberone.numberone.presentation.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.numberone.numberone.core.components.EmptyState
import com.numberone.numberone.core.components.NOCard
import com.numberone.numberone.core.components.NoticeBanner
import com.numberone.numberone.core.components.SectionTitle
import com.numberone.numberone.core.components.formatDateTime
import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.domain.model.ChatConversation
import com.numberone.numberone.domain.model.ChatMessage
import com.numberone.numberone.presentation.viewmodel.AdminViewModel

/** Customer inbox: the conversation list, or the open conversation with its reply box. */
internal fun LazyListScope.chatItems(vm: AdminViewModel) {
    if (vm.chatUserId == null) {
        item { SectionTitle(tr("المحادثات"), tr("رسائل العملاء تصلك هنا. اضغط على محادثة للرد.")) }
        vm.chatError?.let { item { NoticeBanner(text = it, isError = true) } }
        if (vm.conversations.isEmpty()) item { EmptyState(tr("لا توجد محادثات بعد")) }
        items(vm.conversations, key = { "conv-" + it.userId }) { c ->
            ConversationCard(c) { vm.openChat(c.userId, c.email.orEmpty()) }
        }
    } else {
        item { ChatHeader(vm) }
        vm.chatError?.let { item { NoticeBanner(text = it, isError = true) } }
        if (vm.chatMessages.isEmpty()) item { EmptyState(tr("لا توجد رسائل بعد")) }
        items(vm.chatMessages, key = { "msg-" + it.id }) { m -> MessageBubble(m) }
        item { ChatComposer(vm) }
    }
}

@Composable
private fun ConversationCard(c: ChatConversation, onOpen: () -> Unit) {
    NOCard {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onOpen() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(c.email ?: "—", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    c.lastBody,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(formatDateTime(c.lastAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (c.unread > 0) {
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {
                    Text(
                        c.unread.toString(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatHeader(vm: AdminViewModel) {
    NOCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { vm.closeChat() }) { Text(tr("رجوع"), fontWeight = FontWeight.Bold) }
            Text(
                vm.chatEmail,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            TextButton(onClick = { vm.reloadChat() }) { Text(tr("تحديث")) }
        }
    }
}

@Composable
private fun MessageBubble(m: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (m.fromAdmin) Arrangement.Start else Arrangement.End
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (m.fromAdmin) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(m.body, style = MaterialTheme.typography.bodyMedium)
                Text(
                    formatDateTime(m.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChatComposer(vm: AdminViewModel) {
    var text by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(tr("اكتب ردك…")) },
            maxLines = 4,
            modifier = Modifier.weight(1f)
        )
        Button(
            onClick = { vm.sendChat(text); text = "" },
            enabled = text.isNotBlank() && !vm.chatSending,
            shape = RoundedCornerShape(14.dp)
        ) { Text(tr("إرسال"), fontWeight = FontWeight.Bold) }
    }
}
