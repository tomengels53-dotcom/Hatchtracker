package com.example.hatchtracker.feature.breeding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.data.models.Bird
import com.example.hatchtracker.data.models.TraitPromotion
import com.example.hatchtracker.data.repository.BirdRepository
import com.example.hatchtracker.data.repository.TraitPromotionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TraitPromotionViewModel @Inject constructor(
    private val birdRepository: BirdRepository,
    private val traitPromotionRepository: TraitPromotionRepository,
    private val traitPromotionManager: com.example.hatchtracker.domain.logic.TraitPromotionManager
) : ViewModel() {

    private val _birdsWithInferredTraits = MutableStateFlow<List<Bird>>(emptyList())
    val birdsWithInferredTraits: StateFlow<List<Bird>> = _birdsWithInferredTraits.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<TraitPromotion>>(emptyList())
    val auditLogs: StateFlow<List<TraitPromotion>> = _auditLogs.asStateFlow()

    init {
        refreshBirds()
        observeAuditLogs()
    }

    fun refreshBirds() {
        viewModelScope.launch {
            val allBirds = birdRepository.getAllBirds().first()
            _birdsWithInferredTraits.value = allBirds.filter { it.geneticProfile.inferredTraits.isNotEmpty() }
        }
    }

    private fun observeAuditLogs() {
        viewModelScope.launch {
            traitPromotionRepository.getAllPromotionsFlow()
                .catch { e ->
                    // Log error and emit empty, preventing crash
                    android.util.Log.e("TraitPromotionViewModel", "Error fetching promotions", e)
                    emit(emptyList()) 
                }
                .collect {
                    _auditLogs.value = it
                }
        }
    }

    fun promoteTrait(bird: Bird, trait: String, adminId: String, reason: String? = null) {
        viewModelScope.launch {
            val result = traitPromotionManager.promoteTrait(bird, trait, adminId, reason)
            if (result.isSuccess) {
                refreshBirds()
            }
        }
    }
}
