package com.example.hatchtracker.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import com.example.hatchtracker.core.ui.composeutil.premiumClickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.data.models.TraitDisplayCatalog
import com.example.hatchtracker.data.models.GeneticTrait
import com.example.hatchtracker.data.models.TraitOption

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GeneticTraitPicker(
    selectedTraits: Map<String, String>,
    onTraitChanged: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTraitId by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState()
    
    // Bottom Sheet for options
    if (activeTraitId != null) {
        val trait = TraitDisplayCatalog.getTrait(activeTraitId!!)
        if (trait != null) {
            ModalBottomSheet(
                onDismissRequest = { activeTraitId = null },
                sheetState = sheetState
            ) {
                TraitOptionSheetContent(
                    trait = trait,
                    currentSelection = selectedTraits[trait.id],
                    onSelect = { optionId ->
                        onTraitChanged(trait.id, optionId)
                        activeTraitId = null
                    },
                    onClear = {
                        onTraitChanged(trait.id, null)
                        activeTraitId = null
                    }
                )
            }
        }
    }

    Column(modifier = modifier) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TraitDisplayCatalog.traits.forEach { trait ->
                val selectedOptionId = selectedTraits[trait.id]
                val selectedOption = trait.options.find { it.id == selectedOptionId }
                
                TraitChip(
                    label = trait.label,
                    value = selectedOption?.label,
                    colorHex = selectedOption?.colorHex,
                    onClick = { activeTraitId = trait.id }
                )
            }
        }
    }
}

@Composable
fun TraitChip(
    label: String,
    value: String?,
    colorHex: String?,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (value != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        color = if (value != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        modifier = Modifier.height(32.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            if (colorHex != null) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(colorHex)))
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Text(
                text = "$label: ${value ?: "Not Set"}",
                style = MaterialTheme.typography.labelMedium,
                color = if (value != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TraitOptionSheetContent(
    trait: GeneticTrait,
    currentSelection: String?,
    onSelect: (String) -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(trait.label, style = MaterialTheme.typography.titleLarge)
        Text(trait.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn {
            items(trait.options) { option ->
                TraitOptionItem(
                    option = option,
                    isSelected = option.id == currentSelection,
                    onClick = { onSelect(option.id) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear Selection")
                }
            }
        }
    }
}

@Composable
fun TraitOptionItem(
    option: TraitOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .premiumClickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (option.colorHex != null) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(option.colorHex)))
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        
        Text(
            text = option.label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
        }
    }
}
