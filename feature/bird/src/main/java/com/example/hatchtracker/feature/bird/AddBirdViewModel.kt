package com.example.hatchtracker.feature.bird

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.data.models.Breed
import com.example.hatchtracker.data.repository.BreedRepository
import com.example.hatchtracker.data.service.BirdLifecycleService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.hatchtracker.billing.SubscriptionStateManager
import com.example.hatchtracker.data.models.Sex
import kotlinx.coroutines.flow.update

data class BirdFormState(
    val species: String = "",
    val speciesId: String? = null,
    val breed: String = "",
    val sex: Sex = Sex.UNKNOWN,
    val hatchDate: String = "",
    val ringNumber: String = "",
    val color: String = "",
    val notes: String = "",
    val imagePath: String? = null
)

@HiltViewModel
class AddBirdViewModel @Inject constructor(
    private val userRepository: com.example.hatchtracker.data.repository.UserRepository,
    private val subscriptionManager: SubscriptionStateManager,
    private val breedRepository: BreedRepository,
    private val lifecycleService: BirdLifecycleService,
    private val flockRepository: com.example.hatchtracker.data.repository.FlockRepository,
    private val profileImageRepository: com.example.hatchtracker.core.data.repository.ProfileImageRepository
) : ViewModel() {
    private val _formState = MutableStateFlow(BirdFormState())
    val formState: StateFlow<BirdFormState> = _formState.asStateFlow()

    private val _selectedSpeciesId = MutableStateFlow<String?>(null)
    val selectedSpeciesId: StateFlow<String?> = _selectedSpeciesId.asStateFlow()

    private val _breeds = MutableStateFlow<List<Breed>>(emptyList())
    val breeds: StateFlow<List<Breed>> = _breeds.asStateFlow()

    private val _isLoadingBreeds = MutableStateFlow(false)
    val isLoadingBreeds: StateFlow<Boolean> = _isLoadingBreeds.asStateFlow()

    private val _flock = MutableStateFlow<com.example.hatchtracker.data.models.Flock?>(null)
    val flock: StateFlow<com.example.hatchtracker.data.models.Flock?> = _flock.asStateFlow()

    val userProfile = userRepository.userProfile
    private val currentCapabilities = subscriptionManager.currentCapabilities
    
    /**
     * Get all species with gating information based on current subscription tier.
     */
    fun getSpeciesUiRows(): List<com.example.hatchtracker.domain.util.SpeciesUiRow> {
        val caps = currentCapabilities.value
        val profile = userProfile.value
        val isAdminOrDev = profile?.isDeveloper == true || profile?.isSystemAdmin == true
        
        return com.example.hatchtracker.domain.util.SpeciesGatingHelper.getSpeciesUiRows(
            tier = caps.tier,
            isAdminOrDeveloper = isAdminOrDev
        )
    }
    
    /**
     * Check if a species can be used with the current subscription tier.
     */
    fun canUseSpecies(speciesId: String): Boolean {
        val caps = currentCapabilities.value
        val profile = userProfile.value
        val isAdminOrDev = profile?.isDeveloper == true || profile?.isSystemAdmin == true
        
        return com.example.hatchtracker.domain.util.SpeciesGatingHelper.canUseSpecies(
            speciesId = speciesId,
            tier = caps.tier,
            isAdminOrDeveloper = isAdminOrDev
        )
    }

    /**
     * Check if bird count in flock is within tier limits.
     */
    fun canAddBirdsToFlock(currentCount: Int, birdsToAdd: Int = 1): Boolean {
        return currentCount + birdsToAdd <= currentCapabilities.value.maxBirdsPerFlock
    }


    fun selectSpecies(speciesId: String, speciesName: String) {
        _selectedSpeciesId.value = speciesId
        _formState.update { it.copy(speciesId = speciesId, species = speciesName, breed = "") }
        fetchBreeds(speciesId)
    }

    fun updateBreed(breed: String) {
        _formState.update { it.copy(breed = breed) }
    }

    fun updateSex(sex: Sex) {
        _formState.update { it.copy(sex = sex) }
    }

    fun updateHatchDate(date: String) {
        _formState.update { it.copy(hatchDate = date) }
    }

    fun updateRingNumber(ringNumber: String) {
        _formState.update { it.copy(ringNumber = ringNumber) }
    }

    fun updateColor(color: String) {
        _formState.update { it.copy(color = color) }
    }

    fun updateNotes(notes: String) {
        _formState.update { it.copy(notes = notes) }
    }

    fun updateImagePath(path: String?) {
        _formState.update { it.copy(imagePath = path) }
    }

    fun clearForm() {
        _formState.value = BirdFormState()
    }

    private fun fetchBreeds(speciesId: String) {
        viewModelScope.launch {
            _isLoadingBreeds.value = true
            breedRepository.getBreedsForSpecies(speciesId).collectLatest {
                _breeds.value = it
                _isLoadingBreeds.value = false
            }
        }
    }

    fun saveBird(bird: com.example.hatchtracker.data.models.Bird, onSuccess: () -> Unit) {
        viewModelScope.launch {
            lifecycleService.addManualBird(bird)
            onSuccess()
        }
    }

    fun saveBirds(birds: List<com.example.hatchtracker.data.models.Bird>, onSuccess: () -> Unit) {
        if (birds.isEmpty()) return
        viewModelScope.launch {
            lifecycleService.addManualBirds(birds)
            onSuccess()
        }
    }

    fun loadFlock(flockId: Long) {
        viewModelScope.launch {
            val f = flockRepository.getFlockById(flockId)
            _flock.value = f
        }
    }

    suspend fun saveProfilePhoto(uri: android.net.Uri): Result<String?> {
        return profileImageRepository.saveProfilePhoto(uri)
    }

    fun deleteOldPhoto(path: String?) {
        profileImageRepository.deleteOldPhoto(path)
    }
}




