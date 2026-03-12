package com.example.hatchtracker.feature.finance

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.core.ui.R
import com.example.hatchtracker.data.models.FinancialCategory
import com.example.hatchtracker.core.ui.components.ProFeatureDialog
import com.example.hatchtracker.data.models.SubscriptionTier
import java.util.Currency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFinancialEntryScreen(
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    scannedVendor: String? = null,
    scannedDateMillis: Long? = null,
    scannedTotal: Double? = null,
    onScanReceiptClick: () -> Unit = {},
    onClearReceiptScan: () -> Unit = {},
    viewModel: AddFinancialEntryViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val beneficiaries by viewModel.beneficiaries.collectAsState()
    val currencyCode by viewModel.currencyCode.collectAsState()
    val scrollState = rememberScrollState()

    var showProDialog by rememberSaveable { mutableStateOf(false) }

    ProFeatureDialog(
        showDialog = showProDialog,
        onDismissRequest = { showProDialog = false },
        onUpgradeClick = { showProDialog = false },
        title = "Receipt OCR Scanning",
        text = "Receipt OCR scanning is available in PRO. Upgrade to unlock this feature."
    )

    LaunchedEffect(scannedVendor, scannedDateMillis, scannedTotal) {
        scannedVendor?.let { viewModel.onVendorChange(it) }
        scannedDateMillis?.let { viewModel.onDateChange(it) }
        scannedTotal?.let { viewModel.onAmountChange(it.toString()) }
        
        if (scannedVendor != null || scannedDateMillis != null || scannedTotal != null) {
            onClearReceiptScan()
        }
    }

    val currencySymbol = remember(currencyCode) {
        try {
            Currency.getInstance(currencyCode).symbol
        } catch (e: Exception) {
            currencyCode
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isRevenue) stringResource(R.string.finance_title_add_revenue) else stringResource(R.string.finance_title_add_cost)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_action))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val isPro = userProfile?.subscriptionActive == true && userProfile?.subscriptionTier != SubscriptionTier.FREE
                        if (isPro) {
                            onScanReceiptClick()
                        } else {
                            showProDialog = true
                        }
                    }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Receipt")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            OutlinedTextField(
                value = uiState.amount,
                onValueChange = { viewModel.onAmountChange(it) },
                label = { Text(stringResource(R.string.finance_label_amount, currencySymbol)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            if (viewModel.isSharedMode) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.finance_label_split_cost),
                            style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = beneficiaries.all { it.isSelected },
                                onCheckedChange = { viewModel.toggleAllBeneficiaries(it) }
                            )
                            Text(stringResource(R.string.finance_label_select_all), style = MaterialTheme.typography.bodyMedium)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Column(modifier = Modifier.fillMaxWidth()) {
                            beneficiaries.forEach { item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.toggleBeneficiary(item.id) }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = item.isSelected,
                                        onCheckedChange = { viewModel.toggleBeneficiary(item.id) }
                                    )
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }

                        val selectedCount = beneficiaries.count { it.isSelected }
                        if (selectedCount > 0) {
                            val totalAmount = uiState.amount.toDoubleOrNull() ?: 0.0
                            val splitAmount = totalAmount / selectedCount
                            val formattedSplit = "%.2f".format(splitAmount)

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                text = stringResource(R.string.finance_label_split_amount, currencySymbol, formattedSplit),
                                style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (currencySymbol != currencyCode) {
                                Text(
                                    text = "($currencyCode)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = uiState.quantity,
                onValueChange = { viewModel.onQuantityChange(it) },
                label = { Text(stringResource(R.string.finance_label_quantity)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                OutlinedTextField(
                    value = uiState.category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.finance_label_category)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    FinancialCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                viewModel.onCategoryChange(category)
                                expanded = false
                            }
                        )
                    }
                }
            }

            if (uiState.isSaleBlocked) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.finance_error_record_sale_block),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onBackClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.finance_button_go_to_record_sale))
                        }
                    }
                }
            }

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { viewModel.onNotesChange(it) },
                label = { Text(stringResource(R.string.finance_label_notes_optional)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                minLines = 3
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.saveEntry(onSaveSuccess) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.amount.isNotEmpty() &&
                    (!viewModel.isSharedMode || beneficiaries.any { it.isSelected }) &&
                    !uiState.isSaleBlocked
            ) {
                Text(stringResource(R.string.finance_button_save_entry))
            }
        }
    }
}
