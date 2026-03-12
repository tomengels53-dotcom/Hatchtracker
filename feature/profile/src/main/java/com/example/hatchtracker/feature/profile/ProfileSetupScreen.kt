@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.hatchtracker.feature.profile

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.domain.breeding.CountryDefaults
import com.example.hatchtracker.domain.breeding.LocalizationDefaults

@Composable
fun ProfileSetupScreen(
    initialCountryCode: String? = null,
    onComplete: (countryCode: String, weightUnit: String, dateFormat: String, timeFormat: String, currencyCode: String) -> Unit
) {
    var selectedCountry by remember { 
        mutableStateOf<CountryDefaults?>(LocalizationDefaults.countries.find { it.code == initialCountryCode }) 
    }
    var weightUnit by remember(selectedCountry) { mutableStateOf(selectedCountry?.weightUnit ?: "kg") }
    var dateFormat by remember(selectedCountry) { mutableStateOf(selectedCountry?.dateFormat ?: "DD-MM-YYYY") }
    var timeFormat by remember(selectedCountry) { mutableStateOf(selectedCountry?.timeFormat ?: "24h") }
    
    var countryExpanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.setup_title),
            style = MaterialTheme.typography.headlineMedium
        )
        
        Text(
            text = androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.setup_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Country Selection
        ExposedDropdownMenuBox(
            expanded = countryExpanded,
            onExpandedChange = { countryExpanded = !countryExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedCountry?.name ?: "",
                onValueChange = { },
                readOnly = true,
                label = { Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.country_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryExpanded) },
                modifier = Modifier
                    .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = countryExpanded,
                onDismissRequest = { countryExpanded = false }
            ) {
                LocalizationDefaults.countries.forEach { country ->
                    DropdownMenuItem(
                        text = { Text(country.name) },
                        onClick = {
                            selectedCountry = country
                            weightUnit = country.weightUnit
                            dateFormat = country.dateFormat
                            timeFormat = country.timeFormat
                            countryExpanded = false
                        }
                    )
                }
            }
        }
        
        // Settings Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.unit_defaults_title),
                    style = MaterialTheme.typography.titleMedium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.weight_unit_label))
                    Text(weightUnit, style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.date_format_label))
                    Text(dateFormat, style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.time_format_label))
                    Text(timeFormat, style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = {
                selectedCountry?.let { country ->
                    onComplete(country.code, weightUnit, dateFormat, timeFormat, country.currencyCode)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedCountry != null
        ) {
            Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.profile.R.string.complete_setup))
        }
    }
}





