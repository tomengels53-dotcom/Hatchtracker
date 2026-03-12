package com.example.hatchtracker.feature.admin

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hatchtracker.model.AdminAuditLog
import com.example.hatchtracker.model.AuditActionType
import com.example.hatchtracker.feature.admin.AdminAuditLogViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAuditLogScreen(
    onBack: () -> Unit,
    viewModel: AdminAuditLogViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audit Logs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearch(it) },
                    label = { Text("Search") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )
                
                // Action Filter Dropdown (simplified as a simplified menu or just a text for now, or nice chip row)
                // Let's use a Chip Row for common actions if space permits, or just a Filter Chip that opens a dialog.
                // For simplicity/speed: A row of selectable chips for main actions.
            }
            
            // Chips for filtering
            ScrollableTabRow(
                selectedTabIndex = if (uiState.selectedActionFilter == null) 0 else 1,
                edgePadding = 8.dp,
                indicator = {},
                divider = {}
            ) {
                FilterChip(
                    selected = uiState.selectedActionFilter == null,
                    onClick = { viewModel.updateActionFilter(null) },
                    label = { Text("All") },
                    modifier = Modifier.padding(end = 8.dp)
                )
                AuditActionType.values().forEach { action ->
                    FilterChip(
                        selected = uiState.selectedActionFilter == action,
                        onClick = { viewModel.updateActionFilter(if (uiState.selectedActionFilter == action) null else action) },
                        label = { Text(action.name) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.filteredLogs) { log ->
                        AuditLogCard(log)
                    }
                }
            }
        }
    }
}

@Composable
fun AuditLogCard(log: AdminAuditLog) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = when(log.actionType) {
                AuditActionType.DELETE, AuditActionType.DEPRECATE -> MaterialTheme.colorScheme.errorContainer
                AuditActionType.CREATE -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "[${log.actionType}] ${log.targetCollection}",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "ID: ${log.targetDocumentId}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
                Text(
                    text = dateFormat.format(Date(log.timestamp)),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            Text("By: ${log.adminEmail}", style = MaterialTheme.typography.bodyMedium)
            
            if (!log.reason.isNullOrBlank()) {
                Text("Reason: ${log.reason}", style = MaterialTheme.typography.bodySmall, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider()
                    Text("Details:", style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis, modifier = Modifier.padding(vertical = 4.dp))
                    if (log.beforeSnapshot != null) {
                        Text("Before: ${log.beforeSnapshot}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (log.afterSnapshot != null) {
                        Text("After: ${log.afterSnapshot}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}



