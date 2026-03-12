package com.example.hatchtracker.feature.finance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.feature.finance.AddSalesBatchViewModel
import java.text.NumberFormat
import java.util.*
import com.example.hatchtracker.core.ui.components.PricingSettingsDialog
import com.example.hatchtracker.billing.SubscriptionStateManager
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.Icons
import androidx.compose.animation.AnimatedVisibility

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.example.hatchtracker.core.ui.R as UiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSalesBatchScreen(
    onBackClick: () -> Unit,
    viewModel: AddSalesBatchViewModel = hiltViewModel()
) {
    val itemType by viewModel.itemType.collectAsState()
    val quantity by viewModel.quantity.collectAsState()
    val unitPrice by viewModel.unitPrice.collectAsState()
    val buyerType by viewModel.buyerType.collectAsState()
    val buyerName by viewModel.buyerName.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val totalPrice by viewModel.totalPrice.collectAsState(0.0)
    val isSaving by viewModel.isSaving.collectAsState()

    val pricingSuggestionResult by viewModel.pricingSuggestionResult.collectAsState()
    val desiredMargin by viewModel.desiredMargin.collectAsState()
    val marketType by viewModel.marketType.collectAsState()
    val caps by viewModel.capabilities.collectAsState()
    val selectedBirds by viewModel.selectedBirds.collectAsState()
    val isQuantityLocked by viewModel.isQuantityLocked.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    var showExplanation by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(UiR.string.record_batch_sale_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(UiR.string.back_action))
                    }
                },
                actions = {
                    if (caps.isPricingStrategyEnabled) {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(UiR.string.finance_pricing_settings_content_description))
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (showSettings) {
            PricingSettingsDialog(
                currentMargin = desiredMargin,
                currentMarket = marketType,
                onMarginChange = { viewModel.setDesiredMargin(it) },
                onMarketChange = { viewModel.setMarketType(it) },
                onVatModeChange = { },
                onVatRateChange = { },
                onDismiss = { showSettings = false }
            )
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Item Type Selection
            Text(stringResource(UiR.string.finance_sale_type_label), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "egg" to UiR.string.finance_sale_type_egg,
                    "chick" to UiR.string.finance_sale_type_chick,
                    "adult" to UiR.string.finance_sale_type_adult
                ).forEach { (type, labelRes) ->
                    FilterChip(
                        selected = itemType == type,
                        onClick = { viewModel.setItemType(type) },
                        label = { Text(stringResource(labelRes)) }
                    )
                }
            }

            // Quantity & Unit Price
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { if (!isQuantityLocked) viewModel.setQuantity(it) },
                    label = { Text(stringResource(UiR.string.finance_label_quantity)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    readOnly = isQuantityLocked,
                    supportingText = if (isQuantityLocked) {
                        { Text(stringResource(UiR.string.locked_by_current_selection)) }
                    } else null
                )
                OutlinedTextField(
                    value = unitPrice,
                    onValueChange = { viewModel.setUnitPrice(it) },
                    label = { Text(stringResource(UiR.string.finance_label_unit_price)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }

            // Selected Birds Display
            if (selectedBirds.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(UiR.string.finance_selected_birds_count, selectedBirds.size),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        selectedBirds.forEach { bird ->
                            val genderText = when (bird.sex) {
                                com.example.hatchtracker.data.models.Sex.MALE -> stringResource(UiR.string.sex_male)
                                com.example.hatchtracker.data.models.Sex.FEMALE -> stringResource(UiR.string.sex_female)
                                else -> stringResource(UiR.string.unknown_label)
                            }
                            Text(
                                text = stringResource(
                                    UiR.string.finance_selected_bird_item,
                                    bird.species,
                                    bird.breed,
                                    genderText
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Pricing Suggestion (PRO)
            if (caps.isPricingStrategyEnabled && pricingSuggestionResult is com.example.hatchtracker.domain.pricing.PricingSuggestionResult.Available) {
                val suggestion = (pricingSuggestionResult as com.example.hatchtracker.domain.pricing.PricingSuggestionResult.Available).suggestion
                com.example.hatchtracker.core.ui.components.PricingInsightCard(
                    suggestion = suggestion,
                    currencyCode = "USD", // TODO: Fetch from ViewModel/UserProfile
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Button(
                    onClick = { viewModel.useSuggestedPrice() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(UiR.string.finance_use_suggested_price))
                }
            } else if (caps.isPricingStrategyEnabled && pricingSuggestionResult is com.example.hatchtracker.domain.pricing.PricingSuggestionResult.Unavailable) {
                 // Optional: Show why unavailable?
                 // Text("Pricing unavailable: ${(pricingSuggestionResult as PricingSuggestionResult.Unavailable).reason}", style = MaterialTheme.typography.bodySmall)
            }

            // Total Price Display
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(UiR.string.finance_label_total_price), style = MaterialTheme.typography.titleMedium)
                    Text(
                        com.example.hatchtracker.domain.breeding.CurrencyUtils.formatCurrency(totalPrice),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Buyer Info
            Text(stringResource(UiR.string.finance_label_buyer_information), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "private" to UiR.string.finance_buyer_type_private,
                    "market" to UiR.string.finance_buyer_type_market,
                    "breeder" to UiR.string.finance_buyer_type_breeder
                ).forEach { (type, labelRes) ->
                    FilterChip(
                        selected = buyerType == type,
                        onClick = { viewModel.setBuyerType(type) },
                        label = { Text(stringResource(labelRes)) }
                    )
                }
            }
            OutlinedTextField(
                value = buyerName,
                onValueChange = { viewModel.setBuyerName(it) },
                label = { Text(stringResource(UiR.string.buyer_name_optional)) },
                modifier = Modifier.fillMaxWidth()
            )

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { viewModel.setNotes(it) },
                label = { Text(stringResource(UiR.string.notes_optional_label)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    viewModel.saveSale(
                        onSuccess = onBackClick,
                        onError = { errorMessage = it }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(UiR.string.finance_button_confirm_sale))
                }
            }
        }
    }
}






