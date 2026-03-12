// LAYOUT COMPOSITION CONTRACT
// This screen conforms strictly to the pre-polish layout baseline.
// Do NOT alter container structure or component layout without explicit design approval.
package com.example.hatchtracker.feature.flock.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.core.ui.catalog.DefaultCatalogData
import com.example.hatchtracker.data.models.Flock
import kotlinx.coroutines.launch

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.hatchtracker.core.ui.util.ImageUtils
import java.io.File

import com.example.hatchtracker.feature.flock.ui.viewmodels.FlockViewModel
import com.example.hatchtracker.core.ui.components.GeneticTraitPicker
import com.example.hatchtracker.data.models.GeneticProfile
import com.example.hatchtracker.model.Species
import com.example.hatchtracker.core.ui.R as UiR

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddFlockScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onFlockSaved: () -> Unit,
    prefilledSpecies: String? = null,
    prefilledBreeds: List<String>? = null,
    graduatingFlockletId: Long? = null,
    onNavigateToBreedSelection: (String) -> Unit = {},
    selectedBreedFromResult: String? = null,
    onClearBreedResult: () -> Unit = {},
    viewModel: FlockViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    var name by rememberSaveable { mutableStateOf("") }
    var species by rememberSaveable { mutableStateOf(prefilledSpecies ?: "") }
    var purpose by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var imagePath by rememberSaveable { mutableStateOf<String?>(null) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    var tempPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoPath != null) {
            val oldPath = imagePath
            scope.launch {
                viewModel.saveProfilePhoto(Uri.fromFile(File(tempPhotoPath!!)))
                    .onSuccess { newPath ->
                        imagePath = newPath
                        if (oldPath != null) viewModel.deleteOldPhoto(oldPath)
                    }
                    .onFailure {
                        snackbarHostState.showSnackbar("Failed to save photo")
                    }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val oldPath = imagePath
            scope.launch {
                viewModel.saveProfilePhoto(it)
                    .onSuccess { newPath ->
                        imagePath = newPath
                        if (oldPath != null) viewModel.deleteOldPhoto(oldPath)
                    }
                    .onFailure {
                        snackbarHostState.showSnackbar("Failed to save photo")
                    }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingCameraUri?.let { uri -> cameraLauncher.launch(uri) }
        } else {
            pendingCameraUri = null
        }
    }
    
    // UI States
    var speciesExpanded by remember { mutableStateOf(false) }
    var purposeExpanded by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var speciesError by remember { mutableStateOf<String?>(null) }
    var purposeError by remember { mutableStateOf<String?>(null) }
    
    // Breed Selection States
    var availableBreeds by remember { mutableStateOf<List<com.example.hatchtracker.data.models.BreedStandard>>(emptyList()) }
    var breedExpanded by remember { mutableStateOf(false) }
    var selectedBreeds by rememberSaveable { mutableStateOf(prefilledBreeds ?: emptyList()) }
    var geneticTraits by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val isMixed = selectedBreeds.any {
        it.equals("Mixed", ignoreCase = true) ||
            it.equals("Mixed/Other", ignoreCase = true) ||
            it.equals("Other", ignoreCase = true)
    }
    // Get gated species list
    val gatedSpecies by viewModel.speciesUiRows.collectAsState()

    // Update breed when selectedBreedFromResult changes (from navigation result)
    LaunchedEffect(selectedBreedFromResult) {
        selectedBreedFromResult?.let { breedName ->
            if (breedName !in selectedBreeds) {
                selectedBreeds = selectedBreeds + breedName
            }
            onClearBreedResult()
        }
    }


    // Fetch breeds when species changes
    LaunchedEffect(species) {
        if (species.isNotBlank()) {
            availableBreeds = viewModel.getBreedsForSpecies(species)
        } else {
            availableBreeds = emptyList()
        }
    }

    val purposeOptions = listOf(
        "eggs" to stringResource(UiR.string.flock_purpose_eggs),
        "breeding" to stringResource(UiR.string.flock_purpose_breeding),
        "meat" to stringResource(UiR.string.flock_purpose_meat),
        "exhibition" to stringResource(UiR.string.flock_purpose_exhibition),
        "mixed" to stringResource(UiR.string.flock_purpose_mixed)
    )
    val selectedPurposeLabel = purposeOptions.firstOrNull { it.first == purpose }?.second ?: ""
    val nameRequiredError = stringResource(UiR.string.error_name_required)
    val speciesRequiredError = stringResource(UiR.string.species_required_error)
    val purposeRequiredError = stringResource(UiR.string.flock_purpose_required_error)
    val createAndGraduateLabel = stringResource(UiR.string.flock_create_and_graduate)
    val createFlockLabel = stringResource(UiR.string.flock_create_action)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(UiR.string.add_new_flock_action),
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(UiR.string.back_action))
                    }
                },
                windowInsets = WindowInsets.statusBars
            )
        },
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .testTag("AddFlockScreen")
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

        // Name
        OutlinedTextField(
            value = name,
            onValueChange = { 
                name = it
                nameError = null
            },
            label = { Text(stringResource(UiR.string.flock_name_required_label)) },
            placeholder = { Text(stringResource(UiR.string.flock_name_placeholder)) },
            modifier = Modifier.fillMaxWidth().testTag("FlockNameInput"),
            isError = nameError != null,
            supportingText = nameError?.let { { Text(it) } }
        )

        // Species
        ExposedDropdownMenuBox(
            expanded = speciesExpanded,
            onExpandedChange = { speciesExpanded = !speciesExpanded },
            modifier = Modifier.fillMaxWidth().testTag("SpeciesSelector")
        ) {
            OutlinedTextField(
                value = species,
                onValueChange = {},
                readOnly = true,
                enabled = true,
                label = { Text(stringResource(UiR.string.species_required_label)) },
                trailingIcon = { 
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = speciesExpanded) 
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                isError = speciesError != null,
                supportingText = speciesError?.let { errorText -> { Text(errorText) } }
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
                                val catalogSpecies = DefaultCatalogData.allSpecies.find { 
                                    it.id.equals(speciesRow.speciesId, ignoreCase = true) 
                                }
                                catalogSpecies?.imageResId?.let { resId ->
                                    Image(
                                        painter = painterResource(id = resId),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(32.dp)
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
                                
                                // Star icon for locked/premium species
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
                                species = speciesRow.displayName
                                speciesExpanded = false
                                speciesError = null
                            }
                        }
                    )
                }
            }
        }
        
        if (species.isNotBlank()) {
             Text(stringResource(UiR.string.breeds_label), style = MaterialTheme.typography.titleMedium)
             
             // Selected Breeds Chips
             if (selectedBreeds.isNotEmpty()) {
                 FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                             selectedBreeds.forEach { breed ->
                                 InputChip(
                                     selected = true,
                                     onClick = { selectedBreeds = selectedBreeds - breed },
                                     label = { Text(breed) },
                                     trailingIcon = { Icon(Icons.Default.Close, contentDescription = stringResource(UiR.string.remove_action)) }
                                 )
                             }
                         }
             }

             // Add Breed Button - Navigates to standardized selection screen
             Box(modifier = Modifier.testTag("BreedSelector")) {
                 Row(verticalAlignment = Alignment.CenterVertically) {
                     OutlinedButton(
                         onClick = { 
                             val speciesRow = gatedSpecies.find { it.displayName.equals(species, ignoreCase = true) }
                             if (speciesRow != null) {
                                 if (speciesRow.isLocked) {
                                     scope.launch {
                                         snackbarHostState.showSnackbar(
                                             message = com.example.hatchtracker.domain.util.SpeciesGatingHelper.getUpgradeMessage(species),
                                             duration = androidx.compose.material3.SnackbarDuration.Short
                                         )
                                     }
                                 } else {
                                     onNavigateToBreedSelection(speciesRow.speciesId)
                                 }
                             }
                         },
                         modifier = Modifier.weight(1f)
                     ) {
                         Icon(Icons.Default.Add, contentDescription = null)
                         Spacer(Modifier.width(8.dp))
                         Text(stringResource(UiR.string.add_breed_action))
                     }
                     
                     var showBreedInfo by remember { mutableStateOf(false) }
                     IconButton(onClick = { showBreedInfo = true }) {
                         Icon(
                             imageVector = Icons.Default.Info,
                             contentDescription = stringResource(UiR.string.breed_info_content_description),
                             tint = MaterialTheme.colorScheme.primary
                         )
                     }
                     
                     if (showBreedInfo) {
                         AlertDialog(
                             onDismissRequest = { showBreedInfo = false },
                             title = { Text(stringResource(UiR.string.breed_inheritance_title)) },
                             text = { 
                                 Text(stringResource(UiR.string.breed_inheritance_message))
                             },
                             confirmButton = {
                                 TextButton(onClick = { showBreedInfo = false }) {
                                     Text(stringResource(UiR.string.got_it_action))
                                 }
                             }
                         )
                     }
                 }
             }
        }

        // Purpose
        ExposedDropdownMenuBox(
            expanded = purposeExpanded,
            onExpandedChange = { purposeExpanded = !purposeExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedPurposeLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(UiR.string.flock_purpose_required_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = purposeExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                isError = purposeError != null,
                supportingText = purposeError?.let { { Text(it) } }
            )
            ExposedDropdownMenu(
                expanded = purposeExpanded,
                onDismissRequest = { purposeExpanded = false }
            ) {
                purposeOptions.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.second) },
                        onClick = {
                            purpose = item.first
                            purposeExpanded = false
                            purposeError = null
                        }
                    )
                }
            }
        }

        // Notes
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(stringResource(UiR.string.notes_optional_label)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5
        )

        // Image Selection
        Text(stringResource(UiR.string.flock_picture_label), style = MaterialTheme.typography.titleMedium)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (imagePath != null) {
                    AsyncImage(
                        model = imagePath,
                        contentDescription = stringResource(UiR.string.flock_image_content_description),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { imagePath = null },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(UiR.string.remove_image_content_description), tint = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(UiR.string.no_picture_added))
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val file = ImageUtils.createImageFile(context)
                    tempPhotoPath = file.absolutePath
                    val uri = ImageUtils.getUriForFile(context, file)
                    pendingCameraUri = uri
                    if (
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        cameraLauncher.launch(uri)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(UiR.string.take_photo_action))
            }
            OutlinedButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(UiR.string.gallery_action))
            }
        }
        
        // Mixed Breed Genetic Traits Section
        if (isMixed) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(UiR.string.genetic_traits_mixed_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        stringResource(UiR.string.genetic_traits_mixed_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    GeneticTraitPicker(
                        selectedTraits = geneticTraits,
                        onTraitChanged = { traitId, optionId ->
                            geneticTraits = if (optionId == null) {
                                geneticTraits - traitId
                            } else {
                                geneticTraits + (traitId to optionId)
                            }
                        }
                    )
                    
                    if (geneticTraits.isEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(UiR.string.genetic_traits_empty_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Save Button
        Button(
            onClick = {
                var isValid = true
                if (name.isBlank()) {
                    nameError = nameRequiredError
                    isValid = false
                }
                if (species.isBlank()) {
                    speciesError = speciesRequiredError
                    isValid = false
                }
                if (purpose.isBlank()) {
                    purposeError = purposeRequiredError
                    isValid = false
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
                    } else {
                        val flock = Flock(
                            localId = 0,
                            name = name.trim(),
                            species = parseSpecies(species),
                            breeds = selectedBreeds,
                            purpose = purpose,
                            notes = notes.takeIf { it.isNotBlank() },
                            active = true,
                            createdAt = System.currentTimeMillis(),
                            eggCount = 0,
                            syncId = java.util.UUID.randomUUID().toString(),
                            lastUpdated = System.currentTimeMillis(),
                            imagePath = imagePath,
                            defaultGeneticProfile = if (isMixed && geneticTraits.isNotEmpty()) {
                                GeneticProfile(traitValues = geneticTraits)
                            } else null
                        )
                        
                        if (graduatingFlockletId != null) {
                            viewModel.addFlockAndGraduate(flock, graduatingFlockletId)
                        } else {
                            viewModel.addFlock(flock)
                        }
                        onFlockSaved()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("SaveFlockButton")
        ) {
            Text(if (graduatingFlockletId != null) createAndGraduateLabel else createFlockLabel)
        }
    }
}
}

private fun parseSpecies(value: String): Species {
    return runCatching { Species.valueOf(value.trim().uppercase()) }
        .getOrDefault(Species.UNKNOWN)
}

