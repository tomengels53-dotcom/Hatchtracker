package com.example.hatchtracker.domain.breeding

/**
 * Defines static trait covariances (correlations) used to penalize
 * conflicting quantitative breeding goals deterministically.
 */
object TraitCovarianceCatalog {
    /**
     * Map of trait pairs to their correlation coefficient (-1.0 to 1.0).
     * Order of the pair does not matter conceptually, though exact pair match is assumed.
     */
    val correlations: Map<Pair<String, String>, Double> = mapOf(
        ("egg_size" to "egg_count") to -0.4,
        ("growth_rate" to "feed_efficiency") to 0.3
    )

    /**
     * Helper to get correlation regardless of pair order.
     */
    fun getCorrelation(traitA: String, traitB: String): Double {
        return correlations[Pair(traitA, traitB)] ?: correlations[Pair(traitB, traitA)] ?: 0.0
    }
}
