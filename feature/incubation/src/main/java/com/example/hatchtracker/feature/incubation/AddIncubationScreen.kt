@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.hatchtracker.feature.incubation

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import com.example.hatchtracker.core.ui.R as UiR
import com.example.hatchtracker.data.models.BreedStandard
import com.example.hatchtracker.data.models.Incubation
import com.example.hatchtracker.data.models.Sex
import com.example.hatchtracker.data.models.Species
import com.example.hatchtracker.data.repository.DataRepository
import com.example.hatchtracker.core.ui.R
import com.example.hatchtracker.core.ui.catalog.DefaultCatalogData
import com.example.hatchtracker.domain.breeding.IncubationLogic
import com.example.hatchtracker.notifications.scheduling.NotificationScheduler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel

import androidx.compose.ui.graphics.Color

@Composable
fun AddIncubationScreen(
    modifier: Modifier = Modifier,
    onIncubationSaved: (Incubation) -> Unit = {},
    onBackClick: () -> Unit = {},
    onNavigateToBreedSelection: (String) -> Unit = {},
    flockId: Long? = null,
    lockedSpecies: String? = null,
    viewModel: AddIncubationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val birdList by viewModel.birds.collectAsState(initial = emptyList())
    val dateFormat by viewModel.dateFormat.collectAsState()
    
    var selectedBirdId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedBirdDisplay by rememberSaveable { mutableStateOf("") }
    val availableSpecies = DataRepository.allSpecies
    val catalogSpecies = DefaultCatalogData.allSpecies
    val initialSpeciesName = lockedSpecies ?: ""
    
    // Species State (Persisted)
    var species by rememberSaveable { mutableStateOf(initialSpeciesName) }
    
    // Derived State (No need to persist, derived from species)
    val selectedSpecies = remember(species) { 
        availableSpecies.find { it.name.equals(species, ignoreCase = true) } 
    }

    fun updateSpeciesSelection(name: String) {
        species = name
    }

    var startDate by rememberSaveable { mutableStateOf("") }
    var expectedHatch by rememberSaveable { mutableStateOf("") }
    var eggsCount by rememberSaveable { mutableStateOf("") }
    val selectedBreed by viewModel.selectedBreed.collectAsState()

    var sourceOption by rememberSaveable { mutableStateOf("parents") }
    var selectedFlockId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedFatherId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedFatherDisplay by rememberSaveable { mutableStateOf("") }
    var selectedIncubatorId by rememberSaveable { mutableStateOf<String?>(null) }

    // Inheritance from Flock
    if (flockId != null) {
        val flock by viewModel.getFlockById(flockId).collectAsState(initial = null)
        LaunchedEffect(flock) {
            flock?.let {
                updateSpeciesSelection(it.species.name)
                if (it.breeds.isNotEmpty() && it.breeds.first().isNotBlank() && it.breeds.first() != context.getString(UiR.string.mixed_unknown_breed)) {
                    viewModel.setBreedName(it.breeds.first())
                } else {
                    viewModel.clearSelectedBreed()
                }
            }
        }
    }
    
    // Auto-calculate expected hatch date
    LaunchedEffect(species, startDate) {
        if (species.isNotBlank() && startDate.isNotBlank()) {
            val calculatedDate = IncubationLogic.calculateExpectedHatchDate(startDate, species)
            if (calculatedDate.isNotBlank()) {
                expectedHatch = calculatedDate
            }
        }
    }
    
    // Dropdown state
    var expanded by remember { mutableStateOf(false) }
    
    // Validation errors
    var birdError by remember { mutableStateOf<String?>(null) }
    var speciesError by remember { mutableStateOf<String?>(null) }
    var startDateError by remember { mutableStateOf<String?>(null) }
    var expectedHatchError by remember { mutableStateOf<String?>(null) }
    var eggsCountError by remember { mutableStateOf<String?>(null) }
    
    var showSuccessMessage by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Snackbar for locked species messages
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    
    // Get gated species list
    val gatedSpecies = remember { viewModel.getSpeciesUiRows() }

    Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            stringResource(UiR.string.add_incubation_title), 
                            style = MaterialTheme.typography.titleLarge
                        ) 
                    },
                    windowInsets = WindowInsets.statusBars
                )
            },
            snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) }
        ) { paddingValues: PaddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .testTag("AddIncubationScreen")
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            val availableIncubators by viewModel.userIncubators.collectAsState()

            // Device Selection
            if (availableIncubators.isNotEmpty()) {
                var deviceExpanded by remember { mutableStateOf(false) }
                val selectedDevice =
                    availableIncubators.find { it.device.id == selectedIncubatorId }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.5f
                        )
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(UiR.string.incubator_device_label),
                            style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        ExposedDropdownMenuBox(
                            expanded = deviceExpanded,
                            onExpandedChange = { deviceExpanded = !deviceExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedDevice?.let { 
                                    stringResource(UiR.string.device_slots_free_format, it.device.displayName, it.remainingCapacity)
                                } ?: stringResource(UiR.string.select_incubator_optional),
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = deviceExpanded
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                isError = selectedDevice != null && eggsCount.toIntOrNull()
                                    ?.let { it > selectedDevice.remainingCapacity } == true,
                                supportingText = if (selectedDevice != null && eggsCount.toIntOrNull()
                                        ?.let { it > selectedDevice.remainingCapacity } == true
                                ) {
                                    {
                                        Text(
                                            stringResource(UiR.string.capacity_warning_msg),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                } else null
                            )
                            ExposedDropdownMenu(
                                expanded = deviceExpanded,
                                onDismissRequest = { deviceExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(UiR.string.none_option)) },
                                    onClick = {
                                        selectedIncubatorId = null
                                        deviceExpanded = false
                                    }
                                )
                                availableIncubators.forEach { capacity ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(capacity.device.displayName)
                                                    Text(
                                                        stringResource(UiR.string.device_capacity_summary_format, capacity.usedCapacity, capacity.totalCapacity),
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                            }
                                        },
                                        onClick = {
                                            selectedIncubatorId = capacity.device.id
                                            deviceExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (flockId != null) {
                // Flock Mode
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val speciesInfo =
                            catalogSpecies.find { it.name.equals(species, ignoreCase = true) }
                        Icon(
                            painter = painterResource(
                                id = speciesInfo?.imageResId ?: R.drawable.species_chicken
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(stringResource(UiR.string.flock_batch_label), style = MaterialTheme.typography.labelMedium)
                            Text(
                                stringResource(UiR.string.incubating_from_flock_msg),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            } else {
                // Individual Mode: Select Parents or Select Flock
                val canLink = viewModel.canUseParentLinking()
                val availableFlocks by viewModel.activeFlocks.collectAsState(initial = emptyList())

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    // Toggle Source
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = sourceOption == "parents",
                            onClick = {
                                sourceOption = "parents"
                                updateSpeciesSelection("")
                                viewModel.clearSelectedBreed()
                            },
                            label = { Text(stringResource(UiR.string.from_parents_label)) },
                            leadingIcon = { Icon(Icons.Default.Close, null) }
                        )
                        FilterChip(
                            selected = sourceOption == "flock",
                            onClick = {
                                sourceOption = "flock"
                                updateSpeciesSelection("")
                                viewModel.clearSelectedBreed()
                            },
                            label = { Text(stringResource(UiR.string.from_flock_label)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, null) }
                        )
                    }

                    if (sourceOption == "flock") {
                        // Flock Selection Dropdown
                        var flockExpanded by remember { mutableStateOf(false) }
                        var selectedFlockName by remember { mutableStateOf("") }

                        ExposedDropdownMenuBox(
                            expanded = flockExpanded,
                            onExpandedChange = { flockExpanded = !flockExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedFlockName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(UiR.string.select_flock_label)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = flockExpanded) },
                                modifier = Modifier.fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = flockExpanded,
                                onDismissRequest = { flockExpanded = false }
                            ) {
                                if (availableFlocks.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(UiR.string.no_active_flocks_found)) },
                                        onClick = { flockExpanded = false })
                                } else {
                                    availableFlocks.forEach { flock ->
                                        DropdownMenuItem(
                                            text = { Text("${flock.name} (${flock.species})") },
                                            onClick = {
                                                selectedFlockName = flock.name
                                                selectedFlockId = flock.id

                                                // Update species from flock
                                                updateSpeciesSelection(flock.species.name)
                                                
                                                if (flock.breeds.isNotEmpty() && flock.breeds.first().isNotBlank() && flock.breeds.first() != context.getString(UiR.string.mixed_unknown_breed)) {
                                                    viewModel.setBreedName(flock.breeds.first())
                                                } else {
                                                    viewModel.clearSelectedBreed()
                                                }

                                                flockExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Parents Selection
                        var showMotherDialog by remember { mutableStateOf(false) }
                        var showFatherDialog by remember { mutableStateOf(false) }
                        var showPaywallOnLinking by remember { mutableStateOf(false) }

                        // Mother Field
                        Box {
                            OutlinedTextField(
                                value = selectedBirdDisplay,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(UiR.string.mother_optional_label)) },
                                placeholder = { Text(stringResource(UiR.string.click_to_select_mother)) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Box(
                                modifier = Modifier.matchParentSize().clickable {
                                    if (canLink) showMotherDialog = true else showPaywallOnLinking =
                                        true
                                })
                        }

                        // Father Field
                        Box {
                            OutlinedTextField(
                                value = selectedFatherDisplay,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(UiR.string.father_optional_label)) },
                                placeholder = { Text(stringResource(UiR.string.click_to_select_father)) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Box(
                                modifier = Modifier.matchParentSize().clickable {
                                    if (canLink) showFatherDialog = true else showPaywallOnLinking =
                                        true
                                })
                        }

                        if (showMotherDialog) {
                            com.example.hatchtracker.core.ui.components.BirdSelectionDialog(
                                birds = birdList,
                                onDismiss = { showMotherDialog = false },
                                onBirdSelected = { bird ->
                                    selectedBirdId = bird.id
                                    selectedBirdDisplay = "${bird.species} - ${bird.breed}"
                                    if (species.isBlank()) {
                                        updateSpeciesSelection(bird.species.name)
                                        viewModel.clearSelectedBreed()
                                    }
                                    showMotherDialog = false
                                },
                                filterSex = Sex.FEMALE,
                                filterSpecies = species.ifBlank { null }
                            )
                        }

                        if (showFatherDialog) {
                            com.example.hatchtracker.core.ui.components.BirdSelectionDialog(
                                birds = birdList,
                                onDismiss = { showFatherDialog = false },
                                onBirdSelected = { bird ->
                                    selectedFatherId = bird.id
                                    selectedFatherDisplay = "${bird.species} - ${bird.breed}"
                                    if (species.isBlank()) {
                                        updateSpeciesSelection(bird.species.name)
                                        viewModel.clearSelectedBreed()
                                    }
                                    showFatherDialog = false
                                },
                                filterSex = Sex.MALE,
                                filterSpecies = species.ifBlank { null }
                            )
                        }
                    }
                }
            }

            // Species field
            var speciesExpanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = speciesExpanded,
                onExpandedChange = { speciesExpanded = !speciesExpanded }
            ) {
                OutlinedTextField(
                    value = selectedSpecies?.name ?: species,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(stringResource(UiR.string.species_required_label)) },
                    leadingIcon = {
                        val speciesInfo = catalogSpecies.find {
                            it.name.equals(
                                selectedSpecies?.name ?: species,
                                ignoreCase = true
                            )
                        }
                        speciesInfo?.imageResId?.let { resId ->
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = speciesExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("IncubationSpeciesSelector")
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                        .clickable(enabled = true) {
                            speciesExpanded = true
                        },
                    singleLine = true,
                    placeholder = { Text(stringResource(UiR.string.select_species_placeholder)) },
                    isError = speciesError != null,
                    supportingText = speciesError?.let { errorText -> { Text(errorText) } },
                    enabled = true,
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = speciesExpanded,
                    onDismissRequest = { speciesExpanded = false }
                ) {
                    gatedSpecies.forEach { speciesRow ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Species icon
                                    val catalogSpecies = catalogSpecies.find { 
                                        it.id.equals(speciesRow.speciesId, ignoreCase = true) 
                                    }
                                    catalogSpecies?.imageResId?.let { resId ->
                                        Image(
                                            painter = painterResource(id = resId),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .padding(end = 8.dp),
                                            alpha = if (speciesRow.isLocked) 0.5f else 1f
                                        )
                                    }
                                    
                                    // Species name
                                    Text(
                                        text = speciesRow.displayName,
                                        modifier = Modifier.weight(1f),
                                        color = if (speciesRow.isLocked) 
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        else 
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                    
                                    // Star icon for locked species
                                    if (speciesRow.isLocked) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = stringResource(UiR.string.locked_label),
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                if (speciesRow.isLocked) {
                                    // Show snackbar for locked species
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = com.example.hatchtracker.domain.util.SpeciesGatingHelper.getUpgradeMessage(speciesRow.displayName),
                                            duration = androidx.compose.material3.SnackbarDuration.Short
                                        )
                                    }
                                    speciesExpanded = false
                                } else {
                                    updateSpeciesSelection(speciesRow.displayName)
                                    speciesExpanded = false
                                }
                            }
                        )
                    }
                }
        }

        // Breed Selection (Single - Unified Flow)
        if (species.isNotBlank()) {
            if (flockId != null || sourceOption == "flock") {
               // In flock mode, we might implicitly use flock's breed or mixed?
               // For now, let's allow explicit selection but maybe pre-fill if we had logic (we don't right now)
               // Or clearer: if from flock, maybe we don't select breed manually?
               // Requirement says "Species selection stays... Breed selection happens by navigating...".
               // If source is flock, usually breeds are inherited. 
               // Let's keep it simple: Determine if we show the selector.
            }
            
            Box {
                OutlinedTextField(
                    value = selectedBreed ?: stringResource(UiR.string.mixed_unknown_breed),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(UiR.string.breed_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false, // We use the button/click area
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    trailingIcon = {
                         TextButton(onClick = { 
                             val speciesRow = gatedSpecies.find { it.displayName.equals(species, ignoreCase = true) }
                             if (speciesRow != null && speciesRow.isLocked) {
                                 scope.launch {
                                     snackbarHostState.showSnackbar(
                                         message = com.example.hatchtracker.domain.util.SpeciesGatingHelper.getUpgradeMessage(species),
                                         duration = androidx.compose.material3.SnackbarDuration.Short
                                     )
                                 }
                             } else {
                                 onNavigateToBreedSelection(selectedSpecies?.id ?: species)
                             }
                         }) {
                             Text(stringResource(UiR.string.change_action))
                         }
                    }
                )
                Box(modifier = Modifier.matchParentSize().clickable { 
                    val speciesRow = gatedSpecies.find { it.displayName.equals(species, ignoreCase = true) }
                    if (speciesRow != null && speciesRow.isLocked) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = com.example.hatchtracker.domain.util.SpeciesGatingHelper.getUpgradeMessage(species),
                                duration = androidx.compose.material3.SnackbarDuration.Short
                            )
                        }
                    } else {
                        onNavigateToBreedSelection(selectedSpecies?.id ?: species)
                    }
                })
            }
            
        } else {
             Text(
                stringResource(UiR.string.select_species_first_breed_msg),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Date Picker Logic
        val calendar = remember { java.util.Calendar.getInstance() }
        val dateParams = remember {
            Triple(
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH),
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
            )
        }

        // Start Date Picker
        val startDatePickerDialog = android.app.DatePickerDialog(
            LocalContext.current,
            { _, year, month, dayOfMonth ->
                startDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                startDateError = null
            },
            dateParams.first, dateParams.second, dateParams.third
        )

        // Start Date field (Read Only, Clickable)
        Box {
            OutlinedTextField(
                value = if (startDate.isNotBlank()) viewModel.localeFormatService.formatDate(startDate, dateFormat) else "",
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(UiR.string.start_date_required_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(stringResource(UiR.string.select_date_placeholder)) },
                isError = startDateError != null,
                supportingText = startDateError?.let { errorText -> { Text(errorText) } },
                enabled = false, // Disable typing
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = stringResource(UiR.string.select_date_content_description)) }
            )
            // Clickable Box overlay
            Box(modifier = Modifier.matchParentSize().testTag("StartDatePicker").clickable { startDatePickerDialog.show() })
        }

        // Expected Hatch Date Picker
        val hatchDatePickerDialog = android.app.DatePickerDialog(
            LocalContext.current,
            { _, year, month, dayOfMonth ->
                expectedHatch = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                expectedHatchError = null
            },
            dateParams.first, dateParams.second, dateParams.third
        )

        // Expected Hatch Date field (Read Only, Clickable)
        Box {
            OutlinedTextField(
                value = if (expectedHatch.isNotBlank()) viewModel.localeFormatService.formatDate(expectedHatch, dateFormat) else "",
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(UiR.string.expected_hatch_date_required_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(stringResource(UiR.string.select_date_placeholder)) },
                isError = expectedHatchError != null,
                supportingText = expectedHatchError?.let { errorText -> { Text(errorText) } },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = stringResource(UiR.string.select_date_content_description)) }
            )
            Box(modifier = Modifier.matchParentSize().clickable { hatchDatePickerDialog.show() })
        }

        // Eggs Count field
        OutlinedTextField(
            value = eggsCount,
            onValueChange = {
                if (it.all { char -> char.isDigit() }) {
                    eggsCount = it
                    eggsCountError = null
                }
            },
            label = { Text(stringResource(UiR.string.eggs_count_required_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            placeholder = { Text(stringResource(UiR.string.number_of_eggs_placeholder)) },
            isError = eggsCountError != null,
            supportingText = eggsCountError?.let { errorText -> { Text(errorText) } }
        )

        // Save button
        var showPaywall by remember { mutableStateOf(false) }

        Button(
            onClick = {
                scope.launch {
                    val activeCount = viewModel.getActiveIncubationCount()

                    if (!viewModel.canCreateMoreIncubations(activeCount)) {
                        showPaywall = true
                        return@launch
                    }

                    // Validate
                    var isValid = true
                    if (species.isBlank()) {
                        speciesError = context.getString(UiR.string.species_required_error)
                        isValid = false
                    }
                    if (startDate.isBlank()) {
                        startDateError = context.getString(UiR.string.start_date_required_error)
                        isValid = false
                    }
                    if (expectedHatch.isBlank()) {
                        expectedHatchError = context.getString(UiR.string.expected_hatch_required_error)
                        isValid = false
                    }
                    if (eggsCount.isBlank()) {
                        eggsCountError = context.getString(UiR.string.eggs_count_required_error)
                        isValid = false
                    } else {
                        val count = eggsCount.toIntOrNull()
                        if (count == null || count <= 0) {
                            eggsCountError = context.getString(UiR.string.eggs_count_positive_error)
                            isValid = false
                        }
                    }

                    if (isValid) {
                        // Check species gating
                        val speciesRow = gatedSpecies.find { it.displayName.equals(species, ignoreCase = true) }
                        if (speciesRow != null && speciesRow.isLocked) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = com.example.hatchtracker.domain.util.SpeciesGatingHelper.getUpgradeMessage(species),
                                    duration = androidx.compose.material3.SnackbarDuration.Long
                                )
                            }
                            return@launch
                        }
                        
                        val finalFlockId =
                            flockId ?: if (sourceOption == "flock") selectedFlockId else null
                        val finalBirdId = if (sourceOption == "parents") selectedBirdId else null
                        val finalFatherId =
                            if (sourceOption == "parents") selectedFatherId else null

                        val newIncubation = Incubation(
                            id = 0,
                            flockId = finalFlockId,
                            birdId = finalBirdId,
                            fatherBirdId = finalFatherId,
                            species = species.trim(),
                            breeds = emptyList(), // ViewModel handles this now
                            startDate = startDate.trim(),
                            expectedHatch = expectedHatch.trim(),
                            eggsCount = eggsCount.toInt(),
                            incubatorDeviceId = selectedIncubatorId ?: "",
                            hatchedCount = 0
                        )


                        viewModel.saveIncubation(newIncubation) { savedIncubation ->
                            NotificationScheduler.scheduleIncubation(context, savedIncubation)
                            onIncubationSaved(savedIncubation)
                            onBackClick() // Navigate back after success
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("SaveIncubationButton")
        ) {
            Text(stringResource(UiR.string.save_incubation_action))
        }
        }
}
}
