package com.example.hatchtracker.model


/**
 * Common interface for anything that can be used as a genetic parent in breeding scenarios.
 */
interface GeneticSource {
    val geneticSourceId: String
    val displayName: String
    val species: Species
    val sex: Sex?
    val geneticProfile: GeneticProfile
}
