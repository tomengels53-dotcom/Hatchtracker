package com.example.hatchtracker.feature.admin

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hatchtracker.data.models.SupportTicket
import com.example.hatchtracker.data.models.TicketStatus
import com.example.hatchtracker.feature.admin.AdminSupportViewModel
import com.example.hatchtracker.feature.admin.TicketAction
import com.example.hatchtracker.core.ui.R as UiR
import com.example.hatchtracker.core.ui.support.SupportChatView
import com.example.hatchtracker.domain.support.SupportDiagnosticsFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTicketDashboardScreen(
    onBack: () -> Unit,
    onNavigateToTicket: (String) -> Unit,
    viewModel: AdminSupportViewModel = hiltViewModel()
) {
    // SECURITY GUARD: Deep Link & Revocation Protection
    val isAdmin by com.example.hatchtracker.auth.UserAuthManager.isSystemAdmin.collectAsState(initial = false)
    
    LaunchedEffect(isAdmin) {
        if (!isAdmin) {
            onBack() // Or navigate to home/login
        }
    }

    // Only render content if admin to prevent flash of content
    if (!isAdmin) return

    val tickets by viewModel.allTickets.collectAsState(initial = emptyList())
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(UiR.string.admin_dashboard_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(UiR.string.back_action))
                    }
                }
            )
        }
    ) { padding ->
        AdminTicketList(
            modifier = Modifier.padding(padding),
            tickets = tickets,
            onTicketClick = { onNavigateToTicket(it.ticketId) }
        )
    }
}

@Composable
fun AdminTicketList(
    modifier: Modifier = Modifier,
    tickets: List<SupportTicket>,
    onTicketClick: (SupportTicket) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tickets) { ticket ->
            val statusColor = when (ticket.status) {
                TicketStatus.SUBMITTED,
                TicketStatus.IN_REVIEW,
                TicketStatus.OPEN,
                TicketStatus.IN_PROGRESS,
                TicketStatus.WAITING_FOR_USER -> MaterialTheme.colorScheme.primaryContainer
                TicketStatus.APPROVED,
                TicketStatus.RESOLVED -> MaterialTheme.colorScheme.tertiaryContainer
                TicketStatus.REJECTED,
                TicketStatus.CLOSED -> MaterialTheme.colorScheme.surfaceVariant
            }
            Card(
                onClick = { onTicketClick(ticket) },
                colors = CardDefaults.cardColors(
                    containerColor = statusColor
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = ticket.subject.ifBlank { stringResource(UiR.string.admin_no_subject) },
                            style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
                        )
                        Badge { Text(ticket.status.name) }
                    }
                    // Priority Indicator
                    if (ticket.priority != com.example.hatchtracker.data.models.TicketPriority.LOW) {
                        Spacer(modifier = Modifier.height(4.dp))
                        SuggestionChip(
                            onClick = { onTicketClick(ticket) },
                            label = { Text(ticket.priority.name) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = when (ticket.priority) {
                                    com.example.hatchtracker.data.models.TicketPriority.CRITICAL -> MaterialTheme.colorScheme.errorContainer
                                    com.example.hatchtracker.data.models.TicketPriority.HIGH -> MaterialTheme.colorScheme.tertiaryContainer
                                    else -> MaterialTheme.colorScheme.secondaryContainer
                                },
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = null
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ticket.userEmail,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ticket.description,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTicketDetailView(
    ticket: SupportTicket,
    notes: List<Map<String, Any>>,
    onBack: () -> Unit,
    onUpdateStatus: (TicketStatus) -> Unit,
    onAddNote: (String) -> Unit,
    onApprove: () -> Unit,
    onResolve: () -> Unit,
    onReject: () -> Unit,
    currentAction: TicketAction?
) {
    var noteInput by remember { mutableStateOf("") }
    var statusExpanded by remember { mutableStateOf(false) }
    var diagnosticsExpanded by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(UiR.string.admin_ticket_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(UiR.string.back_action))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { statusExpanded = true }) {
                            Text(stringResource(UiR.string.status_label))
                        }
                        DropdownMenu(
                            expanded = statusExpanded,
                            onDismissRequest = { statusExpanded = false }
                        ) {
                            TicketStatus.values().forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(status.name) },
                                    onClick = {
                                        onUpdateStatus(status)
                                        statusExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // User Info
            Text(
                stringResource(UiR.string.admin_user_label, ticket.userEmail),
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                stringResource(UiR.string.admin_device_label, ticket.deviceInfo["model"] ?: stringResource(UiR.string.unknown_label)),
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Content
            Text(ticket.subject, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(ticket.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(12.dp))
            val isActionRunning = currentAction != null
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val approveLabel = if (currentAction == TicketAction.APPROVE) {
                    stringResource(UiR.string.admin_approving)
                } else {
                    stringResource(UiR.string.admin_approve)
                }
                Button(
                    onClick = onApprove,
                    enabled = !isActionRunning && ticket.status != TicketStatus.APPROVED,
                    modifier = Modifier.weight(1f)
                ) {
                    if (currentAction == TicketAction.APPROVE) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(approveLabel)
                }
                val resolveLabel = if (currentAction == TicketAction.RESOLVE) {
                    stringResource(UiR.string.admin_resolving)
                } else {
                    stringResource(UiR.string.admin_resolve)
                }
                OutlinedButton(
                    onClick = onResolve,
                    enabled = !isActionRunning && ticket.status != TicketStatus.RESOLVED,
                    modifier = Modifier.weight(1f)
                ) {
                    if (currentAction == TicketAction.RESOLVE) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(resolveLabel)
                }
                val rejectLabel = if (currentAction == TicketAction.REJECT) {
                    stringResource(UiR.string.admin_rejecting)
                } else {
                    stringResource(UiR.string.admin_reject)
                }
                TextButton(
                    onClick = onReject,
                    enabled = !isActionRunning && ticket.status != TicketStatus.REJECTED
                ) {
                    if (currentAction == TicketAction.REJECT) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(rejectLabel)
                }
            }
            val actionTip = when {
                isActionRunning -> stringResource(
                    UiR.string.admin_action_processing,
                    currentAction?.name?.lowercase()?.replace('_', ' ') ?: ""
                )
                ticket.status == TicketStatus.APPROVED -> stringResource(UiR.string.admin_action_status_saved)
                ticket.status == TicketStatus.RESOLVED -> stringResource(UiR.string.admin_action_resolved)
                ticket.status == TicketStatus.REJECTED -> stringResource(UiR.string.admin_action_rejected)
                else -> stringResource(UiR.string.admin_action_committed)
            }
            Text(
                text = actionTip,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            ticket.approvedAt?.let { approvedAt ->
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    Text(
                        stringResource(UiR.string.admin_approved_by, ticket.approvedBy ?: "Admin"),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        stringResource(UiR.string.admin_at_label, java.text.SimpleDateFormat.getDateTimeInstance().format(approvedAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            val diagnostics = ticket.diagnostics
            if (diagnostics != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(UiR.string.admin_diagnostics_title), style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val text = SupportDiagnosticsFormatter.format(diagnostics)
                                clipboardManager.setText(AnnotatedString(text))
                            }
                        ) { Text(stringResource(UiR.string.finance_label_export)) }
                        TextButton(onClick = { diagnosticsExpanded = !diagnosticsExpanded }) {
                            Text(if (diagnosticsExpanded) stringResource(UiR.string.admin_hide) else stringResource(UiR.string.admin_show))
                        }
                    }
                }
                if (diagnosticsExpanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(UiR.string.admin_app_label, diagnostics.appVersionName, diagnostics.appVersionCode),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(stringResource(UiR.string.admin_device_label, diagnostics.deviceModel), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(UiR.string.admin_android_label, diagnostics.androidVersion), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(UiR.string.admin_user_id_label, diagnostics.userId), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(UiR.string.admin_subscription_label, diagnostics.subscriptionTier), style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(stringResource(UiR.string.admin_worker_runs), style = MaterialTheme.typography.labelLarge)
                    if (diagnostics.workerRuns.isEmpty()) {
                        Text(stringResource(UiR.string.admin_no_worker_status), style = MaterialTheme.typography.bodySmall)
                    } else {
                        diagnostics.workerRuns.forEach { status ->
                            val success = status.lastSuccessAt?.let { java.text.SimpleDateFormat.getDateTimeInstance().format(java.util.Date(it)) }
                                ?: stringResource(UiR.string.admin_not_available)
                            val failure = status.lastFailureAt?.let { java.text.SimpleDateFormat.getDateTimeInstance().format(java.util.Date(it)) }
                                ?: stringResource(UiR.string.admin_not_available)
                            Text(
                                stringResource(UiR.string.admin_worker_run_summary, status.workerName, success, failure),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(stringResource(UiR.string.admin_recent_logs), style = MaterialTheme.typography.labelLarge)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 220.dp)
                            .verticalScroll(rememberScrollState())
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(8.dp)
                    ) {
                        Text(
                            diagnostics.logLines.joinToString("\n"),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }

            // -- HATCHY TRIAGE UI --
            if (ticket.hatchyTriage.isNotEmpty()) {
                val classification = ticket.hatchyTriage["classification"] as? String ?: stringResource(UiR.string.unknown_label)
                val confidence = (ticket.hatchyTriage["confidenceScore"] as? Number)?.toDouble() ?: 0.0
                val suggestedResponse = ticket.hatchyTriage["suggestedResponse"] as? String
                val disclaimer = ticket.hatchyTriage["disclaimer"] as? String
                val nextSteps = ticket.hatchyTriage["nextSteps"] as? List<*>
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(UiR.string.admin_hatchy_analysis), style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.weight(1f))
                            Badge(
                                containerColor = if (confidence > 0.8) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error 
                            ) { 
                                Text(stringResource(UiR.string.admin_confidence_short, (confidence * 100).toInt()))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(UiR.string.admin_classified_as, classification), style = MaterialTheme.typography.labelLarge)
                        
                        if (suggestedResponse != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(UiR.string.admin_suggested_response), style = MaterialTheme.typography.labelSmall)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Text(
                                    text = suggestedResponse,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        if (nextSteps != null && nextSteps.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(UiR.string.admin_suggested_next_steps), style = MaterialTheme.typography.labelSmall)
                            nextSteps.filterIsInstance<String>().forEach { step ->
                                Text(stringResource(UiR.string.admin_bullet_point, step), style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        if (disclaimer != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                disclaimer,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            
            // -- CHAT SYSTEM --
            Text(stringResource(UiR.string.admin_support_chat), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                // We use passing "true" for isAdmin to enable internal toggle
                // We need current user ID for alignment (isMine) logic
                val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                
                SupportChatView(
                    ticketId = ticket.ticketId,
                    currentUserId = currentUserId,
                    isAdmin = true
                )
            }
        }
    }
}







