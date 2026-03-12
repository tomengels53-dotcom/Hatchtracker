package com.example.hatchtracker.feature.bird

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hatchtracker.data.models.Bird
import com.example.hatchtracker.data.repository.BirdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BirdListViewModel @Inject constructor(
    private val repository: BirdRepository
) : ViewModel() {

    val allBirds: StateFlow<List<Bird>> = repository.allBirds
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}




