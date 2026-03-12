package com.example.hatchtracker.domain.genetics

import com.example.hatchtracker.model.BreedStandard
import com.example.hatchtracker.model.TraitLevel
import com.example.hatchtracker.model.genetics.GenotypeCall
import com.example.hatchtracker.model.genetics.Certainty
import com.example.hatchtracker.model.genetics.QuantitativeTraitValue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps BreedStandard repository data to genetics-engine structures.
 * This is the TRANSLATION LAYER between descriptive breed catalogs 
 * and deterministic/probabilistic genetic logic.
 */
@Singleton
class BreedTraitGeneticsMapper @Inject constructor() {

    /**
     * Projects a BreedStandard into explicit GenotypeCalls where trait matches are definitive.
     */
    fun mapToGenotypeCalls(standard: BreedStandard): Map<String, GenotypeCall> {
        val calls = mutableMapOf<String, GenotypeCall>()

        // 1. Egg Color (O Locus, BR Locus)
        standard.eggColor.lowercase().let { color ->
            when {
                color.contains("blue") || color.contains("green") || color.contains("olive") -> {
                    calls[GeneticLocusCatalog.LOCUS_O] = GenotypeCall(
                        GeneticLocusCatalog.LOCUS_O, listOf("O", "o"), Certainty.ASSUMED
                    )
                }
                color.contains("dark brown") -> {
                    calls[GeneticLocusCatalog.LOCUS_BR] = GenotypeCall(
                        GeneticLocusCatalog.LOCUS_BR, listOf("BR", "BR"), Certainty.ASSUMED
                    )
                }
            }
        }

        // 2. Comb Type (P Locus, R Locus)
        standard.combType.lowercase().let { comb ->
            when (comb) {
                "pea" -> {
                    calls["P_Locus"] = GenotypeCall("P_Locus", listOf("P", "p+"), Certainty.ASSUMED)
                }
                "rose" -> {
                    calls["R_Locus"] = GenotypeCall("R_Locus", listOf("R", "r+"), Certainty.ASSUMED)
                }
                "walnut" -> {
                    calls["P_Locus"] = GenotypeCall("P_Locus", listOf("P", "p+"), Certainty.ASSUMED)
                    calls["R_Locus"] = GenotypeCall("R_Locus", listOf("R", "r+"), Certainty.ASSUMED)
                }
                "single" -> {
                    calls["P_Locus"] = GenotypeCall("P_Locus", listOf("p+", "p+"), Certainty.DEFINITE)
                    calls["R_Locus"] = GenotypeCall("R_Locus", listOf("r+", "r+"), Certainty.DEFINITE)
                }
            }
        }

        // 3. Physical markers
        if (standard.shankFeathering == true) {
            calls["Pti_Locus"] = GenotypeCall("Pti_Locus", listOf("Pti-1", "pti+"), Certainty.ASSUMED)
        }
        
        if (standard.muffBeard == true) {
            calls["Mb_Locus"] = GenotypeCall("Mb_Locus", listOf("Mb", "mb+"), Certainty.ASSUMED)
        }

        return calls
    }

    /**
     * Maps polygenic breed predispositions into quantitative means/variances.
     */
    fun mapToQuantitativeTraits(standard: BreedStandard): Map<String, QuantitativeTraitValue> {
        val quant = mutableMapOf<String, QuantitativeTraitValue>()

        // Egg Production (Normalizing 0-365 scale to 0.0-1.0)
        standard.eggProductionPerYear?.let { eggs ->
            quant["egg_production"] = QuantitativeTraitValue(
                mean = (eggs.toDouble() / 365.0).coerceIn(0.0, 1.0),
                variance = 0.05 // Standard breed uniformity
            )
        }

        // Broodiness (TraitLevel Mapping)
        standard.broodinessLevel?.let { level ->
            quant["broodiness"] = QuantitativeTraitValue(
                mean = mapLevelToValue(level),
                variance = 0.1
            )
        }

        // Body Size (Weight Mapping if available)
        if (standard.weightHenKg > 0) {
            quant["body_size"] = QuantitativeTraitValue(
                mean = (standard.weightHenKg / 7.0).coerceIn(0.0, 1.0), // Normalizing relative to 7kg max
                variance = 0.05
            )
        }
        
        // Enriched Polygenic Traits
        standard.foragingAbility?.let { quant["foraging_ability"] = QuantitativeTraitValue(mapLevelToValue(it), 0.1) }
        standard.temperament?.let { quant["temperament"] = QuantitativeTraitValue(mapLevelToValue(it), 0.1) }
        standard.coldHardiness?.let { quant["cold_hardiness"] = QuantitativeTraitValue(mapLevelToValue(it), 0.1) }
        standard.heatTolerance?.let { quant["heat_tolerance"] = QuantitativeTraitValue(mapLevelToValue(it), 0.1) }
        standard.confinementTolerance?.let { quant["confinement_tolerance"] = QuantitativeTraitValue(mapLevelToValue(it), 0.1) }

        return quant
    }

    private fun mapLevelToValue(level: TraitLevel): Double = when (level) {
        TraitLevel.LOW -> 0.2
        TraitLevel.MEDIUM -> 0.5
        TraitLevel.HIGH -> 0.8
    }
}
