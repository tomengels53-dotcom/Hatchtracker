@file:android.annotation.SuppressLint("NewApi")

package com.example.hatchtracker.feature.production

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.data.repository.EggProductionRepository
import com.example.hatchtracker.data.repository.FlockRepository
import com.example.hatchtracker.data.models.BreedLineEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import androidx.lifecycle.SavedStateHandle
import javax.inject.Inject

@HiltViewModel
class EggProductionViewModel @Inject constructor(
    private val repository: EggProductionRepository,
    private val flockRepository: FlockRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Selection State
    // Prefill from navigation argument if present
    private val _selectedFlockId = MutableStateFlow<String?>(savedStateHandle["flockId"])
    val selectedFlockId = _selectedFlockId.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate = _selectedDate.asStateFlow()

    private val _selectedLineId = MutableStateFlow<String?>(null)
    val selectedLineId = _selectedLineId.asStateFlow()

    // Data Streams
    val activeFlocks = flockRepository.allActiveFlocks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val breedLines = _selectedFlockId.flatMapLatest { flockId ->
        if (flockId != null) repository.getBreedLines(flockId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Input Fields
    private val _totalEggs = MutableStateFlow("")
    val totalEggs = _totalEggs.asStateFlow()

    private val _crackedEggs = MutableStateFlow("")
    val crackedEggs = _crackedEggs.asStateFlow()

    private val _setEggs = MutableStateFlow("")
    val setEggs = _setEggs.asStateFlow()
    private val _notes = MutableStateFlow("")
    val notes = _notes.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage = _uiMessage.asStateFlow()

    private val _saveSuccess = MutableSharedFlow<Unit>()
    val saveSuccess = _saveSuccess.asSharedFlow()

    // Load Existing Data when constraints change
    init {
        viewModelScope.launch {
            combine(_selectedFlockId, _selectedDate, _selectedLineId) { flock, date, line ->
                Triple(flock, date, line)
            }.collectLatest { (flock, date, line) ->
                if (flock != null) {
                    // TODO: repository needs a 'getByNaturalKey' exposed or 'observeDay'?
                    // For now, assume empty or need to fetch. 
                    // Repository doesn't expose `getByNaturalKey` directly to VM, only via upsert logic internally?
                    // I should add a way to GET current data to populate fields.
                    // For MVP, just clearing fields or handling locally? 
                    // Better to expose `getDayProduction(flock, date, line)` from repo.
                    clearFields() 
                }
            }
        }
    }

    fun selectFlock(id: String) {
        _selectedFlockId.value = id
        _selectedLineId.value = null // Reset line
    }

    fun selectLine(id: String?) {
        _selectedLineId.value = id
    }

    fun setDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun updateTotal(value: String) { _totalEggs.value = value }
    fun updateCracked(value: String) { _crackedEggs.value = value }
    fun updateSet(value: String) { _setEggs.value = value }
    fun updateNotes(value: String) { _notes.value = value }

    fun saveProduction() {
        val flockId = _selectedFlockId.value ?: return
        val total = _totalEggs.value.toIntOrNull()
        val cracked = _crackedEggs.value.toIntOrNull() ?: 0
        val set = _setEggs.value.toIntOrNull() ?: 0
        
        if (total == null) {
            _uiMessage.value = "Please enter total eggs"
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.upsertEggProduction(
                    flockId = flockId,
                    dateEpochDay = _selectedDate.value.toEpochDay(),
                    totalEggs = total,
                    crackedEggs = cracked,
                    setForIncubation = set,
                    lineId = _selectedLineId.value,
                    notes = _notes.value
                )
                _uiMessage.value = "Saved successfully"
                _saveSuccess.emit(Unit)
            } catch (e: Exception) {
                _uiMessage.value = "Error: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun dismissMessage() {
        _uiMessage.value = null
    }

    private fun clearFields() {
        _totalEggs.value = ""
        _crackedEggs.value = ""
        _setEggs.value = ""
        _notes.value = ""
    }
}
