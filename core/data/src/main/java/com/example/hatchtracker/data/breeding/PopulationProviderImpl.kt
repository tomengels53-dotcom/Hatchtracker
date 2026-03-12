package com.example.hatchtracker.data.breeding

import com.example.hatchtracker.data.repository.BirdRepository
import com.example.hatchtracker.data.repository.BreedRepository
import com.example.hatchtracker.domain.breeding.PopulationProvider
import com.example.hatchtracker.model.Bird
import com.example.hatchtracker.model.GeneticSource
import com.example.hatchtracker.model.Species
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PopulationProviderImpl @Inject constructor(
    private val birdRepository: BirdRepository,
    private val breedRepository: BreedRepository
) : PopulationProvider {

    override fun getEligibleSources(
        species: Species,
        selectedFlockIds: List<String>
    ): Flow<List<GeneticSource>> {
        // Mode specific logic will wrap these calls at the service layer
        // For now, return owned birds as a baseline for backwards compatibility
        return getOwnedSources(species, selectedFlockIds)
    }

    override fun getOwnedSources(
        species: Species,
        selectedFlockIds: List<String>
    ): Flow<List<GeneticSource>> {
        return birdRepository.allBirds.map { birds ->
            birds.filter { bird ->
                bird.species == species &&
                bird.status == "active" &&
                (selectedFlockIds.isEmpty() || bird.flockId?.toString() in selectedFlockIds)
            }
        }
    }

    override fun getGlobalBreedSources(species: Species): Flow<List<GeneticSource>> {
        return breedRepository.getBreedsForSpecies(species.name.lowercase()).map { breeds ->
            breeds.map { breed ->
                // Map Breed models to BreedStandard objects for the source adapter
                // Assuming Breed matches BreedStandard closely enough for this MVP 
                // In a production app, we'd fetch the full BreedStandard doc
                BreedStandardSource(
                    com.example.hatchtracker.data.models.BreedStandard(
                        id = breed.id,
                        name = breed.name,
                        species = breed.speciesId
                    )
                )
            }
        }
    }
}

