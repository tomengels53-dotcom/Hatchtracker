package com.example.hatchtracker.feature.devices

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.example.hatchtracker.core.domain.models.DepreciationMethod
import com.example.hatchtracker.model.CatalogDevice
import com.example.hatchtracker.model.DeviceCategory
import com.example.hatchtracker.model.DeviceType
import com.example.hatchtracker.feature.devices.AddDeviceViewModel
import com.example.hatchtracker.core.ui.util.icon
import com.example.hatchtracker.core.ui.util.labelRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddDeviceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.devices.R.string.add_device_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.devices.R.string.desc_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // 1. Category Selector
            Text(stringResource(com.example.hatchtracker.feature.devices.R.string.device_category_label), style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            ScrollableTabRow(
                selectedTabIndex = DeviceCategory.entries.indexOf(uiState.selectedCategory),
                edgePadding = 0.dp,
                divider = {},
                containerColor = Color.Transparent,
                indicator = {}
            ) {
                DeviceCategory.entries.forEach { category ->
                    val selected = uiState.selectedCategory == category
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.onCategorySelected(category) },
                        label = { Text(stringResource(category.labelRes())) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Device Type Selector (Filtered by Category)
            val availableTypes = DeviceType.entries.filter { it.category == uiState.selectedCategory && it.name != "INCUBATOR" && it.name != "BROODER" }
            
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableTypes.forEach { type ->
                    DeviceTypeOption(
                        type = type,
                        selected = uiState.selectedType == type,
                        onClick = { viewModel.onDeviceTypeSelected(type) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Model Selection (List)
            Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.devices.R.string.select_model), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(uiState.availableModels) { model ->
                        ModelSelectionItem(
                            model = model,
                            isSelected = uiState.selectedModel?.id == model.id,
                            onClick = { viewModel.onModelSelected(model) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Configuration (Name)
            if (uiState.selectedModel != null) {
                OutlinedTextField(
                    value = uiState.customDisplayName,
                    onValueChange = { viewModel.onDisplayNameChanged(it) },
                    label = { Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.devices.R.string.device_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Show Fixed Specs
                val yes = androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.devices.R.string.yes)
                val no = androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.devices.R.string.no)
                Text(
                    text = androidx.compose.ui.res.stringResource(
                        com.example.hatchtracker.feature.devices.R.string.specs_format,
                        uiState.selectedModel?.capacityEggs ?: 0,
                        if (uiState.selectedModel?.features?.autoTurn == true) yes else no
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // 4. Asset Configuration
            if (uiState.selectedModel != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                stringResource(com.example.hatchtracker.feature.devices.R.string.track_as_capital_asset),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = uiState.trackAsAsset,
                                onCheckedChange = { viewModel.onTrackAsAssetChanged(it) }
                            )
                        }
                        if (uiState.trackAsAsset) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Purchase Date Picker
                            val calendar = remember { java.util.Calendar.getInstance() }
                            val datePickerDialog = android.app.DatePickerDialog(
                                LocalContext.current,
                                { _, year, month, dayOfMonth ->
                                    val cal = java.util.Calendar.getInstance()
                                    cal.set(year, month, dayOfMonth)
                                    viewModel.onPurchaseDateChanged(cal.timeInMillis)
                                },
                                calendar.get(java.util.Calendar.YEAR),
                                calendar.get(java.util.Calendar.MONTH),
                                calendar.get(java.util.Calendar.DAY_OF_MONTH)
                            )
                            
                            val dateText = uiState.purchaseDate?.let {
                                java.text.DateFormat.getDateInstance().format(java.util.Date(it))
                            } ?: ""
                            
                            Box {
                                OutlinedTextField(
                                    value = dateText,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(com.example.hatchtracker.feature.devices.R.string.purchase_date_label)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        IconButton(onClick = { datePickerDialog.show() }) {
                                            Icon(Icons.Filled.DateRange, contentDescription = stringResource(com.example.hatchtracker.feature.devices.R.string.select_date_desc))
                                        }
                                    }
                                )
                                Box(modifier = Modifier.matchParentSize().clickable { datePickerDialog.show() })
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = uiState.purchasePriceInput,
                                onValueChange = { viewModel.onPurchasePriceChanged(it) },
                                label = { Text(stringResource(com.example.hatchtracker.feature.devices.R.string.purchase_price_label)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = uiState.residualValueInput,
                                onValueChange = { viewModel.onResidualValueChanged(it) },
                                label = { Text(stringResource(com.example.hatchtracker.feature.devices.R.string.residual_salvage_value_label)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(stringResource(com.example.hatchtracker.feature.devices.R.string.depreciation_method_label), style = MaterialTheme.typography.labelMedium)
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = uiState.depreciationMethod == DepreciationMethod.TIME_BASED,
                                    onClick = { viewModel.onDepreciationMethodChanged(DepreciationMethod.TIME_BASED) },
                                    label = { Text(stringResource(com.example.hatchtracker.feature.devices.R.string.time_based_label)) }
                                )
                                FilterChip(
                                    selected = uiState.depreciationMethod == DepreciationMethod.CYCLE_BASED,
                                    onClick = { viewModel.onDepreciationMethodChanged(DepreciationMethod.CYCLE_BASED) },
                                    label = { Text(stringResource(com.example.hatchtracker.feature.devices.R.string.cycle_based_label)) }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (uiState.depreciationMethod == DepreciationMethod.TIME_BASED) {
                                OutlinedTextField(
                                    value = uiState.usefulLifeMonthsInput,
                                    onValueChange = { viewModel.onUsefulLifeChanged(it) },
                                    label = { Text(stringResource(com.example.hatchtracker.feature.devices.R.string.useful_life_months_label)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            } else {
                                OutlinedTextField(
                                    value = uiState.expectedCyclesInput,
                                    onValueChange = { viewModel.onExpectedCyclesChanged(it) },
                                    label = { Text(stringResource(com.example.hatchtracker.feature.devices.R.string.expected_cycles_label)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Save Button
            Button(
                onClick = { viewModel.saveDevice(onSuccess = onNavigateBack) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = uiState.selectedModel != null && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.devices.R.string.add_device_button))
                }
            }
            
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun DeviceTypeOption(
    type: DeviceType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline

    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp).widthIn(min = 120.dp),
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = type.icon(),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(type.labelRes()),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun ModelSelectionItem(
    model: CatalogDevice,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
    
    Surface(
        onClick = onClick,
        color = backgroundColor,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = model.displayName, style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                Text(
                    text = androidx.compose.ui.res.stringResource(
                        com.example.hatchtracker.feature.devices.R.string.model_capacity_format,
                        model.capacityEggs
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = androidx.compose.ui.res.stringResource(com.example.hatchtracker.feature.devices.R.string.desc_selected),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}




