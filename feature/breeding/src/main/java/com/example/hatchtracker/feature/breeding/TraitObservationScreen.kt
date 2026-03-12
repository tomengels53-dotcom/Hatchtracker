package com.example.hatchtracker.feature.breeding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.hatchtracker.data.models.EvidenceType
import com.example.hatchtracker.domain.breeding.CommunityValidationManager
import com.example.hatchtracker.core.ui.R as UiR
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraitObservationScreen(
    onBack: () -> Unit,
    prefilledBreedId: String = "",
    prefilledParentPairId: String = "",
    prefilledTraitId: String = "",
    canAccessBreeding: Boolean,
    onNavigateToPaywall: () -> Unit = {}
) {
    if (!canAccessBreeding) {
        BreedingLockedScreen(
            onBack = onBack,
            onViewPlans = onNavigateToPaywall
        )
        return
    }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var breedId by remember { mutableStateOf(prefilledBreedId) }
    var traitId by remember { mutableStateOf(prefilledTraitId) } // In real app, select from dropdown of known traits
    var observedValue by remember { mutableStateOf("") }
    var confidence by remember { mutableFloatStateOf(0.8f) }
    var notes by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(UiR.string.trait_observation_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(UiR.string.back_action))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(UiR.string.trait_observation_help), style = MaterialTheme.typography.bodyMedium)
            
            OutlinedTextField(
                value = breedId,
                onValueChange = { breedId = it },
                label = { Text(stringResource(UiR.string.trait_observation_breed_id_label)) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = traitId,
                onValueChange = { traitId = it },
                label = { Text(stringResource(UiR.string.trait_observation_trait_id_label)) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = observedValue,
                onValueChange = { observedValue = it },
                label = { Text(stringResource(UiR.string.trait_observation_observed_value_label)) },
                modifier = Modifier.fillMaxWidth()
            )

            Text(stringResource(UiR.string.trait_observation_confidence_format, (confidence * 100).toInt()))
            Slider(
                value = confidence,
                onValueChange = { confidence = it },
                valueRange = 0f..1f
            )
            
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(UiR.string.trait_observation_notes_label)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            Button(
                onClick = {
                    scope.launch {
                        val result = CommunityValidationManager.submitObservation(
                            breedId = breedId,
                            traitId = traitId,
                            observedValue = observedValue,
                            confidence = confidence.toDouble(),
                            parentPairId = prefilledParentPairId,
                            evidenceType = EvidenceType.NONE, // Only supporting manual entry for now
                            notes = notes
                        )
                        
                        if (result.isSuccess) {
                            resultMessage = context.getString(UiR.string.trait_observation_submit_success, result.getOrNull().orEmpty())
                            // Clear form?
                        } else {
                            resultMessage = context.getString(UiR.string.error_prefix, result.exceptionOrNull()?.message.orEmpty())
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = breedId.isNotBlank() && traitId.isNotBlank() && observedValue.isNotBlank()
            ) {
                Text(stringResource(UiR.string.trait_observation_submit_action))
            }
            
            if (resultMessage != null) {
                Text(
                    text = resultMessage!!,
                    color = if (resultMessage!!.startsWith("Error")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}




