package com.example.hatchtracker.common.util

/**
 * Defines species-specific rules for raising chicks in the nursery.
 */
object NurseryConfig {

    data class NurseryRule(
        val initialTemp: Double, // Starting brooder temp (e.g. 35C)
        val tempReductionPerDay: Double, // How much to lower per day (e.g. 0.5C)
        val minSurvivalTemp: Double, // Do not go below this (ambient/comfort zone)
        val minAgeForFlock: Int // Days before allowed to move out (e.g. 42 = 6 weeks)
    )

    private val RULES = mapOf(
        "Chicken" to NurseryRule(35.0, 0.5, 20.0, 42),
        "Duck" to NurseryRule(32.0, 0.5, 18.0, 35),
        "Goose" to NurseryRule(32.0, 0.5, 15.0, 35),
        "Turkey" to NurseryRule(37.0, 0.4, 20.0, 56),
        "Peafowl" to NurseryRule(36.5, 0.3, 22.0, 84),
        "Quail" to NurseryRule(37.5, 0.5, 24.0, 21)
    )

    val DEFAULT_RULE = RULES["Chicken"]!!

    fun getRuleForSpecies(species: String): NurseryRule {
        return RULES.entries.find { species.contains(it.key, ignoreCase = true) }?.value ?: DEFAULT_RULE
    }
}
