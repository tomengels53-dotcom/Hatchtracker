package com.example.hatchtracker.feature.community.moderation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hatchtracker.core.ui.components.ScreenHeader
import com.example.hatchtracker.feature.community.R
import com.example.hatchtracker.domain.model.ModerationQueueEntry
import com.example.hatchtracker.domain.model.ReportStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModerationQueueScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: ModerationQueueViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val canAccess by viewModel.canAccess.collectAsState()

    if (!canAccess) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("ACCESS DENIED", color = Color.Red, style = MaterialTheme.typography.headlineSmall)
        }
        return
    }

    Scaffold(
        topBar = {
            Column {
                // MODERATION OVERLAY HEADER
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.error)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "TRUST & SAFETY MODE",
                        color = MaterialTheme.colorScheme.onError,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                ScreenHeader(
                    title = "Moderation Queue",
                    onBackClick = onNavigateBack
                )
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(uiState.entries) { entry ->
                    ModerationQueueItem(
                        entry = entry,
                        onClick = { onNavigateToDetail(entry.reportId) }
                    )
                }
                
                if (uiState.entries.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Queue clean. No pending reports.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModerationQueueItem(
    entry: ModerationQueueEntry,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (entry.severity > 3) {
                        Icon(
                            Icons.Default.Warning, 
                            contentDescription = "High Severity",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = entry.targetType.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = when(entry.status) {
                        ReportStatus.OPEN -> MaterialTheme.colorScheme.secondaryContainer
                        ReportStatus.IN_REVIEW -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = entry.status.name,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = stringResource(R.string.moderation_queue_reason, entry.topReason.name),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            
            if (entry.targetPreview != null) {
                Text(
                    text = entry.targetPreview ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.moderation_queue_report_count, entry.reportCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = stringResource(R.string.moderation_queue_priority, entry.queuePriority),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
