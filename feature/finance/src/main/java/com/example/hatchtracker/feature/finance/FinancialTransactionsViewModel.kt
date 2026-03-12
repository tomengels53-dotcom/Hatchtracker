package com.example.hatchtracker.feature.finance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.data.models.FinancialEntry
import com.example.hatchtracker.data.repository.FinancialRepository
import com.example.hatchtracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FinancialTransactionsViewModel @Inject constructor(
    private val repository: FinancialRepository,
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository
) : ViewModel() {

    val ownerId: String = checkNotNull(savedStateHandle["ownerId"])
    val ownerType: String = checkNotNull(savedStateHandle["ownerType"])

    val currencyCode: StateFlow<String> = userRepository.userProfile
        .map { it?.currencyCode ?: "USD" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "USD")

    val transactions: StateFlow<List<FinancialEntry>> = repository.getEntriesForOwner(ownerId, ownerType)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteEntry(entry: FinancialEntry) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
        }
    }
}
