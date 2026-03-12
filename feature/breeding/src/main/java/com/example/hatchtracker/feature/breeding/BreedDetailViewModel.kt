package com.example.hatchtracker.feature.breeding

import androidx.lifecycle.ViewModel
import com.example.hatchtracker.model.BreedStandard
import com.example.hatchtracker.data.repository.BreedStandardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class BreedDetailViewModel @Inject constructor(
    private val breedStandardRepository: BreedStandardRepository
) : ViewModel() {

    private val _breed = MutableStateFlow<BreedStandard?>(null)
    val breed: StateFlow<BreedStandard?> = _breed.asStateFlow()

    fun loadBreed(breedId: String) {
        _breed.value = breedStandardRepository.getBreedById(breedId)
    }
}
