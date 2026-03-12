package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.BreedStandard

/**
 * Domain-level interface for accessing breed standards.
 * Allows Hatchy resolvers to query breed data without depending on the concrete data layer.
 */
interface IBreedStandardRepository {
    fun getAllBreeds(): List<BreedStandard>
    fun getBreedById(id: String): BreedStandard?
    fun getBreedsForSpecies(species: String): List<BreedStandard>
}
