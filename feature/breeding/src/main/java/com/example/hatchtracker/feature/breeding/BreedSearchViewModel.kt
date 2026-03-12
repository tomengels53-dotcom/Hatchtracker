package com.example.hatchtracker.feature.breeding

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.data.models.BreedStandard
import com.example.hatchtracker.data.repository.BreedStandardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for searching and ranking breed standards.
 */
@HiltViewModel
class BreedSearchViewModel @Inject constructor(
    private val breedStandardRepository: BreedStandardRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedSpecies = MutableStateFlow<String?>(null)
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _selectedEggColor = MutableStateFlow<String?>(null)
    private val _userClimate = MutableStateFlow("cold") // Default or fetched from settings

    private val _allBreeds = MutableStateFlow<List<BreedStandard>>(emptyList())

    // Frequency map for local selection counts
    private val _selectionFrequency = MutableStateFlow<Map<String, Int>>(emptyMap())

    val filteredBreeds: StateFlow<List<BreedStandard>> = combine(
        _allBreeds,
        _searchQuery,
        _selectedSpecies,
        _selectedCategory,
        _selectedEggColor,
        _userClimate,
        _selectionFrequency
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        val all = array[0] as List<BreedStandard>
        val query = array[1] as String
        val species = array[2] as String?
        val category = array[3] as String?
        val eggColor = array[4] as String?
        val climate = array[5] as String
        @Suppress("UNCHECKED_CAST")
        val frequency = array[6] as Map<String, Int>

        all.filter { breed ->
            (species == null || breed.species.equals(species, ignoreCase = true)) &&
                (category == null || breed.category?.equals(category, ignoreCase = true) == true) &&
                (eggColor == null || breed.eggColor.contains(eggColor, ignoreCase = true)) &&
                (query.isBlank() || breed.name.contains(query, ignoreCase = true) ||
                    breed.origin.contains(query, ignoreCase = true) ||
                    breed.eggColor.contains(query, ignoreCase = true))
        }.map { breed ->
            breed to calculateRank(breed, query, climate, frequency[breed.id] ?: 0)
        }.sortedByDescending { it.second }
            .map { it.first }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadBreeds()
    }

    private fun loadBreeds() {
        val breeds = breedStandardRepository.getAllBreeds()
        _allBreeds.value = breeds
        Log.i("BreedSearchViewModel", "Loaded ${breeds.size} breed standards")
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setFilters(species: String?, category: String?, eggColor: String?) {
        _selectedSpecies.value = species
        _selectedCategory.value = category
        _selectedEggColor.value = eggColor
    }

    fun setUserClimate(climate: String) {
        _userClimate.value = climate
    }

    /**
     * Record a selection to boost its rank in the future.
     */
    fun recordSelection(breedId: String) {
        val current = _selectionFrequency.value.toMutableMap()
        current[breedId] = (current[breedId] ?: 0) + 1
        _selectionFrequency.value = current
    }

    private fun calculateRank(breed: BreedStandard, query: String, userClimate: String, frequency: Int): Int {
        var rank = 0

        // Exact name match
        if (query.isNotBlank() && breed.name.equals(query, ignoreCase = true)) {
            rank += 100
        } else if (query.isNotBlank() && breed.name.contains(query, ignoreCase = true)) {
            rank += 50
        }

        // Climate match
        val traits = breed.geneticProfile.fixedTraits + breed.geneticProfile.inferredTraits
        if (traits.any { it.contains(userClimate, ignoreCase = true) || it.contains("hardy", ignoreCase = true) }) {
            rank += 30
        }

        // Official status
        if (breed.official) {
            rank += 10
        }

        // Selection frequency
        rank += frequency * 5

        return rank
    }
}
