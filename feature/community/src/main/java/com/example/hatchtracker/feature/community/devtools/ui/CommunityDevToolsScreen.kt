package com.example.hatchtracker.feature.community.devtools.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hatchtracker.core.ui.components.ScreenHeader
import com.example.hatchtracker.core.ui.components.HatchyInsightPulse
import com.example.hatchtracker.core.ui.components.HatchyAssistButton
import com.example.hatchtracker.core.ui.R as UiR
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import com.example.hatchtracker.domain.model.ListingCategory
import com.example.hatchtracker.domain.model.PostKind
import com.example.hatchtracker.domain.model.Visibility
import androidx.compose.foundation.shape.CircleShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityDevToolsScreen(
    onNavigateBack: () -> Unit,
    viewModel: CommunityDevToolsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val canAccess by viewModel.canAccess.collectAsState()

    if (!canAccess) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("ACCESS DENIED", color = Color.Red, style = MaterialTheme.typography.headlineLarge)
        }
        return
    }

    Scaffold(
        topBar = {
            Column {
                // RED WARNING HEADER
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Red)
                        .padding(4.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        "INTERNAL COMMUNITY DEV TOOLS",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                ScreenHeader(
                    title = "Community Debug",
                    onBackClick = onNavigateBack
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                if (uiState.lastOperationMessage.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            uiState.lastOperationMessage,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // SECTION 1: POSTS
            item { DevSectionHeader("1. Community Posts") }
            item {
                var postBody by remember { mutableStateOf("") }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = postBody,
                        onValueChange = { postBody = it },
                        label = { Text("Post Body") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = { viewModel.createTestPost(postBody, PostKind.TEXT, Visibility.PUBLIC) }) {
                        Text("Create Public Text Post")
                    }
                }
            }

            // SECTION 4: MARKETPLACE
            item { DevSectionHeader("4. Marketplace Listings") }
            item {
                var listingTitle by remember { mutableStateOf("") }
                var price by remember { mutableStateOf("10.0") }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = listingTitle,
                        onValueChange = { listingTitle = it },
                        label = { Text("Listing Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Price") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = { 
                        viewModel.createTestListing(listingTitle, price.toDoubleOrNull() ?: 0.0, ListingCategory.EQUIPMENT) 
                    }) {
                        Text("Create Equipment Listing")
                    }
                }
            }

            // SECTION 5: SALE SIMULATION
            item { DevSectionHeader("5. Sale Simulation") }
            item {
                var saleListingId by remember { mutableStateOf("") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = saleListingId,
                        onValueChange = { saleListingId = it },
                        label = { Text("Listing ID") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = { viewModel.simulateSale(saleListingId, 15.0) }) {
                        Text("Simulate")
                    }
                }
            }

            // SECTION 10: EXPERTISE & PROGRESSION
            item { DevSectionHeader("10. Expertise & Reputation") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.calculateExpertise() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Calculate Current User Expertise")
                    }
                    if (uiState.expertiseResult.isNotEmpty()) {
                        Text(uiState.expertiseResult, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // SECTION 11: SHAREABLE CARDS
            item { DevSectionHeader("11. Shareable Cards") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.simulateShareCard(com.example.hatchtracker.domain.model.CardType.HATCH_RESULT) }) {
                            Text("Hatch Card")
                        }
                    }
                    uiState.shareCardPreview?.let { card ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(card.title, fontWeight = FontWeight.Bold)
                                Text(card.subtitle)
                                card.stats.forEach { stat ->
                                    Text("${stat.label}: ${stat.value}", style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(card.brandingText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // SECTION 12: SMART ROUTING
            item { DevSectionHeader("12. Smart Question Routing") }
            item {
                var questionText by remember { mutableStateOf("") }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = questionText,
                        onValueChange = { questionText = it },
                        label = { Text("Draft Question") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = { viewModel.testRouting(questionText) }) {
                        Text("Analyze & Route")
                    }
                    if (uiState.routingResult.isNotEmpty()) {
                        Text(uiState.routingResult, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // SECTION 8: PROJECTION INSPECTOR
            item { DevSectionHeader("8. Community Projection Inspector") }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Data Pipeline Trace", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        ProjectionStep("Source Entity", "Bird #823 (Active, Gen F4)")
                        ProjectionArrow()
                        ProjectionStep("Snapshot", "EntityPassportSnapshot v2")
                        ProjectionArrow()
                        ProjectionStep("Projection", "AuthorSnapshot (Reputation: ${uiState.reputationSignals?.let { "Scored" } ?: "Pending"})")
                        ProjectionArrow()
                        ProjectionStep("Rendering", "FeedCard (Verified Badge: Visible)")
                    }
                }
            }

            // SECTION 13: DAILY INSIGHTS
            item { DevSectionHeader("13. Daily HatchBase Insights") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.triggerDailyInsights("2026-03-06") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading
                    ) {
                        Text(if (uiState.isLoading) "Generating..." else "Trigger Global Batch (Today)")
                    }
                    
                    OutlinedButton(
                        onClick = { viewModel.previewLowConfidenceInsight() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading
                    ) {
                        Text("Preview Low Confidence Tone")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Visual Microinteractions", style = MaterialTheme.typography.labelMedium)
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.triggerPulseSimulation() },
                            modifier = Modifier.weight(1f),
                            shape = CircleShape
                        ) {
                            Text("Sim: Card Pulse", style = MaterialTheme.typography.labelSmall)
                        }
                        Button(
                            onClick = { viewModel.triggerNudgeSimulation() },
                            modifier = Modifier.weight(1f),
                            shape = CircleShape
                        ) {
                            Text("Sim: Button Nudge", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Simulation: Reduced Motion", style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = uiState.isReducedMotionMode,
                            onCheckedChange = { viewModel.toggleReducedMotion() }
                        )
                    }

                    // Isolated Visual Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), MaterialTheme.shapes.medium),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text("Isolated Visualization Preview", style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Pulse Preview
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                   HatchyInsightPulse(
                                       insightId = uiState.pulseKey, 
                                       forceReducedMotion = uiState.isReducedMotionMode
                                   ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                painter = painterResource(id = UiR.drawable.hatchy_1),
                                                contentDescription = null,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                   }
                                   Text("Card Pulse", style = MaterialTheme.typography.labelSmall)
                                }

                                // Nudge Preview
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    HatchyAssistButton(
                                        onClick = { },
                                        nudgeKey = uiState.nudgeKey,
                                        forceReducedMotion = uiState.isReducedMotionMode,
                                        bottomPadding = 0.dp // Reset for preview
                                    )
                                    Text("Assist Nudge", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    
                    uiState.insightGenerationResult?.let { result ->
                        Text("Batch Summary", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        Text("Insights: ${result.batch.totalInsightCount} | Window: ${result.batch.sourceWindowStart} - ${result.batch.sourceWindowEnd}", style = MaterialTheme.typography.bodySmall)
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("1. Generated Insights", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        result.insights.forEach { insight ->
                            Card(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(insight.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text(insight.body, style = MaterialTheme.typography.bodySmall)
                                    Text("Rule: ${insight.ruleId} | Score: ${String.format("%.2f", insight.rankingScore)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("2. Suppressed / Filtered", fontWeight = FontWeight.Bold, color = Color.Red)
                        result.suppressed.forEach { sup ->
                            Text("• ${sup.ruleId}: ${sup.reason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("3. Raw Candidates", fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text("Total candidates considered: ${result.candidates.size}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun ProjectionStep(label: String, value: String) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(MaterialTheme.colorScheme.primary, shape = androidx.compose.foundation.shape.CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ProjectionArrow() {
    Box(
        modifier = Modifier
            .padding(start = 3.dp)
            .width(2.dp)
            .height(16.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
fun DevSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
