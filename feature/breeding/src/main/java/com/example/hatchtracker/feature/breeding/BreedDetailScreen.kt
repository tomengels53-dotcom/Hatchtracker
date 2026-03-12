package com.example.hatchtracker.feature.breeding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hatchtracker.core.ui.R as UiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreedDetailScreen(
    breedId: String,
    onBackClick: () -> Unit,
    viewModel: BreedDetailViewModel = hiltViewModel()
) {
    val breed by viewModel.breed.collectAsState()

    LaunchedEffect(breedId) {
        viewModel.loadBreed(breedId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(breed?.name ?: stringResource(UiR.string.breed_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(UiR.string.back_action))
                    }
                }
            )
        }
    ) { paddingValues ->
    if (breed == null) {
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text(stringResource(UiR.string.breed_information_not_found))
        }
    } else {
        val currentBreed = breed ?: return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Headers
            InfoSection(title = stringResource(UiR.string.origin_label), content = currentBreed.origin)
            InfoSection(title = stringResource(UiR.string.species_label), content = currentBreed.species)
            InfoSection(title = stringResource(UiR.string.egg_color_label), content = currentBreed.eggColor)
            
            if (currentBreed.acceptedColors.isNotEmpty()) {
                InfoSection(title = stringResource(UiR.string.accepted_colors_label), content = currentBreed.acceptedColors.joinToString(", "))
            }

            InfoSection(
                title = stringResource(UiR.string.standard_weights_label),
                content = stringResource(UiR.string.standard_weights_format, currentBreed.weightRoosterKg, currentBreed.weightHenKg)
            )

            if (currentBreed.recognizedBy.isNotEmpty()) {
                InfoSection(title = stringResource(UiR.string.recognized_by_label), content = currentBreed.recognizedBy.joinToString(", "))
            }

                HorizontalDivider()

                // Genetic Profile
                Text(stringResource(UiR.string.genetic_profile_title), style = MaterialTheme.typography.titleLarge)
                
            if (currentBreed.geneticProfile.fixedTraits.isNotEmpty()) {
                TraitList(title = stringResource(UiR.string.fixed_traits_label), traits = currentBreed.geneticProfile.fixedTraits)
            }
            
            if (currentBreed.geneticProfile.knownGenes.isNotEmpty()) {
                TraitList(title = stringResource(UiR.string.known_genes_label), traits = currentBreed.geneticProfile.knownGenes)
            }

            if (currentBreed.geneticProfile.inferredTraits.isNotEmpty()) {
                TraitList(title = stringResource(UiR.string.inferred_traits_label), traits = currentBreed.geneticProfile.inferredTraits)
            }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun InfoSection(title: String, content: String) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(content, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun TraitList(title: String, traits: List<String>) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        traits.forEach { trait ->
            Text("\u2022 ${trait.replace("_", " ").replaceFirstChar { it.uppercase() }}", modifier = Modifier.padding(start = 8.dp))
        }
    }
}


