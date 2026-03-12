package com.example.hatchtracker.domain.usecase

import com.example.hatchtracker.domain.repo.FlockRepository
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetFlockSummary @Inject constructor(
    private val repository: FlockRepository
) {
    operator fun invoke() = repository.allActiveFlocks.map { list ->
        "Total active flocks: ${list.size}"
    }
}
