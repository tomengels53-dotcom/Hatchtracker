package com.example.hatchtracker.feature.breeding

import com.example.hatchtracker.core.ui.theme.bodyEmphasis

import com.example.hatchtracker.feature.breeding.BreedingLockedScreen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import com.example.hatchtracker.data.models.TraitObservation
import com.example.hatchtracker.domain.breeding.CommunityValidationManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.hatchtracker.core.ui.R as UiR

// Simple ViewModel for this screen
class CommunityValidationViewModel : ViewModel() {
    private val db by lazy { FirebaseFirestore.getInstance() }
    
    private val _observations = MutableStateFlow<List<TraitObservation>>(emptyList())
    val observations = _observations.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        fetchObservations()
    }

    fun fetchObservations() {
        Logger.d(LogTags.BREEDING, "Fetching observations...")
        _isLoading.value = true
        // For demo, fetching all. In real app, filter by breeds user owns or hasn't voted on.
        db.collection("traitObservations")
            .limit(50)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.toObjects(TraitObservation::class.java)
                Logger.d(LogTags.BREEDING, "Fetched ${list.size} observations")
                _observations.value = list
                _isLoading.value = false
            }
            .addOnFailureListener {
                Logger.e(LogTags.BREEDING, "Error fetching", it)
                _isLoading.value = false
            }
    }
    
    fun vote(observationId: String, agree: Boolean) {
        // Optimistic update or fire-and-forget for UI responsiveness
        viewModelScope.launch {
             CommunityValidationManager.voteOnObservation(observationId, agree)
             // In real app, remove from list or show "Voted" state
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityValidationScreen(
    onBack: () -> Unit,
    canAccessBreeding: Boolean,
    onNavigateToPaywall: () -> Unit = {},
    viewModel: CommunityValidationViewModel = viewModel()
) {
    // Check PRO access
    if (!canAccessBreeding) {
        BreedingLockedScreen(
            onBack = onBack,
            onViewPlans = onNavigateToPaywall
        )
        return
    }

    val observations by viewModel.observations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUser = com.example.hatchtracker.auth.UserAuthManager.currentUser.value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(UiR.string.community_validation_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(UiR.string.back_action))
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
             LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (observations.isEmpty()) {
                    item {
                        Text(stringResource(UiR.string.no_pending_observations), style = MaterialTheme.typography.bodyLarge)
                    }
                }
                
                items(observations) { observation ->
                    // Don't show own observations for voting (or show as read-only)
                    val isOwn = observation.userId == currentUser?.uid
                    
                    ObservationVoteCard(
                        observation = observation,
                        isOwn = isOwn,
                        onVote = { agree -> viewModel.vote(observation.id, agree) }
                    )
                }
            }
        }
    }
}

@Composable
fun ObservationVoteCard(
    observation: TraitObservation,
    isOwn: Boolean,
    onVote: (Boolean) -> Unit
) {
    var voted by remember { mutableStateOf(false) }

    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(observation.breedId, style = MaterialTheme.typography.titleMedium)
                Text(
                     java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(java.util.Date(observation.timestamp)),
                     style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(stringResource(UiR.string.trait_prefix, observation.traitId), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(UiR.string.observed_prefix, observation.observedValue), style = androidx.compose.material3.MaterialTheme.typography.bodyEmphasis)
            
            if (!observation.environmentalNotes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(stringResource(UiR.string.notes_prefix, observation.environmentalNotes.orEmpty()), style = MaterialTheme.typography.bodySmall, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isOwn) {
                Text(
                    stringResource(UiR.string.your_submission),
                    style = MaterialTheme.typography.labelMedium, 
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (voted) {
                 Text(
                    stringResource(UiR.string.vote_recorded),
                    style = MaterialTheme.typography.labelMedium, 
                    color = MaterialTheme.colorScheme.secondary
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { 
                            onVote(true) 
                            voted = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                    ) {
                        Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(UiR.string.action_confirm))
                    }
                    
                    Button(
                        onClick = { 
                            onVote(false) 
                            voted = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                    ) {
                        Icon(Icons.Default.ThumbDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(UiR.string.dispute_action))
                    }
                }
            }
        }
    }
}











