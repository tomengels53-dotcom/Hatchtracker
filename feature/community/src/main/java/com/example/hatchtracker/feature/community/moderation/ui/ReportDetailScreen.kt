package com.example.hatchtracker.feature.community.moderation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hatchtracker.core.ui.components.ScreenHeader
import com.example.hatchtracker.domain.model.ReportStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReportDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val canAccess by viewModel.canAccess.collectAsState()
    var showActionDialog by remember { mutableStateOf(false) }
    var actionNote by remember { mutableStateOf("") }

    if (!canAccess) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("ACCESS DENIED", color = Color.Red)
        }
        return
    }

    Scaffold(
        topBar = {
            Column {
                Box(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.error).padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("TRUST & SAFETY: DETAIL REVIEW", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
                ScreenHeader(title = "Report Details", onBackClick = onNavigateBack)
            }
        }
    ) { padding ->
        val report = uiState.report
        if (uiState.isLoading || report == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // STATUS CARD
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Status: ${report.status.name}", fontWeight = FontWeight.Bold)
                        Text("ID: ${report.id.take(8)}...", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // TARGET INFO
                SectionHeader("Target Information")
                Text("Type: ${report.targetType.name}")
                Text("Target ID: ${report.targetId}")
                Text("Reported User: ${report.reportedUserId}")

                // REASON
                SectionHeader("Report Reason")
                Text(report.reasonCode.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                if (report.details.isNotEmpty()) {
                    Text(report.details, style = MaterialTheme.typography.bodyMedium)
                }

                // PREVIEW SNAPSHOT
                if (report.targetPreviewSnapshot != null) {
                    SectionHeader("Content Preview")
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = report.targetPreviewSnapshot ?: "",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // MOTERATION ACTIONS
                if (report.status == ReportStatus.OPEN || report.status == ReportStatus.IN_REVIEW) {
                    Button(
                        onClick = { showActionDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("TAKE ACTION")
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.resolveReport(ReportStatus.DISMISSED, "False report / Dismissed", false) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("DISMISS REPORT")
                }
            }
        }
    }

    if (showActionDialog) {
        AlertDialog(
            onDismissRequest = { showActionDialog = false },
            title = { Text("Take Moderation Action") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add a note explaining the resolution/action:")
                    OutlinedTextField(
                        value = actionNote,
                        onValueChange = { actionNote = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Moderator note...") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.issueStrike(uiState.report?.reasonCode ?: return@TextButton, actionNote)
                    showActionDialog = false
                }) {
                    Text("ISSUE STRIKE & RESOLVE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showActionDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.outline,
        fontWeight = FontWeight.Bold
    )
}
