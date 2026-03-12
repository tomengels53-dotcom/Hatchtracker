package com.example.hatchtracker.feature.admin

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import com.example.hatchtracker.data.models.TraitConfidence
import com.example.hatchtracker.data.models.TraitStatus
import kotlinx.coroutines.launch

@HiltViewModel
class AdminTraitDashboardViewModel @Inject constructor(
    private val breedStandardRepository: com.example.hatchtracker.data.repository.BreedStandardRepository,
    private val traitPromotionRepository: com.example.hatchtracker.data.repository.TraitPromotionRepository
) : ViewModel() {
    
    private val _pendingPromotions = MutableStateFlow<List<TraitConfidence>>(emptyList())
    val pendingPromotions = _pendingPromotions.asStateFlow()

    init {
        loadCandidates()
    }

    private fun loadCandidates() {
        _pendingPromotions.value = breedStandardRepository.getHighConfidenceTraits()
    }

    fun promoteTrait(breedId: String, traitId: String) {
        Logger.d(LogTags.BREEDING, "Promoting $traitId for $breedId")
        
        viewModelScope.launch {
            val breed = breedStandardRepository.getBreedById(breedId) ?: return@launch
            
            // 1. Update Breed Standard Genetic Profile
            val oldProfile = breed.geneticProfile
            val updatedProfile = oldProfile.copy(
                inferredTraits = oldProfile.inferredTraits.filter { it != traitId },
                fixedTraits = (oldProfile.fixedTraits + traitId).distinct()
            )
            val updatedBreed = breed.copy(geneticProfile = updatedProfile, official = true)
            
            breedStandardRepository.updateBreedStandard(updatedBreed)

            // 2. Audit Log
            com.example.hatchtracker.data.audit.AuditLogger.logAction(
                actionType = com.example.hatchtracker.model.AuditActionType.PROMOTE,
                targetCollection = "breedStandards",
                targetDocumentId = breedId,
                reason = "Trait Promotion: $traitId (Promoted from Inferred)",
                before = oldProfile,
                after = updatedProfile
            )
            
            // 3. Update local list (Remove confirmed candidate)
            _pendingPromotions.value = _pendingPromotions.value.filter { it.traitId != traitId || it.breedId != breedId }
        }
    }
    
    fun rejectTrait(breedId: String, traitId: String) {
        Logger.d(LogTags.BREEDING, "Rejecting $traitId for $breedId")
        viewModelScope.launch {
            // Log rejection
             com.example.hatchtracker.data.audit.AuditLogger.logAction(
                actionType = com.example.hatchtracker.model.AuditActionType.UPDATE,
                targetCollection = "traitConfidence",
                targetDocumentId = "${breedId}_${traitId}",
                reason = "Admin Rejection: Trait did not meet standards for promotion.",
                after = mapOf("status" to "REJECTED")
            )
            
            // Remove from list
            _pendingPromotions.value = _pendingPromotions.value.filter { it.traitId != traitId || it.breedId != breedId }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTraitDashboard(
    onBack: () -> Unit,
    viewModel: AdminTraitDashboardViewModel = hiltViewModel()
) {
    val promotions by viewModel.pendingPromotions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trait Promotions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text(
                "High Confidence Candidates", 
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (promotions.isEmpty()) {
                Text("No traits currently meet the promotion threshold.")
            }
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(promotions) { item ->
                    PromotionCard(
                        item = item,
                        onPromote = { viewModel.promoteTrait(item.breedId, item.traitId) },
                        onReject = { viewModel.rejectTrait(item.breedId, item.traitId) }
                    )
                }
            }
        }
    }
}

@Composable
fun PromotionCard(
    item: TraitConfidence,
    onPromote: () -> Unit,
    onReject: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    // Mock data for demo - in real app would come from item.events or separate fetch
    val mockEvents = remember {
        listOf(
            com.example.hatchtracker.data.models.ConfidenceEvent(System.currentTimeMillis() - 86400000 * 10, 20.0, "Initial"),
            com.example.hatchtracker.data.models.ConfidenceEvent(System.currentTimeMillis() - 86400000 * 5, 30.0, "Votes"),
            com.example.hatchtracker.data.models.ConfidenceEvent(System.currentTimeMillis(), 35.0, "Recent")
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(item.breedId, style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
                Badge { Text("Score: ${item.currentScore.toInt()}") }
            }
            Text("Candidate Trait: ${item.traitId}")
            
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                com.example.hatchtracker.core.ui.components.ConfidenceTrendGraph(
                    events = mockEvents, // Using mock events for demo visualization
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide History" else "View Trend")
                }
                
                Row {
                    OutlinedButton(onClick = onReject) {
                        Text("Reject")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onPromote) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Promote")
                    }
                }
            }
        }
    }
}



