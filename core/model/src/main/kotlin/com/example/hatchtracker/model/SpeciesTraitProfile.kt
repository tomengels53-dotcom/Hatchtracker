package com.example.hatchtracker.model

import com.example.hatchtracker.model.KnowledgeTopic

/**
 * Defines which trait topics are supported for a given species.
 * Replaces TraitFamily with KnowledgeTopic for tighter routing integration.
 */
data class SpeciesTraitProfile(
    val species: Species,
    val supportedTopics: Set<KnowledgeTopic>
)

/**
 * Central registry mapping species to their applicable knowledge topics.
 * Used by Hatchy resolvers and intent classifiers to validate query applicability.
 */
object TraitApplicabilityMatrix {
    private val matrix: Map<Species, Set<KnowledgeTopic>> = mapOf(
        Species.CHICKEN to setOf(
            KnowledgeTopic.EGG_TRAITS,
            KnowledgeTopic.TEMPERAMENT,
            KnowledgeTopic.HARDINESS,
            KnowledgeTopic.UTILITY_PURPOSE,
            KnowledgeTopic.PHYSICAL_TRAITS,
            KnowledgeTopic.HEALTH_ROBUSTNESS
        ),
        Species.DUCK to setOf(
            KnowledgeTopic.EGG_TRAITS,
            KnowledgeTopic.TEMPERAMENT,
            KnowledgeTopic.HARDINESS,
            KnowledgeTopic.UTILITY_PURPOSE,
            KnowledgeTopic.PHYSICAL_TRAITS // Includes scale, flight, water etc. later
        ),
        Species.GOOSE to setOf(
            KnowledgeTopic.TEMPERAMENT,
            KnowledgeTopic.HARDINESS,
            KnowledgeTopic.UTILITY_PURPOSE,
            KnowledgeTopic.PHYSICAL_TRAITS
        ),
        Species.TURKEY to setOf(
            KnowledgeTopic.UTILITY_PURPOSE,
            KnowledgeTopic.HEALTH_ROBUSTNESS,
            KnowledgeTopic.PHYSICAL_TRAITS
        ),
        Species.QUAIL to setOf(
            KnowledgeTopic.EGG_TRAITS,
            KnowledgeTopic.UTILITY_PURPOSE,
            KnowledgeTopic.HEALTH_ROBUSTNESS
        )
    )

    /**
     * Returns the set of topics applicable to the given species.
     */
    fun getSupportedTopics(species: Species): Set<KnowledgeTopic> {
        return matrix[species] ?: emptySet()
    }

    /**
     * Checks if a specific topic is biologically applicable to the species.
     */
    fun isTopicApplicable(species: Species, topic: KnowledgeTopic): Boolean {
        return matrix[species]?.contains(topic) ?: false
    }

    /**
     * Lists all species that support a specific trait topic.
     */
    fun getSpeciesSupportingTopic(topic: KnowledgeTopic): List<Species> {
        return matrix.filter { it.value.contains(topic) }.keys.toList()
    }
}
