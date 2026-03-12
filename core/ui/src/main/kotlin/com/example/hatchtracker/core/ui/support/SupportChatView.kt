package com.example.hatchtracker.core.ui.support

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.data.models.ChatMessage
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hatchtracker.core.ui.support.SupportChatViewModel

@Composable
fun SupportChatView(
    ticketId: String,
    currentUserId: String,
    isAdmin: Boolean,
    viewModel: SupportChatViewModel = hiltViewModel()
) {
    LaunchedEffect(ticketId) {
        viewModel.loadMessages(ticketId)
    }

    val messages by viewModel.messages.collectAsState()
    var input by remember { mutableStateOf("") }
    var isInternalContext by remember { mutableStateOf(false) } // For admin toggle

    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Message List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                MessageBubble(
                    message = msg,
                    isMine = msg.isMine(currentUserId),
                    isAdminView = isAdmin
                )
            }
        }

        // Input Area
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                if (isAdmin) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isInternalContext) "ðŸ”’ Internal Note" else "ðŸ“¢ Public Reply",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isInternalContext) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = isInternalContext,
                            onCheckedChange = { isInternalContext = it },
                            thumbContent = {
                                if (isInternalContext) {
                                    // Lock icon logic if needed
                                }
                            }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { 
                            Text(if (isAdmin && isInternalContext) "Add internal note..." else "Type a message...") 
                        },
                        colors = if (isAdmin && isInternalContext) {
                            OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.error,
                                focusedLabelColor = MaterialTheme.colorScheme.error
                            )
                        } else OutlinedTextFieldDefaults.colors()
                    )
                    IconButton(onClick = {
                        if (input.isNotBlank()) {
                            viewModel.sendMessage(input, isInternal = isInternalContext)
                            input = ""
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isMine: Boolean,
    isAdminView: Boolean
) {
    val isSystem = message.senderRole == "system" || message.senderRole == "hatchy"
    val isInternal = message.isInternal

    // If I'm a regular user, I shouldn't see internal messages anyway (filtered by Security Rules)
    // But IF somehow one slips through, we hide it or show it distinctively?
    // Security rules prevent read, so we assume valid data here.

    val alignment = when {
        isSystem -> Alignment.CenterHorizontally
        isMine -> Alignment.End
        else -> Alignment.Start
    }

    val bubbleColor = when {
        isInternal -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) // Admin Internal
        isSystem -> MaterialTheme.colorScheme.surfaceVariant
        isMine -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (!isMine && !isSystem) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            )
        }
        
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMine) 16.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        "ðŸ”’ INTERNAL", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.error
                    )
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}





