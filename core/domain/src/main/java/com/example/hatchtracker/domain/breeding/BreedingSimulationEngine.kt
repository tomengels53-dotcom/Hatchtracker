package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.model.Bird
import com.example.hatchtracker.model.Species
import com.example.hatchtracker.domain.breeding.BreedingPredictionResult
import com.example.hatchtracker.model.genetics.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class BreedingSimulationEngine @Inject constructor() {

    // Simple bounded LRU cache (Max 500 entries)
    private val MAX_CACHE_ENTRIES = 500
    private val simulationCache = object : LinkedHashMap<String, BreedingPredictionResult>(MAX_CACHE_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, BreedingPredictionResult>?): Boolean {
            return size > MAX_CACHE_ENTRIES
        }
    }
    
    /**
     * Simulations are now deterministic based on a canonical hash of the request.
     */
    fun simulate(
        species: Species,
        sire: Bird,
        dam: Bird,
        scenario: BreedingScenarioProfile?,
        reportVersion: Int = 1,
        catalogVersion: Int = 1
    ): BreedingPredictionResult {
        // 1. Generate Canonical Cache Key (Request Normalization)
        val cacheKey = generateCacheKey(sire, dam, scenario, reportVersion, catalogVersion)
        
        // 2. Cache Lookup (LRU)
        synchronized(simulationCache) {
            simulationCache[cacheKey]?.let { return it }
        }

        // 3. Deterministic Seeding (Normalized Hash basis)
        val seed = cacheKey.hashCode().toLong()
        val random = Random(seed)

        // 4. Run Representative Simulation
        val result = runSimulationCore(species, sire, dam, random)
        
        // 5. Cache (LRU) and Return
        synchronized(simulationCache) {
            simulationCache[cacheKey] = result
        }
        return result
    }

    /**
     * Formal Cache Key Contract:
     * - Accounts for sire/dam identity & genetic composition.
     * - Accounts for scenario goals and versioning.
     * - Ensures stable seeds across sessions for identical queries.
     */
    private fun generateCacheKey(
        sire: Bird, 
        dam: Bird, 
        scenario: BreedingScenarioProfile?, 
        reportVersion: Int,
        catalogVersion: Int
    ): String {
        // Canonicalize breed compositions
        val sComp = sire.breedComposition.sortedBy { it.breedId }.joinToString { "${it.breedId}:${it.percentage}" }
        val dComp = dam.breedComposition.sortedBy { it.breedId }.joinToString { "${it.breedId}:${it.percentage}" }
        
        // Scenario hashing
        val scenarioKey = scenario?.let { "${it.goalType}:${it.breedingMode}:${it.variabilityTolerance}" } ?: "NONE"
        
        return "v${reportVersion}:c${catalogVersion}|S:${sire.syncId}[$sComp]|D:${dam.syncId}[$dComp]|$scenarioKey"
    }

    private fun runSimulationCore(
        species: Species,
        sire: Bird,
        dam: Bird,
        random: Random
    ): BreedingPredictionResult {
        // Phase 2 Representative Logic: Phenotype recombination placeholder
        return BreedingPredictionResult(
            phenotypeResult = PhenotypeResult(emptyList()),
            lineStability = null
        )
    }

    fun clearCache() {
        synchronized(simulationCache) {
            simulationCache.clear()
        }
    }
}
