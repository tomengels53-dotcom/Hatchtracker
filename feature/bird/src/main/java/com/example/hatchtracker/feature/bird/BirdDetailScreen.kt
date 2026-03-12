package com.example.hatchtracker.feature.bird

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hatchtracker.data.models.ConfidenceLevel
import com.example.hatchtracker.auth.UserAuthManager
import kotlinx.coroutines.launch
import com.example.hatchtracker.data.models.TraitDisplayCatalog
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.res.stringResource
import com.example.hatchtracker.core.ui.components.ProfileHeroHeader
import com.example.hatchtracker.core.ui.util.rememberProfileImageState
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.CatchingPokemon
import com.example.hatchtracker.core.ui.R as UiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirdDetailScreen(
    birdId: Long,
    onBack: () -> Unit,
    onRecordSale: (String, String, List<Long>) -> Unit = { _, _, _ -> },
    viewModel: BirdDetailViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val userProfileDomain by viewModel.userProfile.collectAsState()
    val userProfile by UserAuthManager.currentUser.collectAsState()
    val isSystemAdmin by UserAuthManager.isSystemAdmin.collectAsState()
    val isCommunityAdmin by UserAuthManager.isCommunityAdmin.collectAsState()
    val isAdmin = isSystemAdmin || isCommunityAdmin

    val bird by viewModel.bird.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val dateFormat by viewModel.dateFormat.collectAsState()
    val errorMsg by viewModel.error.collectAsState()
    val geneticInsight by viewModel.geneticInsight.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    val hasPro = com.example.hatchtracker.common.util.BreedingGating.hasProAccess(userProfileDomain)
    
    LaunchedEffect(errorMsg) {
        errorMsg?.let { error ->
            val message = when (error) {
                "PRO_LEVEL_REQUIRED_OVERRIDES" -> "Manual Genetic Overrides require a PRO subscription."
                else -> error
            }
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    LaunchedEffect(birdId) {
        viewModel.loadBird(birdId)
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is BirdUiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    var showRecordLossDialog by remember { mutableStateOf(false) }
    var showOverridePicker by remember { mutableStateOf(false) }

    if (showOverridePicker) {
        val currentOverrides = bird?.customGeneticProfile?.traitOverrides ?: emptyList()
        val selectedTraitsMap = currentOverrides.associate { it.traitId to it.optionId }
        
        AlertDialog(
            onDismissRequest = { showOverridePicker = false },
            title = { Text(stringResource(UiR.string.genetic_overrides_label)) },
            text = {
                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                    com.example.hatchtracker.core.ui.components.GeneticTraitPicker(
                        selectedTraits = selectedTraitsMap,
                        onTraitChanged = { traitId, optionId ->
                            val category = com.example.hatchtracker.data.models.TraitDisplayCatalog.getTrait(traitId)?.category ?: com.example.hatchtracker.data.models.TraitCategory.PHYSICAL
                            viewModel.updateTraitOverride(traitId, optionId, category)
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showOverridePicker = false }) {
                    Text(stringResource(UiR.string.action_done))
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("") }, // Title is handled by HeroHeader
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(UiR.string.back_action))
                    }
                },
                actions = {
                    // Options Menu
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(UiR.string.options_content_description)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (bird?.status == "active") {
                                DropdownMenuItem(
                                    text = { Text(stringResource(UiR.string.record_sale_action)) },
                                    onClick = {
                                        showMenu = false
                                        bird?.let { onRecordSale(it.syncId, "flock", listOf(it.id)) }
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.ShoppingCart, contentDescription = null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(UiR.string.record_loss_content_description), color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        showRecordLossDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        var showLogEventDialog by remember { mutableStateOf(false) }

        if (showLogEventDialog) {
            LogHealthEventDialog(
                onDismiss = { showLogEventDialog = false },
                onLog = { type, notes ->
                    when (type) {
                        "Vaccination" -> viewModel.recordVaccination(notes)
                        "Treatment" -> viewModel.recordTreatment(notes)
                        else -> viewModel.recordHealthEvent(type, notes)
                    }
                    showLogEventDialog = false
                }
            )
        }
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (bird == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(UiR.string.bird_not_found))
            }
        } else {
            val currentBird = bird!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Header
                val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                ) { uri: android.net.Uri? ->
                    uri?.let {
                        scope.launch {
                            viewModel.saveProfilePhoto(it)
                                .onFailure { snackbarHostState.showSnackbar("Failed to save photo") }
                        }
                    }
                }

                ProfileHeroHeader(
                    state = rememberProfileImageState(
                        imagePath = currentBird.imagePath,
                        species = currentBird.species
                    ),
                    title = stringResource(UiR.string.bird_title_format, currentBird.breed, currentBird.sex.name.lowercase().replaceFirstChar { it.uppercase() }),
                    subtitle = "${currentBird.species.name} • ${currentBird.status.replaceFirstChar { it.uppercase() }}",
                    onEditClick = { galleryLauncher.launch("image/*") },
                    onRemoveClick = { viewModel.removeProfilePhoto() }
                )

                val summary by viewModel.financialSummary.collectAsState()
                if (summary != null) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        com.example.hatchtracker.core.ui.components.FinancialSummaryCard(
                            summary = summary!!,
                            currencyCode = "EUR",
                            onAddCostClick = { },
                            onAddRevenueClick = { }
                        )
                    }
                }

                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    InfoCard(title = stringResource(UiR.string.general_information)) {
                        InfoLine(label = stringResource(UiR.string.species_label), value = currentBird.species.name)
                        InfoLine(label = stringResource(UiR.string.breed_label), value = currentBird.breed)
                        InfoLine(label = stringResource(UiR.string.sex_label), value = currentBird.sex.name.lowercase().replaceFirstChar { it.uppercase() })
                        InfoLine(label = stringResource(UiR.string.status_label), value = currentBird.status.replaceFirstChar { it.uppercase() })
                        InfoLine(label = stringResource(UiR.string.hatch_date_label), value = if (currentBird.hatchDate.isNotBlank()) viewModel.localeFormatService.formatDate(currentBird.hatchDate, dateFormat) else stringResource(UiR.string.unknown_label))
                        currentBird.ringNumber?.let { InfoLine(label = stringResource(UiR.string.ring_number_label), value = it) }
                    }
                }

                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    InfoCard(title = stringResource(UiR.string.genetics_label), containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(UiR.string.confidence_level_label), style = MaterialTheme.typography.bodySmall)
                            Badge(
                                containerColor = when (currentBird.geneticProfile.confidenceLevelEnum) {
                                    ConfidenceLevel.HIGH -> MaterialTheme.colorScheme.primary
                                    ConfidenceLevel.MEDIUM -> MaterialTheme.colorScheme.secondary
                                    ConfidenceLevel.LOW -> MaterialTheme.colorScheme.error
                                    ConfidenceLevel.FIXED -> MaterialTheme.colorScheme.tertiary
                                }
                            ) {
                                Text(
                                    currentBird.geneticProfile.confidenceLevel,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(UiR.string.fixed_traits_label), style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                        if (currentBird.geneticProfile.fixedTraits.isEmpty()) {
                            Text(stringResource(UiR.string.no_verified_traits), style = MaterialTheme.typography.bodySmall)
                        } else {
                            currentBird.geneticProfile.fixedTraits.forEach { Text("\u2022 $it", style = MaterialTheme.typography.bodyMedium) }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(UiR.string.inferred_traits_label), style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                        if (currentBird.geneticProfile.inferredTraits.isEmpty()) {
                            Text(stringResource(UiR.string.no_inferred_traits), style = MaterialTheme.typography.bodySmall)
                        } else {
                            currentBird.geneticProfile.inferredTraits.forEach { trait ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("\u2022 $trait", style = MaterialTheme.typography.bodyMedium)
                                    if (isAdmin) {
                                        TextButton(
                                            onClick = { scope.launch { viewModel.promoteTrait(currentBird, trait, userProfile?.uid ?: "ADMIN") } },
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text(stringResource(UiR.string.promote_action), style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }

                        // Mixed/Other traits
                        currentBird.customGeneticProfile?.let { profile ->
                            if (profile.traitValues.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(stringResource(UiR.string.physical_traits_label), style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                                profile.traitValues.forEach { (traitId, optionId) ->
                                    val trait = TraitDisplayCatalog.traits.find { it.id == traitId }
                                    val option = trait?.options?.find { it.id == optionId }
                                    if (trait != null && option != null) {
                                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(trait.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(option.label, style = MaterialTheme.typography.bodyMedium)
                                            }
                                            option.colorHex?.let {
                                                Box(modifier = Modifier.size(16.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(android.graphics.Color.parseColor(it))).border(1.dp, MaterialTheme.colorScheme.outline, androidx.compose.foundation.shape.CircleShape))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Overrides
                        val overrides = currentBird.customGeneticProfile?.traitOverrides ?: emptyList()
                        if (overrides.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(UiR.string.genetic_overrides_label), style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                                SuggestionChip(
                                    onClick = { if (hasPro) showOverridePicker = true else viewModel.setError("PRO_LEVEL_REQUIRED_OVERRIDES") },
                                    label = { Text(stringResource(UiR.string.edit_action), style = MaterialTheme.typography.labelSmall) },
                                    icon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                            overrides.forEach { override ->
                                val trait = TraitDisplayCatalog.traits.find { it.id == override.traitId }
                                val option = trait?.options?.find { it.id == override.optionId }
                                if (trait != null && option != null) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(trait.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(option.label, style = MaterialTheme.typography.bodyMedium)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                                    Text(stringResource(UiR.string.overridden_badge), modifier = Modifier.padding(horizontal = 4.dp))
                                                }
                                            }
                                        }
                                        option.colorHex?.let {
                                            Box(modifier = Modifier.size(16.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(android.graphics.Color.parseColor(it))))
                                        }
                                    }
                                }
                            }
                        } else if (isAdmin) {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(onClick = { if (hasPro) showOverridePicker = true else viewModel.setError("PRO_LEVEL_REQUIRED_OVERRIDES") }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(UiR.string.add_override_action))
                            }
                        }
                    }
                }

                // Breeding Intelligence Section
                geneticInsight?.let { insight ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        BreedingInsightCardCompact(uiModel = insight)
                    }
                }

                // Bird Timeline Section
                val timelineEvents by viewModel.timelineEvents.collectAsState()
                if (timelineEvents.isNotEmpty()) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        InfoCard(
                            title = "Bird Timeline",
                            trailingAction = {
                                TextButton(onClick = { showLogEventDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Log", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        ) {
                            timelineEvents.forEachIndexed { index, event ->
                                val (icon, color) = when (event.type) {
                                    com.example.hatchtracker.data.DomainEventLogger.HEALTH_RECORDED,
                                    com.example.hatchtracker.data.DomainEventLogger.VACCINATION_APPLIED,
                                    com.example.hatchtracker.data.DomainEventLogger.TREATMENT_APPLIED -> 
                                        Icons.Default.MedicalServices to MaterialTheme.colorScheme.primary
                                    
                                    com.example.hatchtracker.data.DomainEventLogger.BIRD_GRADUATED -> 
                                        Icons.Default.School to Color(0xFF4CAF50) // Material Green
                                    
                                    com.example.hatchtracker.data.DomainEventLogger.BIRD_MOVED -> 
                                        Icons.Default.SwapHoriz to MaterialTheme.colorScheme.secondary
                                    
                                    com.example.hatchtracker.data.DomainEventLogger.BIRD_ADDED ->
                                        Icons.Default.Add to MaterialTheme.colorScheme.tertiary
                                        
                                    com.example.hatchtracker.data.DomainEventLogger.BIRD_REMOVED ->
                                        Icons.Default.Delete to MaterialTheme.colorScheme.error
                                        
                                    else -> Icons.Default.Info to MaterialTheme.colorScheme.outline
                                }

                                TimelineRow(
                                    event = event,
                                    icon = icon,
                                    color = color,
                                    isLast = index == timelineEvents.size - 1,
                                    dateFormat = dateFormat,
                                    localeFormatService = viewModel.localeFormatService
                                )
                            }
                        }
                    }
                }

                if (isAdmin) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        InfoCard(title = stringResource(UiR.string.admin_actions_label), containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)) {
                            Text(stringResource(UiR.string.confidence_benchmarks_tip), style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(onClick = { scope.launch { viewModel.boostConfidence(currentBird, ConfidenceLevel.MEDIUM, userProfile?.uid ?: "ADMIN") } }, modifier = Modifier.weight(1f)) {
                                    Text(stringResource(UiR.string.medium_confidence), style = MaterialTheme.typography.labelSmall)
                                }
                                Button(onClick = { scope.launch { viewModel.boostConfidence(currentBird, ConfidenceLevel.HIGH, userProfile?.uid ?: "ADMIN") } }, modifier = Modifier.weight(1f)) {
                                    Text(stringResource(UiR.string.high_confidence), style = MaterialTheme.typography.labelSmall)
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
fun InfoCard(
    title: String,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface,
    trailingAction: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                trailingAction?.invoke()
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
            content()
        }
    }
}

@Composable
fun TimelineRow(
    event: BirdTimelineEvent,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    isLast: Boolean,
    dateFormat: String,
    localeFormatService: com.example.hatchtracker.common.format.LocaleFormatService
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Timeline Indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = color
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .fillMaxHeight()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                        .padding(vertical = 4.dp)
                )
            }
        }

        // Event Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = localeFormatService.formatDate(event.timestamp.toString(), dateFormat),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            event.description?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
    }
}

@Composable
fun LogHealthEventDialog(
    onDismiss: () -> Unit,
    onLog: (String, String) -> Unit
) {
    var type by remember { mutableStateOf("Health Note") }
    var notes by remember { mutableStateOf("") }
    val types = listOf("Health Note", "Vaccination", "Treatment", "Injury", "General")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Health Event") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Event Type", style = MaterialTheme.typography.labelMedium)
                // Simplified type selection
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t) }
                        )
                    }
                }
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(if (type == "Vaccination") "Vaccine Name" else if (type == "Treatment") "Treatment/Medication" else "Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onLog(type, notes) },
                enabled = notes.isNotBlank()
            ) {
                Text("Log Event")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun BreedingInsightCardCompact(uiModel: com.example.hatchtracker.model.breeding.GeneticInsightUiModel) {
    InfoCard(
        title = "Breeding Intelligence",
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Text(
            text = uiModel.summary,
            style = MaterialTheme.typography.bodyMedium
        )
        uiModel.topWarning?.let { warning ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = warning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
