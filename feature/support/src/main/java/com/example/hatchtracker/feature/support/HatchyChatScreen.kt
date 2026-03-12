package com.example.hatchtracker.feature.support

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.example.hatchtracker.core.ui.R
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hatchtracker.domain.hatchy.model.HatchyChatMessage
import com.example.hatchtracker.domain.hatchy.model.SenderType
import com.example.hatchtracker.domain.hatchy.model.FeedbackType
import com.example.hatchtracker.feature.support.HatchyChatViewModel
import com.example.hatchtracker.core.ui.components.HatchyFeedbackDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HatchyChatScreen(
    onBack: () -> Unit,
    viewModel: HatchyChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var feedbackMessageId by rememberSaveable { mutableStateOf<String?>(null) }
    var isLearningFeedback by rememberSaveable { mutableStateOf(false) }
    val messagesReversed = remember(uiState.messages) { uiState.messages.asReversed() }
    val isNearBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex <= 1
        }
    }

    LaunchedEffect(messagesReversed.size) {
        if (messagesReversed.isNotEmpty()) {
            if (isNearBottom) {
                listState.animateScrollToItem(0)
            }
            
            // Auto-prompt feedback for "Ignorance Fallback" messages
            val lastMessage = uiState.messages.last()
            if (lastMessage.sender == SenderType.HATCHY && 
                lastMessage.text.contains("don’t have a reliable answer") &&
                lastMessage.feedback == FeedbackType.NONE
            ) {
                feedbackMessageId = lastMessage.id
                isLearningFeedback = true
            }
        }
    }

    if (feedbackMessageId != null) {
        HatchyFeedbackDialog(
            title = if (isLearningFeedback) androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.support.R.string.feedback_title_learning) else androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.support.R.string.feedback_title_improvement),
            description = if (isLearningFeedback) 
                androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.support.R.string.feedback_desc_learning) 
                else androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.support.R.string.feedback_desc_improvement),
            onDismiss = { 
                feedbackMessageId = null
                isLearningFeedback = false
            },
            onSubmit = { comment ->
                viewModel.submitFeedback(feedbackMessageId!!, FeedbackType.THUMBS_DOWN, comment)
                feedbackMessageId = null
                isLearningFeedback = false
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.support.R.string.chat_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (uiState.isPro) androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.support.R.string.pro_mode_label) else androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.support.R.string.basic_mode_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.support.R.string.desc_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .imePadding()
        ) {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.isProcessing) {
                    item {
                        Text(
                            androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.support.R.string.hatchy_thinking),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(messagesReversed, key = { it.id }) { message ->
                    ChatMessageBubble(
                        message = message,
                        onFeedback = { mId, type ->
                            if (type == FeedbackType.THUMBS_UP) {
                                viewModel.submitFeedback(mId, type)
                            } else {
                                feedbackMessageId = mId
                            }
                        }
                    )
                }

                if (uiState.messages.size <= 1) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Try asking about:",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val suggestions = listOf(
                                    "Breeding advice",
                                    "Incubation timing",
                                    "Brooder care",
                                    "Flock records",
                                    "App help"
                                )
                                suggestions.forEach { suggestion ->
                                    AssistChip(
                                        onClick = { viewModel.sendMessage(suggestion) },
                                        label = { Text(suggestion) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Production Chat Input Bar
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.support.R.string.chat_placeholder)) },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Send,
                            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSend = {
                                if (messageText.isNotBlank()) {
                                    viewModel.sendMessage(messageText)
                                    messageText = ""
                                }
                            }
                        )
                    )
                    
                    FloatingActionButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(messageText)
                                messageText = ""
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.support.R.string.ticket_chat_send))
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: HatchyChatMessage,
    onFeedback: (String, FeedbackType) -> Unit
) {
    val isUser = message.sender == SenderType.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val shape = if (isUser) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            Image(
                painter = painterResource(id = R.drawable.hatchy_1),
                contentDescription = androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.support.R.string.sender_hatchy),
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = alignment,
            modifier = Modifier.weight(1f, fill = false) 
        ) {
            Surface(
                color = containerColor,
                shape = shape,
                tonalElevation = 1.dp
            ) {
                Column {
                    Text(
                        text = message.text,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    if (!isUser) {
                        Row(
                            modifier = Modifier.padding(bottom = 4.dp, start = 8.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            IconButton(
                                onClick = { onFeedback(message.id, FeedbackType.THUMBS_UP) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.ThumbUp, 
                                    contentDescription = androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.support.R.string.helpful_desc),
                                    tint = if (message.feedback == FeedbackType.THUMBS_UP) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { onFeedback(message.id, FeedbackType.THUMBS_DOWN) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.ThumbDown, 
                                    contentDescription = androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.support.R.string.not_helpful_desc),
                                    tint = if (message.feedback == FeedbackType.THUMBS_DOWN) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
            Text(
                text = if (isUser) androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.support.R.string.sender_you) else androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.support.R.string.sender_hatchy),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
                style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
