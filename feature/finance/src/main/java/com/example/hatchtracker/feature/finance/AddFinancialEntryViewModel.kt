package com.example.hatchtracker.feature.finance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.data.models.FinancialCategory
import com.example.hatchtracker.data.models.FinancialEntry
import com.example.hatchtracker.data.repository.FinancialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddFinancialEntryViewModel @Inject constructor(
    private val repository: FinancialRepository,
    savedStateHandle: SavedStateHandle,
    private val userRepository: com.example.hatchtracker.data.repository.UserRepository,
    private val flockRepository: com.example.hatchtracker.data.repository.FlockRepository,
    private val incubationRepository: com.example.hatchtracker.data.repository.IncubationRepository,
    private val nurseryRepository: com.example.hatchtracker.data.repository.NurseryRepository
) : ViewModel() {

    val ownerId: String = checkNotNull(savedStateHandle["ownerId"])
    val ownerType: String = checkNotNull(savedStateHandle["ownerType"])
    val isRevenue: Boolean = checkNotNull(savedStateHandle["isRevenue"])
    val isSharedMode: Boolean = ownerId == "shared"

    private val _uiState = MutableStateFlow(AddFinancialEntryUiState())
    val currencyCode: StateFlow<String> = userRepository.userProfile
        .map { it?.currencyCode ?: "USD" }
        .stateIn(
            viewModelScope, 
            SharingStarted.WhileSubscribed(5000), 
            "USD"
        )

    val uiState = _uiState.asStateFlow()

    val userProfile = userRepository.userProfile

    // Beneficiary Logic
    data class Beneficiary(val id: String, val name: String, val isSelected: Boolean = true)
    
    private val _beneficiaries = MutableStateFlow<List<Beneficiary>>(emptyList())
    val beneficiaries = _beneficiaries.asStateFlow()

    init {
        // Set default category based on context
        val defaultCategory = when (ownerType) {
            "flocklet" -> if (isRevenue) FinancialCategory.SALE_CHICKS else FinancialCategory.FEED
            "incubation" -> if (isRevenue) FinancialCategory.SALE_CHICKS else FinancialCategory.PURCHASE_EGGS
            "flock" -> if (isRevenue) FinancialCategory.SALE_EGGS else FinancialCategory.FEED
            else -> FinancialCategory.OTHER
        }
        _uiState.value = _uiState.value.copy(category = defaultCategory)

        if (isSharedMode) {
            loadBeneficiaries()
        }
    }

    private fun loadBeneficiaries() {
        viewModelScope.launch {
            val list = when (ownerType) {
                "flock" -> {
                    flockRepository.allActiveFlocks.first().map { 
                        Beneficiary(it.syncId, it.name) 
                    }
                }
                "incubation" -> {
                    val activeIncubations = incubationRepository.allIncubations.first()
                        .filter { !it.hatchCompleted && !it.deleted }
                    activeIncubations.map { 
                         // Use start date as name differentiator if needed, or species
                        Beneficiary(it.syncId, "${it.species} Batch (${it.startDate})") 
                    }
                }
                "flocklet" -> {
                    nurseryRepository.activeFlocklets.first().map {
                        Beneficiary(it.syncId, "${it.species} Chicks (${it.chickCount})")
                    }
                }
                else -> emptyList()
            }
            _beneficiaries.value = list
        }
    }

    fun toggleBeneficiary(id: String) {
        val current = _beneficiaries.value
        _beneficiaries.value = current.map { 
            if (it.id == id) it.copy(isSelected = !it.isSelected) else it 
        }
    }

    fun toggleAllBeneficiaries(selected: Boolean) {
        val current = _beneficiaries.value
        _beneficiaries.value = current.map { it.copy(isSelected = selected) }
    }

    fun onAmountChange(amount: String) {
        _uiState.value = _uiState.value.copy(amount = amount)
    }

    fun onQuantityChange(quantity: String) {
        _uiState.value = _uiState.value.copy(quantity = quantity)
    }

    fun onCategoryChange(category: FinancialCategory) {
        val isBlocked = category.name.startsWith("SALE_")
        _uiState.value = _uiState.value.copy(
            category = category,
            isSaleBlocked = isBlocked
        )
    }

    fun onNotesChange(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun onDateChange(date: Long) {
        _uiState.value = _uiState.value.copy(date = date)
    }

    fun onVendorChange(vendor: String) {
        val currentNotes = _uiState.value.notes
        if (!currentNotes.contains(vendor)) {
            _uiState.value = _uiState.value.copy(notes = if (currentNotes.isEmpty()) "Vendor: $vendor" else "$currentNotes\nVendor: $vendor")
        }
    }

    fun saveEntry(onSuccess: () -> Unit) {
        if (_uiState.value.isSaleBlocked) return

        val amountValue = _uiState.value.amount.toDoubleOrNull() ?: 0.0
        val quantityValue = _uiState.value.quantity.toIntOrNull() ?: 1
        
        if (amountValue <= 0) return

        viewModelScope.launch {
            if (isSharedMode) {
                val selected = _beneficiaries.value.filter { it.isSelected }
                if (selected.isNotEmpty()) {
                    val totalAmount = amountValue * quantityValue
                    val splitAmount = totalAmount / selected.size
                    
                    selected.forEach { beneficiary ->
                        val entry = FinancialEntry(
                            syncId = UUID.randomUUID().toString(),
                            ownerId = beneficiary.id,
                            ownerType = ownerType, 
                            type = if (isRevenue) "revenue" else "cost",
                            category = _uiState.value.category.value,
                            amount = splitAmount,
                            amountGross = splitAmount,
                            quantity = 1, // Quantity is 1 per beneficiary in split mode
                            date = _uiState.value.date,
                            notes = _uiState.value.notes + " (Split from shared cost)"
                        )
                        repository.addEntry(entry)
                    }
                }
            } else {
                val totalAmount = amountValue * quantityValue
                val entry = FinancialEntry(
                    syncId = UUID.randomUUID().toString(),
                    ownerId = ownerId,
                    ownerType = ownerType,
                    type = if (isRevenue) "revenue" else "cost",
                    category = _uiState.value.category.value,
                    amount = totalAmount,
                    amountGross = totalAmount,
                    quantity = quantityValue,
                    date = _uiState.value.date,
                    notes = _uiState.value.notes
                )
                repository.addEntry(entry)
            }
            onSuccess()
        }
    }
}

data class AddFinancialEntryUiState(
    val amount: String = "",
    val quantity: String = "1",
    val category: FinancialCategory = FinancialCategory.OTHER,
    val notes: String = "",
    val date: Long = System.currentTimeMillis(),
    val isSaleBlocked: Boolean = false
)
