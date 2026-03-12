package com.example.hatchtracker.feature.breeding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.data.models.BreedStandard
import com.example.hatchtracker.data.repository.BreedStandardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BreedSelectionViewModel @Inject constructor(
    private val breedStandardRepository: BreedStandardRepository
) : ViewModel() {

    private val _breeds = MutableStateFlow<List<BreedStandard>>(emptyList())
    val breeds: StateFlow<List<BreedStandard>> = _breeds.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var allBreeds: List<BreedStandard> = emptyList()

    fun loadBreeds(speciesId: String) {
        viewModelScope.launch {
            allBreeds = breedStandardRepository.getBreedsForSpecies(speciesId)
            filterBreeds()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterBreeds()
    }

    private fun filterBreeds() {
        val query = _searchQuery.value.trim()
        _breeds.value = if (query.isEmpty()) {
            allBreeds
        } else {
            allBreeds.filter { it.name.contains(query, ignoreCase = true) }
        }
    }
}


