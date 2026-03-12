package com.example.hatchtracker.model

import androidx.room.ColumnInfo
import com.example.hatchtracker.model.genetics.GenotypeCall

data class GeneticProfile(
    val knownGenes: List<String> = emptyList(),
    val fixedTraits: List<String> = emptyList(),
    val inferredTraits: List<String> = emptyList(),
    val unknownTraits: List<String> = emptyList(),
    val confidenceLevel: String = "LOW",
    // Mixed/Other Trait Support
    @ColumnInfo(defaultValue = "'{}'")
    val traitValues: Map<String, String> = emptyMap(),
    @ColumnInfo(defaultValue = "'{}'")
    val traitWeights: Map<String, Float> = emptyMap(),
    @ColumnInfo(defaultValue = "'[]'")
    val traitOverrides: List<BirdTraitOverride> = emptyList(),
    val notes: String? = null,
    // Core Genetics Engine (Phase 2)
    val genotypeCalls: Map<String, GenotypeCall>? = null,
    @ColumnInfo(defaultValue = "1")
    val genotypeVersion: Int = 1,
    @ColumnInfo(defaultValue = "'{}'")
    val quantitativeTraits: Map<String, com.example.hatchtracker.model.genetics.QuantitativeTraitValue> = emptyMap(),
    
    // Structured Traits (Phase E/Master Pass)
    val eggColor: EggColor? = null,
    val temperament: TemperamentLevel? = null,
    val broodiness: BroodinessLevel? = null,
    val bodySize: BodySizeClass? = null,
    val primaryUsage: PrimaryUsage? = null,
    val combType: CombType? = null
) {
    val confidenceLevelEnum: ConfidenceLevel
        get() = try {
            ConfidenceLevel.valueOf(confidenceLevel.uppercase())
        } catch (e: Exception) {
            ConfidenceLevel.LOW
        }
}

