package com.example.hatchtracker.feature.breeding.actionplan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hatchtracker.core.ui.R as UiR
import com.example.hatchtracker.core.ui.components.AppCard
import com.example.hatchtracker.core.ui.components.AppCardVariant


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreedingProgramBirdMultiPickerScreen(
    genIndex: Int,
    onNavigateBack: () -> Unit,
    viewModel: BreedingProgramDetailViewModel = hiltViewModel()
) {
    var selectedBirds by remember { mutableStateOf(setOf<String>()) }
    
    val mockBirds = listOf("B1: Bluey", "B2: Speckles", "B3: Goldie", "B4: Shadow")

    Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(UiR.string.breeding_select_birds_for_generation, genIndex)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(UiR.string.back_action))
                        }
                    },
                    actions = {
                        TextButton(onClick = { 
                            viewModel.setSelectedBirds(genIndex, selectedBirds.toList())
                            onNavigateBack()
                        }) {
                            Text(stringResource(UiR.string.breeding_save_action))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                )
            },
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ) { padding ->
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(mockBirds) { bird ->
                    val id = bird.substringBefore(": ")
                    val name = bird.substringAfter(": ")
                    AppCard(variant = AppCardVariant.STANDARD) {
                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(name)
                            Checkbox(
                                checked = selectedBirds.contains(id),
                                onCheckedChange = { checked ->
                                    selectedBirds = if (checked) selectedBirds + id else selectedBirds - id
                                }
                            )
                        }
                    }
                }
            }
        }
    }
