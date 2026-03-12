@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.hatchtracker.feature.support

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hatchtracker.data.models.ChatMessage
import com.example.hatchtracker.data.models.SupportFeature
import com.example.hatchtracker.data.models.SupportModule
import com.example.hatchtracker.data.models.SupportTicket
import com.example.hatchtracker.data.models.TicketStatus
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(
    onBack: () -> Unit,
    initialModuleId: String? = null,
    initialFeatureId: String? = null,
    onNavigateToAdminDashboard: () -> Unit = {},
    viewModel: SupportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val tickets by viewModel.userTickets.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val messages by viewModel.ticketMessages.collectAsState()
    val currentUserId = viewModel.sessionManager.getCurrentUser()?.uid ?: ""

    // Admin Check
    val isAdmin by viewModel.isAdmin.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedTicket by remember { mutableStateOf<SupportTicket?>(null) }
    var draftMessage by remember { mutableStateOf("") }

    LaunchedEffect(uiState.modules, initialModuleId) {
        if (initialModuleId != null && uiState.selectedModule == null) {
            uiState.modules.find { it.id == initialModuleId }?.let { viewModel.onModuleSelected(it) }
        }
    }

    LaunchedEffect(uiState.features, initialFeatureId) {
        if (initialFeatureId != null && uiState.selectedFeature == null) {
            uiState.features.find { it.id == initialFeatureId }?.let { viewModel.onFeatureSelected(it) }
        }
    }

    Scaffold(
        modifier = Modifier.testTag("HelpSupportScreen"),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.support_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.support_title))
                        }
                    },
                    actions = {
                        if (isAdmin) {
                            TextButton(onClick = onNavigateToAdminDashboard) {
                                Text(stringResource(R.string.support_admin_panel))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
                if (!isOnline) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.offline_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.testTag("SupportTabRow")
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(R.string.tab_new_request)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(R.string.tab_my_tickets)) }
                    )
                }
            }
        }
    ) { padding ->
        when {
            selectedTab == 0 -> {
                if (uiState.isSuccess) {
                    SupportSuccessView(
                        modifier = Modifier.padding(padding),
                        onBack = {
                            viewModel.reset()
                            selectedTab = 1
                        }
                    )
                } else {
                    val localizationModule = uiState.modules.firstOrNull { it.id == "localization" }
                    val translationFeature = uiState.features.firstOrNull { it.id == "translation_error" }
                        ?: SupportFeature("translation_error", "localization", "")

                    SupportTicketForm(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize(),
                        uiState = uiState,
                        isOnline = isOnline,
                        onModuleSelected = viewModel::onModuleSelected,
                        onFeatureSelected = viewModel::onFeatureSelected,
                        onMessageChanged = viewModel::onMessageChanged,
                        onRequestedCountryChanged = viewModel::onRequestedCountryChanged,
                        onAcknowledgementChanged = viewModel::onAcknowledgementChanged,
                        onSubmit = viewModel::submitTicket,
                        onSelectLocalizationShortcut = {
                            if (localizationModule != null) {
                                viewModel.onModuleSelected(localizationModule)
                                viewModel.onFeatureSelected(translationFeature)
                            }
                        }
                    )
                }
            }
            selectedTicket != null -> {
                TicketDetailView(
                    modifier = Modifier.padding(padding),
                    ticket = selectedTicket!!,
                    messages = messages,
                    currentUserId = currentUserId,
                    draftMessage = draftMessage,
                    onMessageChange = { draftMessage = it },
                    onSendMessage = {
                        viewModel.sendTicketMessage(selectedTicket!!.ticketId, draftMessage)
                        draftMessage = ""
                    },
                    onClose = {
                        selectedTicket = null
                        draftMessage = ""
                        viewModel.clearTicketMessages()
                    }
                )
            }
            else -> {
                TicketHistoryList(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    tickets = tickets,
                    onTicketClick = { ticket ->
                        selectedTicket = ticket
                        draftMessage = ""
                        selectedTab = 1
                        viewModel.watchMessagesForTicket(ticket.ticketId)
                    }
                )
            }
        }
    }
}

@Composable
private fun SupportTicketForm(
    modifier: Modifier = Modifier,
    uiState: SupportUiState,
    isOnline: Boolean,
    onModuleSelected: (SupportModule) -> Unit,
    onFeatureSelected: (SupportFeature) -> Unit,
    onMessageChanged: (String) -> Unit,
    onRequestedCountryChanged: (String) -> Unit,
    onAcknowledgementChanged: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onSelectLocalizationShortcut: () -> Unit
) {
    val scrollState = rememberScrollState()
    var moduleExpanded by remember { mutableStateOf(false) }
    var featureExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            stringResource(R.string.support_form_header),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.support_translation_quick_title),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
                )
                Text(
                    text = stringResource(R.string.support_translation_quick_desc),
                    style = MaterialTheme.typography.bodySmall
                )
                TextButton(onClick = onSelectLocalizationShortcut) {
                    Text(stringResource(R.string.support_translation_quick_action))
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = uiState.selectedModule?.let { supportModuleLabel(it) } ?: stringResource(R.string.select_module),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.label_module)) },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { moduleExpanded = !moduleExpanded }) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                }
            )
            DropdownMenu(
                expanded = moduleExpanded,
                onDismissRequest = { moduleExpanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                uiState.modules.forEach { module ->
                    DropdownMenuItem(
                        text = { Text(supportModuleLabel(module)) },
                        onClick = {
                            onModuleSelected(module)
                            moduleExpanded = false
                        }
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = uiState.selectedFeature?.let { supportFeatureLabel(it) } ?: stringResource(R.string.select_feature),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.label_feature)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.selectedModule != null,
                trailingIcon = {
                    IconButton(
                        onClick = { if (uiState.selectedModule != null) featureExpanded = !featureExpanded },
                        enabled = uiState.selectedModule != null
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                }
            )
            DropdownMenu(
                expanded = featureExpanded,
                onDismissRequest = { featureExpanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                uiState.features.forEach { feature ->
                    DropdownMenuItem(
                        text = { Text(supportFeatureLabel(feature)) },
                        onClick = {
                            onFeatureSelected(feature)
                            featureExpanded = false
                        }
                    )
                }
            }
        }

        if (uiState.selectedFeature?.id == "change_country") {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        stringResource(R.string.country_change_title),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
                    )
                    Text(
                        stringResource(R.string.country_change_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    OutlinedTextField(
                        value = uiState.requestedCountry,
                        onValueChange = onRequestedCountryChanged,
                        label = { Text(stringResource(R.string.label_country_code)) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.placeholder_country_code)) },
                        maxLines = 1,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAcknowledgementChanged(!uiState.isAcknowledged) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = uiState.isAcknowledged,
                            onCheckedChange = onAcknowledgementChanged
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.country_change_acknowledgement),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = uiState.message,
            onValueChange = onMessageChanged,
            label = { Text(stringResource(R.string.label_message)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            placeholder = { Text(stringResource(R.string.placeholder_message)) },
            maxLines = 6,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
        )

        uiState.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.selectedModule != null &&
                uiState.selectedFeature != null &&
                uiState.message.isNotBlank() &&
                (uiState.selectedFeature?.id != "change_country" || (uiState.requestedCountry.length == 2 && uiState.isAcknowledged)) &&
                !uiState.isSubmitting
        ) {
            if (uiState.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(if (isOnline) stringResource(R.string.submit_ticket) else stringResource(R.string.queue_ticket))
            }
        }
    }
}

@Composable
private fun supportModuleLabel(module: SupportModule): String = when (module.id) {
    "flock" -> stringResource(R.string.support_module_flock)
    "incubation" -> stringResource(R.string.support_module_incubation)
    "breeding" -> stringResource(R.string.support_module_breeding)
    "nursery" -> stringResource(R.string.support_module_nursery)
    "financial" -> stringResource(R.string.support_module_financial)
    "user_profile" -> stringResource(R.string.support_module_profile)
    "localization" -> stringResource(R.string.support_module_localization)
    "other" -> stringResource(R.string.support_module_other)
    else -> module.name
}

@Composable
private fun supportFeatureLabel(feature: SupportFeature): String = when (feature.id) {
    "add_bird" -> stringResource(R.string.support_feature_adding_birds)
    "flock_edit" -> stringResource(R.string.support_feature_editing_flocks)
    "inventory" -> stringResource(R.string.support_feature_bird_inventory)
    "start_hatch" -> stringResource(R.string.support_feature_starting_hatch)
    "candling" -> stringResource(R.string.support_feature_candling_results)
    "hatch_outcome" -> stringResource(R.string.support_feature_hatch_statistics)
    "pairing" -> stringResource(R.string.support_feature_creating_pairs)
    "recommendation" -> stringResource(R.string.support_feature_ai_recommendations)
    "compatibility" -> stringResource(R.string.support_feature_genetic_compatibility)
    "flocklet_stats" -> stringResource(R.string.support_feature_updating_chick_stats)
    "brooder_temp" -> stringResource(R.string.support_feature_temperature_tracking)
    "move_to_flock" -> stringResource(R.string.support_feature_moving_to_adult_flock)
    "sales" -> stringResource(R.string.support_feature_sales_tracking)
    "expenses" -> stringResource(R.string.support_feature_expense_logging)
    "summary" -> stringResource(R.string.support_feature_financial_overview)
    "change_country" -> stringResource(R.string.support_feature_country_change)
    "data_correction" -> stringResource(R.string.support_feature_account_data_correction)
    "identity_mismatch" -> stringResource(R.string.support_feature_identity_mismatch)
    "translation_error" -> stringResource(R.string.support_feature_translation_error)
    "missing_translation" -> stringResource(R.string.support_feature_missing_translation)
    "language_switch_issue" -> stringResource(R.string.support_feature_language_switch_issue)
    "general" -> stringResource(R.string.support_feature_general_issue)
    "bug" -> stringResource(R.string.support_feature_report_bug)
    "suggestion" -> stringResource(R.string.support_feature_suggestion)
    else -> feature.name
}

@Composable
private fun SupportSuccessView(modifier: Modifier = Modifier, onBack: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            stringResource(R.string.ticket_submitted_title),
            style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            stringResource(R.string.ticket_submitted_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.back_to_app))
        }
    }
}

@Composable
private fun TicketHistoryList(
    modifier: Modifier = Modifier,
    tickets: List<SupportTicket>,
    onTicketClick: (SupportTicket) -> Unit
) {
    if (tickets.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.no_tickets_found),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tickets) { ticket ->
            Card(
                onClick = { onTicketClick(ticket) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ticket.subject.ifBlank { stringResource(R.string.default_ticket_subject) },
                            style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
                        )
                        TicketStatusBadge(status = ticket.status)
                    }
                    Text(
                        text = ticket.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.created_at_format, formatTicketTimestamp(ticket.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ticket.changeRequest?.let { change ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    stringResource(
                                        R.string.change_request_format,
                                        change.oldValue.ifBlank { "—" },
                                        change.newValue.ifBlank { "—" }
                                    ),
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
                                )
                                change.adminNotes?.let { adminNote ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        stringResource(R.string.admin_note_format, adminNote),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TicketDetailView(
    modifier: Modifier = Modifier,
    ticket: SupportTicket,
    messages: List<ChatMessage>,
    currentUserId: String,
    draftMessage: String,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.ticket_detail_back))
                }
                Text(
                    stringResource(R.string.ticket_details_title),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
                )
            }
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.ticket_detail_back))
            }
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ticket.subject.ifBlank { stringResource(R.string.default_ticket_subject) },
                        style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
                    )
                    TicketStatusBadge(status = ticket.status)
                }
                Text(
                    text = ticket.description,
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column {
                        Text(stringResource(R.string.ticket_module_label), style = MaterialTheme.typography.labelSmall)
                        Text(ticket.categoryDetail.moduleName.ifBlank { "—" }, style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                    }
                    Column {
                        Text(stringResource(R.string.ticket_feature_label), style = MaterialTheme.typography.labelSmall)
                        Text(ticket.categoryDetail.featureName.ifBlank { "—" }, style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                    }
                }
                Text(
                    text = stringResource(R.string.created_at_format, formatTicketTimestamp(ticket.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ticket.changeRequest?.adminNotes?.let { adminNote ->
                    Text(
                        stringResource(R.string.admin_note_format, adminNote),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        TicketChatView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            messages = messages,
            currentUserId = currentUserId,
            draftMessage = draftMessage,
            onMessageChange = onMessageChange,
            onSendMessage = onSendMessage
        )
    }
}

@Composable
private fun TicketChatView(
    modifier: Modifier = Modifier,
    messages: List<ChatMessage>,
    currentUserId: String,
    draftMessage: String,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit
) {
    val listState = rememberLazyListState()
    var autoScrollEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(messages.size, autoScrollEnabled) {
        if (autoScrollEnabled && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    LaunchedEffect(listState.isScrollInProgress, messages.size) {
        if (listState.isScrollInProgress) {
            autoScrollEnabled = false
        } else {
            val lastIndex = messages.lastIndex
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            autoScrollEnabled = lastIndex < 0 || (lastVisible != null && lastVisible >= lastIndex - 1)
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            stringResource(R.string.ticket_chat_header),
            style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
        )
        HorizontalDivider()
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_messages_yet),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(messages) { message ->
                    TicketMessageBubble(message = message, isCurrentUser = message.senderId == currentUserId)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = draftMessage,
                onValueChange = onMessageChange,
                placeholder = { Text(stringResource(R.string.ticket_chat_placeholder)) },
                modifier = Modifier.weight(1f),
                maxLines = 3,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send)
            )
            IconButton(
                onClick = onSendMessage,
                enabled = draftMessage.isNotBlank()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.ticket_chat_send))
            }
        }
    }
}

@Composable
private fun TicketMessageBubble(message: ChatMessage, isCurrentUser: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .background(
                    color = if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = message.senderName.ifBlank { if (isCurrentUser) stringResource(R.string.sender_you) else message.senderRole },
                style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
            )
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = formatMessageTimestamp(message.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TicketStatusBadge(status: TicketStatus) {
    val (containerColor, contentColor) = when (status) {
        TicketStatus.SUBMITTED,
        TicketStatus.OPEN,
        TicketStatus.IN_REVIEW,
        TicketStatus.IN_PROGRESS -> {
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        }
        TicketStatus.APPROVED,
        TicketStatus.RESOLVED -> {
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        }
        TicketStatus.REJECTED,
        TicketStatus.CLOSED -> {
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        }
        else -> {
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        }
    }
    Badge(
        containerColor = containerColor,
        contentColor = contentColor
    ) {
        Text(status.name.replace('_', ' '), style = MaterialTheme.typography.labelSmall)
    }
}

private fun formatTicketTimestamp(date: Date?): String {
    return date?.let {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(it)
    } ?: "—"
}

private fun formatMessageTimestamp(millis: Long?): String {
    return millis?.let {
        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it))
    } ?: ""
}
