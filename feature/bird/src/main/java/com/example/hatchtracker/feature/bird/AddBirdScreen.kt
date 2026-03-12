package com.example.hatchtracker.feature.bird

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.data.models.Bird
import com.example.hatchtracker.core.ui.R
import com.example.hatchtracker.core.ui.catalog.DefaultCatalogData
import com.example.hatchtracker.data.models.Sex
import kotlinx.coroutines.delay
import com.example.hatchtracker.core.ui.components.GeneticTraitPicker
import com.example.hatchtracker.data.models.GeneticProfile
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.hatchtracker.core.ui.util.ImageUtils
import java.io.File
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBirdScreen(
    modifier: Modifier = Modifier,
    onBirdSaved: (Bird) -> Unit = {},
    onBackClick: () -> Unit = {},
    onNavigateToBreedSelection: (String) -> Unit = {},
    selectedBreed: String? = null,
    onClearBreedResult: () -> Unit = {},
    flockId: Long? = null,
    lockedSpecies: String? = null,
    viewModel: com.example.hatchtracker.feature.bird.AddBirdViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // User Profile for Localization
    val userProfile by viewModel.userProfile.collectAsState()
    
    // Determine Date Format Pattern
    val displayPattern = remember(userProfile) {
        (userProfile?.dateFormat ?: "YYYY-MM-DD").uppercase()
    }
    
    val parsePattern = remember(displayPattern) {
        displayPattern.replace("Y", "y").replace("D", "d")
    }

    // Snackbar for gating messages
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Gated species metadata
    val gatedSpecies = remember(userProfile) {
        viewModel.getSpeciesUiRows()
    }


    val formState by viewModel.formState.collectAsStateWithLifecycle()
    
    var showBulkAddDialog by rememberSaveable { mutableStateOf(false) }
    
    // Scoped editable state for Bulk Add dialog
    var bulkQuantity by rememberSaveable { mutableStateOf("1") }
    var bulkBreed by rememberSaveable { mutableStateOf("") }
    var bulkSex by rememberSaveable { mutableStateOf(Sex.UNKNOWN) }
    var bulkHatchDate by rememberSaveable { mutableStateOf("") }

    // Update breed when selectedBreed changes (from navigation result)
    LaunchedEffect(selectedBreed) {
        selectedBreed?.let {
            if (showBulkAddDialog) {
                bulkBreed = it
            } else {
                viewModel.updateBreed(it)
            }
            onClearBreedResult()
        }
    }

    // Initialize bulkBreed when dialog opens
    LaunchedEffect(showBulkAddDialog) {
        if (showBulkAddDialog && bulkBreed.isEmpty()) {
            bulkBreed = formState.breed
        }
    }

    // Initialize species if locked
    LaunchedEffect(lockedSpecies) {
        lockedSpecies?.let { name ->
            val id = DefaultCatalogData.allSpecies.find { it.name.equals(name, ignoreCase = true) }?.id
            if (id != null && formState.speciesId == null) {
                viewModel.selectSpecies(id, name)
            }
        }
    }

    var tempPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    
    // Mixed Breed Traits
    var geneticTraits by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var useFlockDefaults by rememberSaveable { mutableStateOf(true) }
    
    val flock by viewModel.flock.collectAsState()
    
    // Determine if we should show mixed breed options
    val isMixed = formState.breed.equals("Mixed", ignoreCase = true) || 
                  formState.breed.equals("Mixed/Other", ignoreCase = true) || 
                  formState.breed.equals("Other", ignoreCase = true) ||
                  (flock?.breeds?.any { it.equals("Mixed", ignoreCase = true) || it.equals("Mixed/Other", ignoreCase = true) } == true)

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoPath != null) {
            val oldPath = formState.imagePath
            scope.launch {
                viewModel.saveProfilePhoto(Uri.fromFile(File(tempPhotoPath!!)))
                    .onSuccess { newPath ->
                        viewModel.updateImagePath(newPath)
                        viewModel.deleteOldPhoto(oldPath)
                    }
                    .onFailure {
                        // Handle error (e.g. snackbar)
                        scope.launch { snackbarHostState.showSnackbar("Failed to save photo") }
                    }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val oldPath = formState.imagePath
            scope.launch {
                viewModel.saveProfilePhoto(it)
                    .onSuccess { newPath ->
                        viewModel.updateImagePath(newPath)
                        viewModel.deleteOldPhoto(oldPath)
                    }
                    .onFailure {
                        // Handle error (e.g. snackbar)
                        scope.launch { snackbarHostState.showSnackbar("Failed to save photo") }
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
    
    // Error states
    var speciesError by remember { mutableStateOf<String?>(null) }
    var breedError by remember { mutableStateOf<String?>(null) }
    var sexError by remember { mutableStateOf<String?>(null) }
    var hatchDateError by remember { mutableStateOf<String?>(null) }
    
    // Dropdown expanded states
    var speciesExpanded by remember { mutableStateOf(false) }
    var sexExpanded by remember { mutableStateOf(false) }
    
    // Auto-fill from Flock
    LaunchedEffect(flockId) {
        flockId?.let { viewModel.loadFlock(it) }
    }

        // Auto-fill from Flock
    LaunchedEffect(flock) {
        flock?.let { f ->
            // If flock has a single specific breed (not Mixed), pre-fill it
            if (f.breeds.size == 1 && !f.breeds.first().equals("Mixed", ignoreCase = true)) {
                if (formState.breed.isBlank()) {
                    viewModel.updateBreed(f.breeds.first())
                }
                // Also ensure species matches flock species if not locked/set
                if (formState.species.isBlank() && lockedSpecies == null) {
                    // Start species selection
                     val flockSpeciesId = DefaultCatalogData.allSpecies.find { it.name.equals(f.species.name, ignoreCase = true) }?.id
                     if (flockSpeciesId != null) {
                         viewModel.selectSpecies(flockSpeciesId, f.species.name)
                     }
                }
            }

            // Load default traits from flock if available
             f.defaultGeneticProfile?.let { profile ->
                 if (useFlockDefaults) {
                     geneticTraits = profile.traitValues
                 }
             }
        }
    }
    
    val availableBreeds by viewModel.breeds.collectAsState()
    
    var showSuccessMessage by remember { mutableStateOf(false) }
    if (showBulkAddDialog) {
        BulkAddDialog(
            onDismiss = { showBulkAddDialog = false },
            onAdd = { q, b, s, date ->
                val birds = List(q) {
                    Bird(
                        flockId = flockId,
                        species = formState.species,
                        breed = b,
                        sex = s,
                        hatchDate = date
                    )
                }
                viewModel.saveBirds(birds) {
                    showBulkAddDialog = false
                    onBirdSaved(birds.first()) // Just notify once for the batch
                    showSuccessMessage = true
                }
            },
            initialSpecies = formState.species,
            initialSpeciesId = formState.speciesId ?: "",
            displayPattern = displayPattern,
            parsePattern = parsePattern,
            breed = bulkBreed,
            onBreedClick = { 
                formState.speciesId?.let { id -> onNavigateToBreedSelection(id) }
            },
            onBreedChange = { bulkBreed = it },
            quantity = bulkQuantity,
            onQuantityChange = { bulkQuantity = it },
            sex = bulkSex,
            onSexChange = { bulkSex = it },
            hatchDate = bulkHatchDate,
            onHatchDateChange = { bulkHatchDate = it }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Add New Bird", 
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                actions = {
                    TextButton(onClick = { showBulkAddDialog = true }) {
                        Text(stringResource(R.string.bulk_add_action))
                    }
                },
                windowInsets = WindowInsets.statusBars
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

        // Species field
        ExposedDropdownMenuBox(
            expanded = speciesExpanded && lockedSpecies == null, // Disable expansion if locked
            onExpandedChange = { if (lockedSpecies == null) speciesExpanded = !speciesExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = formState.species,
                onValueChange = { },
                readOnly = true,
                label = { Text(stringResource(R.string.species_required_breed_label)) },
                leadingIcon = {
                    formState.speciesId?.let { id ->
                        DefaultCatalogData.allSpecies.find { it.id == id }?.imageResId?.let { resId ->
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                trailingIcon = { 
                    if (lockedSpecies == null) {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = speciesExpanded)
                    }
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                isError = speciesError != null,
                supportingText = speciesError?.let { { Text(it) } },
                enabled = lockedSpecies == null // Visually indicate it's locked
            )
            
            if (lockedSpecies == null) {
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
                                    DefaultCatalogData.allSpecies.find { it.id.equals(speciesRow.speciesId, ignoreCase = true) }?.imageResId?.let { resId ->
                                        Image(
                                            painter = painterResource(id = resId),
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp).padding(end = 8.dp),
                                            alpha = if (speciesRow.isLocked) 0.5f else 1.0f
                                        )
                                    }
                                    Text(
                                        text = speciesRow.displayName,
                                        modifier = Modifier.weight(1f),
                                        color = if (speciesRow.isLocked) 
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        else 
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                    if (speciesRow.isLocked) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = stringResource(R.string.locked_content_description),
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                if (speciesRow.isLocked) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = com.example.hatchtracker.domain.util.SpeciesGatingHelper.getUpgradeMessage(speciesRow.displayName),
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                    speciesExpanded = false
                                } else {
                                    viewModel.selectSpecies(speciesRow.speciesId, speciesRow.displayName)
                                    speciesExpanded = false
                                    speciesError = null
                                }
                            }
                        )
                    }
                }
            }
        }

        // Breed field - Click to open selection screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = formState.speciesId != null) {
                    formState.speciesId?.let { onNavigateToBreedSelection(it) }
                }
        ) {
            OutlinedTextField(
                value = formState.breed,
                onValueChange = { },
                readOnly = true,
                label = { Text(stringResource(R.string.breed_required_label)) },
                trailingIcon = {
                    androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward.let { icon ->
                        Icon(icon, contentDescription = stringResource(R.string.select_breed_content_description))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                isError = breedError != null,
                supportingText = when {
                    breedError != null -> {
                        { Text(breedError!!) }
                    }
                    formState.speciesId == null -> {
                        { Text(stringResource(R.string.select_species_first_short)) }
                    }
                    else -> null
                },
                enabled = false, // Disable to prevent keyboard, clicks handled by Box
                placeholder = { Text(if (formState.speciesId == null) "Select Species first" else "Tap to select breed") },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        // Ring Number field
        OutlinedTextField(
            value = formState.ringNumber,
            onValueChange = { viewModel.updateRingNumber(it) },
            label = { Text(stringResource(R.string.ring_number_optional_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Sex field
        ExposedDropdownMenuBox(
            expanded = sexExpanded,
            onExpandedChange = { sexExpanded = !sexExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = formState.sex.name.lowercase().replaceFirstChar { it.uppercase() },
                onValueChange = { },
                readOnly = true,
                label = { Text(stringResource(R.string.sex_required_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sexExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                isError = sexError != null,
                supportingText = sexError?.let { { Text(it) } }
            )
            ExposedDropdownMenu(
                expanded = sexExpanded,
                onDismissRequest = { sexExpanded = false }
            ) {
                // Male option with icon
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.male_label)) },
                    leadingIcon = {
                        Image(
                            painter = painterResource(id = R.drawable.male),
                            contentDescription = stringResource(R.string.male_content_description),
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onClick = {
                        viewModel.updateSex(Sex.MALE)
                        sexExpanded = false
                        sexError = null
                    }
                )
                // Female option with icon
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.female_label)) },
                    leadingIcon = {
                        Image(
                            painter = painterResource(id = R.drawable.femenine),
                            contentDescription = stringResource(R.string.female_content_description),
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onClick = {
                        viewModel.updateSex(Sex.FEMALE)
                        sexExpanded = false
                        sexError = null
                    }
                )
                // Unknown option without icon
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.unknown_label)) },
                    onClick = {
                        viewModel.updateSex(Sex.UNKNOWN)
                        sexExpanded = false
                        sexError = null
                    }
                )
            }
        }

        // Hatch Date field
        OutlinedTextField(
            value = formState.hatchDate,
            onValueChange = {
                viewModel.updateHatchDate(it)
                hatchDateError = null
            },
            label = { Text(stringResource(R.string.hatch_date_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(displayPattern) }, // Dynamic placeholder based on user's locale
            isError = hatchDateError != null,
            supportingText = hatchDateError?.let { { Text(it) } }
        )

        // Color field (optional)
        OutlinedTextField(
            value = formState.color,
            onValueChange = { viewModel.updateColor(it) },
            label = { Text(stringResource(R.string.color_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Notes field (optional)
        OutlinedTextField(
            value = formState.notes,
            onValueChange = { viewModel.updateNotes(it) },
            label = { Text(stringResource(R.string.notes_label)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5
        )

        // Image Selection
        Text(stringResource(R.string.bird_picture_label), style = MaterialTheme.typography.titleMedium)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (formState.imagePath != null) {
                    AsyncImage(
                        model = formState.imagePath,
                        contentDescription = stringResource(R.string.bird_image_content_description),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = {
                            viewModel.deleteOldPhoto(formState.imagePath)
                            viewModel.updateImagePath(null)
                        },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.remove_image_content_description), tint = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.no_picture_added))
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
                Text(stringResource(R.string.take_photo_action))
            }
            OutlinedButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.gallery_action))
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
                        "Genetic Traits",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    if (flock?.defaultGeneticProfile != null) {
                         Row(
                             modifier = Modifier.fillMaxWidth(),
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             Text(
                                 "Use flock default traits",
                                 modifier = Modifier.weight(1f),
                                 style = MaterialTheme.typography.bodyMedium
                             )
                             Switch(
                                 checked = useFlockDefaults,
                                 onCheckedChange = { checked -> 
                                     useFlockDefaults = checked
                                     if (checked) {
                                         // Restore defaults
                                         geneticTraits = flock?.defaultGeneticProfile?.traitValues ?: emptyMap()
                                     }
                                 }
                             )
                         }
                         Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    if (!useFlockDefaults || flock?.defaultGeneticProfile == null) {
                        Text(
                            "Define specific traits for this bird.",
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
                    } else {
                        // Read-only display of inherited traits
                        Text(
                            "Inheriting traits from flock.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                         Spacer(modifier = Modifier.height(8.dp))
                         // Show read-only chips? using disabled picker logic or just manual display
                         // For now, simpler to just show "Inheriting"
                    }
                }
            }
        }

        // Save button
        Button(
            onClick = {
                // Validate required fields
                var isValid = true
                
                if (formState.species.isBlank()) {
                    speciesError = "Species is required"
                    isValid = false
                }
                
                if (formState.breed.isBlank()) {
                    breedError = "Breed is required"
                    isValid = false
                }
                var formattedDate = ""
                if (formState.hatchDate.isNotBlank()) {
                    // Try to parse using user's locale preference
                    try {
                        val inputFormat = SimpleDateFormat(parsePattern, Locale.getDefault())
                        inputFormat.isLenient = false
                        val date = inputFormat.parse(formState.hatchDate.trim())
                        if (date != null) {
                             // Normalize to ISO (YYYY-MM-DD) for database consistency
                            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            formattedDate = outputFormat.format(date)
                        } else {
                            hatchDateError = "Invalid date"
                            isValid = false
                        }
                    } catch (e: Exception) {
                        hatchDateError = "Format: $displayPattern"
                        isValid = false
                    }
                }
                
                if (isValid) {
                    val newBird = Bird(
                        flockId = flockId, // Save flockId
                        species = formState.species.trim(),
                        breed = formState.breed.trim(),
                        sex = formState.sex,
                        hatchDate = formattedDate, // Use conversion result
                        color = formState.color.takeIf { it.isNotBlank() },
                        notes = formState.notes.takeIf { it.isNotBlank() },
                        imagePath = formState.imagePath,
                        customGeneticProfile = if (isMixed && !useFlockDefaults && geneticTraits.isNotEmpty()) {
                            GeneticProfile(traitValues = geneticTraits)
                        } else null
                    )
                    
                    // Save to Database using ViewModel
                    viewModel.saveBird(newBird) {
                         // Callback
                        onBirdSaved(newBird)
                    }
                    
                    // Show success message (Optional since we navigate back)
                    showSuccessMessage = true
                    
                    // Clear form (Optional since we navigate back)
                    viewModel.clearForm()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text(stringResource(R.string.save_bird_action))
        }


        // Success message
        if (showSuccessMessage) {
            LaunchedEffect(Unit) {
                delay(2000)
                showSuccessMessage = false
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "Bird saved successfully!",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkAddDialog(
    onDismiss: () -> Unit,
    onAdd: (Int, String, Sex, String) -> Unit,
    initialSpeciesId: String,
    initialSpecies: String,
    displayPattern: String,
    parsePattern: String,
    breed: String,
    onBreedClick: () -> Unit,
    onBreedChange: (String) -> Unit,
    quantity: String,
    onQuantityChange: (String) -> Unit,
    sex: Sex,
    onSexChange: (Sex) -> Unit,
    hatchDate: String,
    onHatchDateChange: (String) -> Unit
) {
    var quantityError by remember { mutableStateOf<String?>(null) }
    var breedError by remember { mutableStateOf<String?>(null) }
    var hatchDateError by remember { mutableStateOf<String?>(null) }
    
    var sexExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.bulk_add_birds_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.species_format, initialSpecies), style = MaterialTheme.typography.bodySmall)
                
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { 
                        if (it.all { char -> char.isDigit() }) onQuantityChange(it)
                    },
                    label = { Text(stringResource(R.string.number_of_birds_required_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = quantityError != null,
                    supportingText = quantityError?.let { { Text(it) } },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )

                // Breed Field (Clickable)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBreedClick() }
                ) {
                    OutlinedTextField(
                        value = breed,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.breed_required_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = breedError != null,
                        supportingText = breedError?.let { { Text(it) } },
                        enabled = false,
                        trailingIcon = {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.select_breed_content_description))
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = sexExpanded,
                    onExpandedChange = { sexExpanded = !sexExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = sex.name.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.sex_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sexExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = sexExpanded,
                        onDismissRequest = { sexExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.male_label)) },
                            onClick = { onSexChange(Sex.MALE); sexExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.female_label)) },
                            onClick = { onSexChange(Sex.FEMALE); sexExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.unknown_label)) },
                            onClick = { onSexChange(Sex.UNKNOWN); sexExpanded = false }
                        )
                    }
                }

                OutlinedTextField(
                    value = hatchDate,
                    onValueChange = { onHatchDateChange(it); hatchDateError = null },
                    label = { Text(stringResource(R.string.hatch_date_optional_label)) },
                    placeholder = { Text(displayPattern) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = hatchDateError != null,
                    supportingText = hatchDateError?.let { { Text(it) } }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    var isValid = true
                    val q = quantity.toIntOrNull() ?: 0
                    if (q <= 0) {
                        quantityError = "Enter a valid number"
                        isValid = false
                    }
                    
                    if (breed.isBlank()) {
                        breedError = "Breed is required"
                        isValid = false
                    }
                    
                    var formattedDate = ""
                    if (hatchDate.isNotBlank()) {
                        try {
                            val inputFormat = SimpleDateFormat(parsePattern, Locale.getDefault())
                            inputFormat.isLenient = false
                            val date = inputFormat.parse(hatchDate.trim())
                            if (date != null) {
                                val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                formattedDate = outputFormat.format(date)
                            } else {
                                hatchDateError = "Invalid date"
                                isValid = false
                            }
                        } catch (e: Exception) {
                            hatchDateError = "Format: $displayPattern"
                            isValid = false
                        }
                    }
                    
                    if (isValid) {
                        onAdd(q, breed.trim(), sex, formattedDate)
                    }
                }
            ) {
                Text(stringResource(R.string.add_to_flock_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}












