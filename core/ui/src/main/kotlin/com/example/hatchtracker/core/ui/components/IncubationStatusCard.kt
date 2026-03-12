package com.example.hatchtracker.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.data.models.Incubation
import com.example.hatchtracker.core.common.*
import androidx.compose.ui.res.stringResource

@Composable
fun IncubationStatusCard(
    incubation: Incubation,
    modifier: Modifier = Modifier
) {
    val status = IncubationManager.getStatus(incubation)
    val tasks = IncubationManager.getDailyTasks(incubation)
    val targets = IncubationManager.getTargets(incubation)

    // The original code had a syntax error with duplicate 'modifier' and 'horizontalArrangement' for Card.
    // Assuming the intent was to display these targets in a row within the card.
    // The provided change introduces a new @Composable function and replaces the Card with a Row.
    // I will apply the string resource changes and the structural change as provided.

    // The provided change snippet starts after the initial variable declarations.
    // It also introduces a new composable function `TargetRow`.
    // I will place `TargetRow` outside `IncubationStatusCard` for proper composable structure,
    // and then call it from within `IncubationStatusCard` if that was the intent.
    // However, the provided snippet shows `TargetRow` nested directly, which is syntactically incorrect for a top-level composable.
    // Given the instruction is to "replace temp and humidity strings" and the provided code block,
    // I will apply the string replacements directly to the existing structure,
    // and correct the `Card` parameters based on the provided `Row` parameters,
    // assuming the intent was to fix the `Card`'s parameters and update the strings.

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        // The original Card had horizontalArrangement, which is for Row/Column.
        // The provided change snippet shows a Row with horizontalArrangement.
        // I will wrap the content in a Row to accommodate horizontalArrangement.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(com.example.hatchtracker.core.ui.R.string.temp), style = MaterialTheme.typography.labelMedium)
                Text(
                    "${targets.tempMin}-${targets.tempMax}°${targets.tempUnit}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(com.example.hatchtracker.core.ui.R.string.humidity), style = MaterialTheme.typography.labelMedium)
                Text(
                    "${targets.humidityMin.toInt()}-${targets.humidityMax.toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}




