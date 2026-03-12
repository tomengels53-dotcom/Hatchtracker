package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.domain.breeding.quant.QuantitativeTraitId
import com.example.hatchtracker.model.Species
import com.example.hatchtracker.domain.genetics.GeneticLocusCatalog
import javax.inject.Inject
import javax.inject.Singleton

data class BreedingGoalOption(
    val id: String,
    val label: String,
    val type: GoalOptionType,
    val category: String,
    val possibleValues: List<String> = emptyList() // For discrete traits
)

enum class GoalOptionType {
    MENDELIAN_DISCRETE,
    QUANTITATIVE_RANGE
}

@Singleton
class BreedingGoalTraitCatalog @Inject constructor() {

    fun getAvailableGoals(species: Species): List<BreedingGoalOption> {
        val goals = mutableListOf<BreedingGoalOption>()

        // 1. Add Quantitative Traits
        goals.add(
            BreedingGoalOption(
                id = QuantitativeTraitId.TEMPERAMENT.name,
                label = "Temperament",
                type = GoalOptionType.QUANTITATIVE_RANGE,
                category = "Behavior"
            )
        )
         goals.add(
            BreedingGoalOption(
                id = QuantitativeTraitId.SIZE.name,
                label = "Size",
                type = GoalOptionType.QUANTITATIVE_RANGE,
                category = "Conformation"
            )
        )
         goals.add(
            BreedingGoalOption(
                id = QuantitativeTraitId.EGG_PRODUCTION.name,
                label = "Egg Production",
                type = GoalOptionType.QUANTITATIVE_RANGE,
                category = "Production"
            )
        )

        // 2. Add Mendelian Traits from Catalog
        val loci = GeneticLocusCatalog.lociForSpecies(species)
        loci.forEach { locus ->
            // Filter out internal-only loci if needed
            if (!locus.locusId.startsWith("INTERNAL")) {
                goals.add(
                    BreedingGoalOption(
                        id = locus.locusId,
                        label = locus.displayName,
                        type = GoalOptionType.MENDELIAN_DISCRETE,
                        category = "Appearance",
                        possibleValues = locus.alleles.toList()
                    )
                )
            }
        }

        return goals
    }
}

