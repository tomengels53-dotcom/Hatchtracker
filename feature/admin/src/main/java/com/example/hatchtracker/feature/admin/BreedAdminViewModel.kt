package com.example.hatchtracker.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.data.models.BreedStandard
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class BreedAdminViewModel @Inject constructor(
    private val db: FirebaseFirestore
) : ViewModel() {
    private val _uiState = MutableStateFlow(BreedAdminUiState())
    val uiState: StateFlow<BreedAdminUiState> = _uiState.asStateFlow()

    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        observeBreeds()
    }

    private fun observeBreeds() {
        listenerRegistration = db.collection("breedStandards")
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                    return@addSnapshotListener
                }
                
                try {
                    val breedList = snapshot?.toObjects(BreedStandard::class.java) ?: emptyList()
                    _uiState.update { 
                        it.copy(
                            breeds = breedList,
                            isLoading = false
                        )
                    }
                } catch (e: Exception) {
                     android.util.Log.e("BreedAdminViewModel", "Error deserializing breeds", e)
                    _uiState.update { it.copy(error = "Data error: ${e.message}", isLoading = false) }
                }
                applyFilters()
            }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    fun updateSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun updateFilters(species: String?, official: Boolean) {
        _uiState.update { it.copy(speciesFilter = species, officialOnly = official) }
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        val filtered = state.breeds.filter { breed ->
            val matchesSearch = breed.name.contains(state.searchQuery, ignoreCase = true) ||
                                 breed.origin.contains(state.searchQuery, ignoreCase = true)
            val matchesSpecies = state.speciesFilter == null || breed.species == state.speciesFilter
            val matchesOfficial = !state.officialOnly || breed.official
            
            matchesSearch && matchesSpecies && matchesOfficial
        }
        _uiState.update { it.copy(filteredBreeds = filtered) }
    }

    fun initiateSave(breed: BreedStandard, original: BreedStandard? = null) {
        if (breed.species.isBlank() || breed.name.isBlank()) {
            _uiState.update { it.copy(error = "Species and Name are required.") }
            return
        }
        
        _uiState.update { 
            it.copy(
                pendingBreed = breed, 
                originalBreed = original,
                showConfirmation = true 
            ) 
        }
    }

    fun dismissConfirmation() {
        _uiState.update { it.copy(showConfirmation = false, pendingBreed = null) }
    }

    fun confirmSave() {
        val pending = _uiState.value.pendingBreed ?: return
        val original = _uiState.value.originalBreed
        
        _uiState.update { it.copy(isSaving = true, showConfirmation = false) }

        viewModelScope.launch {
            try {
                val docRef = if (pending.id.isBlank()) {
                    db.collection("breedStandards").document()
                } else {
                    db.collection("breedStandards").document(pending.id)
                }
                
                val finalBreed = pending.copy(id = docRef.id, lastUpdated = System.currentTimeMillis())
                
                db.runBatch { batch ->
                    batch.set(docRef, finalBreed)
                    
                    com.example.hatchtracker.data.audit.AuditLogger.logActionToBatch(
                        batch = batch,
                        actionType = if (original == null) com.example.hatchtracker.model.AuditActionType.CREATE else com.example.hatchtracker.model.AuditActionType.UPDATE,
                        targetCollection = "breedStandards",
                        targetDocumentId = docRef.id,
                        before = original,
                        after = finalBreed,
                        reason = if (original == null) "Created via Admin UI" else "Updated via Admin UI"
                    )
                }.await()

                _uiState.update { it.copy(isSaving = false, pendingBreed = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isSaving = false) }
            }
        }
    }

    fun softDelete(breed: BreedStandard) {
        viewModelScope.launch {
            try {
                val docRef = db.collection("breedStandards").document(breed.id)
                val updatedBreed = breed.copy(
                    tags = breed.tags + "deprecated",
                    lastUpdated = System.currentTimeMillis()
                )
                
                db.runBatch { batch ->
                    batch.set(docRef, updatedBreed)
                    
                    com.example.hatchtracker.data.audit.AuditLogger.logActionToBatch(
                        batch = batch,
                        actionType = com.example.hatchtracker.model.AuditActionType.DEPRECATE,
                        targetCollection = "breedStandards",
                        targetDocumentId = breed.id,
                        before = breed,
                        after = updatedBreed,
                        reason = "Deprecated via Admin UI"
                    )
                }.await()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

@androidx.compose.runtime.Immutable
data class BreedAdminUiState(
    val breeds: List<BreedStandard> = emptyList(),
    val filteredBreeds: List<BreedStandard> = emptyList(),
    val searchQuery: String = "",
    val speciesFilter: String? = null,
    val officialOnly: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isSaving: Boolean = false,
    val showConfirmation: Boolean = false,
    val pendingBreed: BreedStandard? = null,
    val originalBreed: BreedStandard? = null
)
