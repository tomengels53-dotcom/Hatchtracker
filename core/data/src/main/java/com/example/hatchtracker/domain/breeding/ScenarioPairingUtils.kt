package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.ScenarioPairing

/**
 * Helper object to standardize ScenarioPairing source strings.
 * Prevents format drift and ensures consistent parsing.
 */
object ScenarioPairingSourceFormatter {

    private const val PREFIX_BIRD = "BIRD:"
    private const val PREFIX_BREED = "BREED:"
    private const val PREFIX_SCENARIO_GEN = "SCENARIO:"
    private const val PREFIX_GEN_VIRTUAL = "GEN:"
    
    // Explicit format markers
    private const val MARKER_GEN = ":GEN:"
    private const val MARKER_VIRTUAL_PAIR = ":VIRTUAL_PAIR"

    fun formatRealBird(birdId: Long): String {
        return "$PREFIX_BIRD$birdId"
    }

    fun formatBreedVirtual(breedId: String): String {
        return "$PREFIX_BREED$breedId"
    }

    fun formatScenarioFork(scenarioId: String, genIndex: Int): String {
        return "$PREFIX_SCENARIO_GEN$scenarioId$MARKER_GEN$genIndex"
    }

    fun formatGenVirtualPair(genIndex: Int): String {
        return "$PREFIX_GEN_VIRTUAL$genIndex$MARKER_VIRTUAL_PAIR"
    }

    fun parseType(source: String): SourceType {
        return when {
            source.startsWith(PREFIX_BIRD) -> SourceType.REAL_BIRD
            source.startsWith(PREFIX_BREED) -> SourceType.BREED_VIRTUAL
            source.startsWith(PREFIX_SCENARIO_GEN) -> SourceType.SCENARIO_FORK
            source.startsWith(PREFIX_GEN_VIRTUAL) -> SourceType.GEN_VIRTUAL
            else -> SourceType.UNKNOWN
        }
    }

    fun extractId(source: String): String {
        return when (parseType(source)) {
            SourceType.REAL_BIRD -> source.removePrefix(PREFIX_BIRD)
            SourceType.BREED_VIRTUAL -> source.removePrefix(PREFIX_BREED)
            else -> source // Complex formats handled specifically if needed
        }
    }
    
    fun isTraitFirst(maleSource: String, femaleSource: String): Boolean {
         val maleType = parseType(maleSource)
         val femaleType = parseType(femaleSource)
         // If either is a BREED virtual source, it implies trait-first selection from DB
         return maleType == SourceType.BREED_VIRTUAL || femaleType == SourceType.BREED_VIRTUAL
    }

    enum class SourceType {
        REAL_BIRD,
        BREED_VIRTUAL,
        SCENARIO_FORK,
        GEN_VIRTUAL,
        UNKNOWN
    }
}
