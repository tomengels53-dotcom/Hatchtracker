package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.model.GeneticSource
import com.example.hatchtracker.model.Species
import kotlinx.coroutines.flow.Flow

/**
 * Domain interface for providing the candidate population for breeding planning.
 */
interface PopulationProvider {
    /**
     * Returns active birds/sources of the specified species from the selected flocks.
     */
    fun getEligibleSources(
        species: Species,
        selectedFlockIds: List<String>
    ): Flow<List<GeneticSource>>

    fun getOwnedSources(species: Species, selectedFlockIds: List<String>): Flow<List<GeneticSource>>
    
    fun getGlobalBreedSources(species: Species): Flow<List<GeneticSource>>
}

