package com.example.hatchtracker.data.models

import com.example.hatchtracker.model.Species

/**
 * Categorizes the genetic complexity of a trait to drive generation estimation.
 */
enum class GeneticComplexity {
    MONOGENIC,   // Single locus (e.g., Pea Comb, Blue Egg)
    OLIGOGENIC,  // Few loci (e.g., Some plumage patterns)
    POLYGENIC    // Many loci, continuous variation (e.g., Egg Production, Size)
}

/**
 * Defines the known genetic and phenotypic tendencies for a specific breed.
 */
data class BreedTraitProfile(
    val breedName: String,
    val species: Species,
    val traits: Map<String, TraitTendency>,
    val confidence: EstimateConfidence = EstimateConfidence.MED
)

/**
 * Represents a specific trait's behavior within a breed profile.
 */
data class TraitTendency(
    val traitId: String,
    val dominantValue: String?,
    val complexity: GeneticComplexity,
    val reliability: Double = 0.8 // 0.0 to 1.0 likelihood of passing according to standard
)

/**
 * Represents a breed source outside the user's current flocks.
 */
data class ExternalBreedSource(
    val breedName: String,
    val species: Species,
    val availableTraits: List<String>
)

/**
 * In-memory catalog of breed genetic knowledge.
 */
@Deprecated("Use BreedStandardRepository instead. This catalog contains hardcoded mock data.")
object BreedKnowledgeCatalog {
    private val profiles = mutableMapOf<String, BreedTraitProfile>()

    init {
        // Sample bootstrap data - logic only, will be expanded in domain service
        registerProfile(
            BreedTraitProfile(
                breedName = "Ameraucana",
                species = Species.CHICKEN,
                traits = mapOf(
                    "O_Locus" to TraitTendency("O_Locus", "BLUE_EGG", GeneticComplexity.MONOGENIC),
                    "PEA_COMB" to TraitTendency("PEA_COMB", "PEA", GeneticComplexity.MONOGENIC),
                    "MUFFS_BEARDS" to TraitTendency("MUFFS_BEARDS", "PRESENT", GeneticComplexity.OLIGOGENIC)
                )
            )
        )
        registerProfile(
            BreedTraitProfile(
                breedName = "Leghorn",
                species = Species.CHICKEN,
                traits = mapOf(
                    "EGG_PRODUCTION" to TraitTendency("EGG_PRODUCTION", "HIGH", GeneticComplexity.POLYGENIC),
                    "WHITE_EGG" to TraitTendency("WHITE_EGG", "WHITE", GeneticComplexity.MONOGENIC)
                )
            )
        )
    }

    private fun registerProfile(profile: BreedTraitProfile) {
        profiles[profile.breedName] = profile
    }

    fun getProfile(breed: String): BreedTraitProfile? = profiles[breed]
    
    fun findDonorsForTrait(species: Species, traitId: String): List<BreedTraitProfile> {
        return profiles.values.filter { it.species == species && it.traits.containsKey(traitId) }
    }
}

