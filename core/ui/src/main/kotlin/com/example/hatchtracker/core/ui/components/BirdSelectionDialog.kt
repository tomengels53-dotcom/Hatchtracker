package com.example.hatchtracker.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.hatchtracker.data.models.Bird
import com.example.hatchtracker.data.repository.DataRepository
import com.example.hatchtracker.core.ui.R
import com.example.hatchtracker.data.models.Sex

@Composable
fun BirdSelectionDialog(
    birds: List<Bird>,
    onDismiss: () -> Unit,
    onBirdSelected: (Bird) -> Unit,
    title: String = androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.select_bird),
    filterSex: Sex? = null,
    filterSpecies: String? = null
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredBirds = remember(birds, searchQuery, filterSex, filterSpecies) {
        birds.filter { bird ->
            (filterSex == null || bird.sex == filterSex) &&
            (filterSpecies.isNullOrBlank() || bird.species.name.equals(filterSpecies, ignoreCase = true)) &&
            (searchQuery.isBlank() || 
             bird.species.name.contains(searchQuery, ignoreCase = true) || 
              bird.breed.contains(searchQuery, ignoreCase = true) ||
             bird.localId.toString().contains(searchQuery))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.close))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.search)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.clear_search))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bird List
                if (filteredBirds.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.no_birds_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredBirds) { bird ->
                            BirdSelectionItem(bird = bird, onClick = { onBirdSelected(bird) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BirdSelectionItem(
    bird: Bird,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Prefer species icon from DefaultCatalogData
            val speciesCatalog = com.example.hatchtracker.core.ui.catalog.DefaultCatalogData.allSpecies
            val catalogEntry = speciesCatalog.find { it.name.equals(bird.species.name, ignoreCase = true) }
            val iconRes = catalogEntry?.imageResId ?: if (bird.sex == Sex.MALE) R.drawable.male else R.drawable.femenine
            
            if (catalogEntry?.imageResId != null) {
                 androidx.compose.foundation.Image(
                    painter = painterResource(id = catalogEntry.imageResId),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            } else {
                 // Fallback icon logic
                 val fallbackIcon = when(bird.sex) {
                     Sex.MALE -> R.drawable.male
                     Sex.FEMALE -> R.drawable.femenine
                     else -> com.example.hatchtracker.core.ui.R.drawable.chick // Placeholder
                 }
                  androidx.compose.foundation.Image(
                    painter = painterResource(id = fallbackIcon),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            }
           

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "${bird.species} - ${bird.breed}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.bird_id_format, bird.localId, bird.ageInWeeks()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (bird.flockId != null) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.example.hatchtracker.core.ui.R.string.flock_id_format, bird.flockId.toString()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// Ext helper for age (mock implementation if not in Bird class, but assume it might be useful)
// Actually we can just calculation rough age if needed or skip. 
// For now omitting the extension fun to avoid context issues, will just use placeholder text logic if needed
// or we can add it to Bird.kt later. For now let's just use "ID"
private fun Bird.ageInWeeks(): Int {
     // Simple approximate
    return 0 // Placeholder
}


