@file:android.annotation.SuppressLint("NewApi")

package com.example.hatchtracker.feature.incubation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.data.models.Incubation
import com.example.hatchtracker.data.repository.NurseryRepository
import com.example.hatchtracker.NotificationHelper
import com.example.hatchtracker.data.repository.IncubationRepository
import com.example.hatchtracker.data.service.BirdLifecycleService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import com.example.hatchtracker.auth.UserAuthManager
import com.example.hatchtracker.core.featureaccess.FeatureAccessPolicy
import com.example.hatchtracker.core.featureaccess.FeatureKey

@HiltViewModel
class HatchOutcomeViewModel @Inject constructor(
    private val subscriptionStateManager: com.example.hatchtracker.billing.SubscriptionStateManager,
    private val breedingScenarioService: com.example.hatchtracker.data.service.BreedingScenarioService,
    private val nurseryRepository: NurseryRepository,
    private val incubationRepository: IncubationRepository,
    private val lifecycleService: BirdLifecycleService,
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _incubation = MutableStateFlow<Incubation?>(null)
    val incubation: StateFlow<Incubation?> = _incubation

    private val _isFinished = MutableStateFlow(false)
    val isFinished: StateFlow<Boolean> = _isFinished

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadIncubation(id: Long) {
        viewModelScope.launch {
            _incubation.value = incubationRepository.getIncubationById(id)
        }
    }

    fun completeIncubation(incubation: Incubation, hatched: Int, infertile: Int, failed: Int) {
        viewModelScope.launch {
            val result = lifecycleService.completeIncubation(
                incubation.id,
                hatched,
                BirdLifecycleService.IncubationOutcomeMeta(
                    infertileCount = infertile,
                    failedCount = failed,
                    hatchDate = java.time.LocalDate.now()
                )
            )
            if (result.isSuccess) {
                val caps = subscriptionStateManager.currentCapabilities.value
                val canScheduleNursery = FeatureAccessPolicy
                    .canAccess(FeatureKey.NURSERY, caps.tier, UserAuthManager.isSystemAdmin.value || UserAuthManager.isDeveloper.value)
                    .allowed

                val flocklet = nurseryRepository.getFlockletByHatchId(incubation.id)
                if (flocklet != null) {
                    NotificationHelper.scheduleNurseryMilestones(
                        context = appContext,
                        flocklet = flocklet,
                        canSchedule = canScheduleNursery
                    )
                }

                // Trigger scenario evaluation if linked
                if (incubation.sourceScenarioId.isNotEmpty()) {
                    val updatedIncubation = incubation.copy(
                        hatchedCount = hatched,
                        eggsCount = incubation.eggsCount // Explicitly pass for rate calc
                    )
                    breedingScenarioService.finalizeScenarioHatch(updatedIncubation)
                }
                _isFinished.value = true
            } else {
                _error.value = result.exceptionOrNull()?.message
            }
        }
    }

    val capabilities = subscriptionStateManager.currentCapabilities
}






