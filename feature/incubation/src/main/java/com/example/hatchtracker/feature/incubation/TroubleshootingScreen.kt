package com.example.hatchtracker.feature.incubation

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.hatchtracker.troubleshooting.ConfidenceLevel
import com.example.hatchtracker.troubleshooting.Diagnosis
import com.example.hatchtracker.troubleshooting.IncubationTroubleshooter
import androidx.compose.foundation.text.KeyboardOptions
import com.example.hatchtracker.core.ui.components.ProFeatureDialog
import com.example.hatchtracker.data.models.SubscriptionTier
import androidx.compose.ui.res.stringResource
import com.example.hatchtracker.core.ui.R as UiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TroubleshootingScreen(
    viewModel: TroubleshootingViewModel,
    onBackClick: () -> Unit = {},
    scannedTemp: Double? = null,
    scannedHumidity: Double? = null,
    onScanIncubatorClick: () -> Unit = {},
    onClearIncubatorScan: () -> Unit = {}
) {
    val incubation by viewModel.incubation.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val troubleshooter = remember { IncubationTroubleshooter() }

    var showProDialog by rememberSaveable { mutableStateOf(false) }

    ProFeatureDialog(
        showDialog = showProDialog,
        onDismissRequest = { showProDialog = false },
        onUpgradeClick = { showProDialog = false },
        title = stringResource(UiR.string.incubator_ocr_scanning_title),
        text = stringResource(UiR.string.incubator_ocr_scanning_upgrade_copy)
    )
    
    // State for manual sensor input
    var tempInput by remember { mutableStateOf("") }
    var humidityInput by remember { mutableStateOf("") }

    LaunchedEffect(scannedTemp, scannedHumidity) {
        if (scannedTemp != null) {
            tempInput = scannedTemp.toString()
        }
        if (scannedHumidity != null) {
            humidityInput = scannedHumidity.toString()
        }
        if (scannedTemp != null || scannedHumidity != null) {
            onClearIncubatorScan()
        }
    }
    
    // Diagnosis State
    var diagnoses by remember { mutableStateOf<List<Diagnosis>>(emptyList()) }
    var hasRun by remember { mutableStateOf(false) }

    fun runAnalysis() {
        val currentIncubation = incubation ?: return
        val t = tempInput.toDoubleOrNull()
        val h = humidityInput.toDoubleOrNull()
        diagnoses = troubleshooter.analyze(currentIncubation, t, h)
        hasRun = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(stringResource(UiR.string.troubleshooting_assistant_title))
                        incubation?.let {
                            Text(
                                text = stringResource(UiR.string.batch_header_format, it.species),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(UiR.string.back_action))
                    }
                }
            )
        }
    ) { padding ->
        if (incubation == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                // Input Section
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(UiR.string.enter_current_conditions),
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(onClick = {
                                val isPro = userProfile?.subscriptionActive == true && userProfile?.subscriptionTier != SubscriptionTier.FREE
                                if (isPro) {
                                    onScanIncubatorClick()
                                } else {
                                    showProDialog = true
                                }
                            }) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = stringResource(UiR.string.scan_incubator_action))
                            }
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(
                                value = tempInput,
                                onValueChange = { tempInput = it },
                                label = { Text(stringResource(UiR.string.temp_short_label)) },
                                suffix = { Text("\u00B0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = humidityInput,
                                onValueChange = { humidityInput = it },
                                label = { Text(stringResource(UiR.string.humidity_label)) },
                                suffix = { Text("%") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Button(
                            onClick = { runAnalysis() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(UiR.string.analyze_now_action))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Results Section
                if (hasRun) {
                    if (diagnoses.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    stringResource(UiR.string.no_obvious_issues_detected),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(diagnoses) { diagnosis ->
                                DiagnosisCard(diagnosis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosisCard(diagnosis: Diagnosis) {
    val configColor = when (diagnosis.confidence) {
        ConfidenceLevel.HIGH -> MaterialTheme.colorScheme.errorContainer
        ConfidenceLevel.MEDIUM -> MaterialTheme.colorScheme.tertiaryContainer
        ConfidenceLevel.LOW -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = when (diagnosis.confidence) {
        ConfidenceLevel.HIGH -> MaterialTheme.colorScheme.onErrorContainer
        ConfidenceLevel.MEDIUM -> MaterialTheme.colorScheme.onTertiaryContainer
        ConfidenceLevel.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = configColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (diagnosis.confidence == ConfidenceLevel.HIGH) Icons.Default.Warning else Icons.Default.Info,
                    contentDescription = null,
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = diagnosis.issue,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis,
                    color = contentColor
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = diagnosis.explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Actions
            if (diagnosis.immediateActions.isNotEmpty()) {
                Text(
                    text = stringResource(UiR.string.immediate_actions_title),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis,
                    color = contentColor.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                diagnosis.immediateActions.forEach { action ->
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text("\u2022 ", color = contentColor)
                        Text(action, style = MaterialTheme.typography.bodyMedium, color = contentColor)
                    }
                }
            }
        }
    }
}
