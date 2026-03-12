package com.example.hatchtracker.feature.notifications

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hatchtracker.data.models.InboxNotification
import com.example.hatchtracker.feature.notifications.NotificationCenterViewModel
import kotlinx.coroutines.launch
import com.example.hatchtracker.core.ui.R as UiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    viewModel: NotificationCenterViewModel,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onIncubationClick: (Long) -> Unit = {}
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(UiR.string.notification_center_title)) },
                navigationIcon = {
                    TextButton(onClick = onBackClick) { Text(stringResource(UiR.string.close)) }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearReadInfo() }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(UiR.string.clear_read_info_content_description))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Notifications, 
                        contentDescription = null, 
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(UiR.string.no_new_notifications),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(notifications, key = { it.id }) { notification ->
                    NotificationCard(
                        notification = notification,
                        onDismiss = { viewModel.delete(notification.id) },
                        onSnooze = { viewModel.snooze(notification.id) },
                        onClick = { 
                            viewModel.markAsRead(notification.id)
                            onIncubationClick(notification.incubationId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: InboxNotification,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
    onClick: () -> Unit
) {
    val isCritical = notification.severity == "CRITICAL"
    val isWarning = notification.severity == "WARNING"
    
    val containerColor = when {
        isCritical -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        isWarning -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surface
    }
    
    val borderColor = when {
        isCritical -> MaterialTheme.colorScheme.error
        isWarning -> MaterialTheme.colorScheme.tertiary
        else -> Color.Transparent
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (borderColor != Color.Transparent) androidx.compose.foundation.BorderStroke(1.dp, borderColor) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (isCritical) Icons.Default.Warning else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isCritical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = notification.title,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
                        )
                        Text(
                            text = DateUtils.getRelativeTimeSpanString(notification.timestamp).toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (!notification.isRead) {
                    Badge(containerColor = MaterialTheme.colorScheme.primary)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onSnooze) {
                    Text(stringResource(UiR.string.snooze_24h_action))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(UiR.string.dismiss_action))
                }
            }
        }
    }
}



