package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.TraitTarget
import com.example.hatchtracker.data.models.TargetSex
import com.example.hatchtracker.domain.genetics.GeneticLocusCatalog
import com.example.hatchtracker.model.Species
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalTemplateCatalog @Inject constructor() {

    fun getTemplatesForSpecies(species: Species): List<BreedingGoalTemplate> {
        val availableLoci = GeneticLocusCatalog.lociForSpecies(species).map { it.locusId }
        val prefix = GeneticLocusCatalog.speciesPrefix(species)

        val templates = mutableListOf<BreedingGoalTemplate>()

        // 1. Universal Templates (Always Included)
        templates.add(
            BreedingGoalTemplate(
                id = "maximize_diversity",
                title = "Maximize Diversity",
                description = "Low-priority on specific traits; high focus on maintaining genetic variance.",
                diversityWeight = 1.0
            )
        )
        templates.add(
            BreedingGoalTemplate(
                id = "reduce_inbreeding",
                title = "Reduce Inbreeding Risk",
                description = "Specifically prioritizes unrelated pairings to lower co-ancestry coefficients.",
                diversityWeight = 0.8
            )
        )
        templates.add(
            BreedingGoalTemplate(
                id = "foundation_planning",
                title = "Foundation Planning",
                description = "Generic plan for establishing a new line with external stock.",
                diversityWeight = 0.4
            )
        )

        // 2. Species-Specific Templates
        // Colors (Chicken Only for now)
        if (species == Species.CHICKEN) {
             templates.add(
                BreedingGoalTemplate(
                    id = "black_plumage",
                    title = "Pure Black Plumage",
                    description = "Focus on Extended Black (E) and minimizing leakage.",
                    mustHave = listOf(TraitTarget("E_Locus", "E"))
                )
            )
        }

        // Egg Colors
        val esLocus = if (species == Species.CHICKEN) "O_Locus" else "${prefix}ES_Locus"
        if (esLocus in availableLoci) {
             val targetValue = if (species == Species.CHICKEN) "O" else "ES"
             templates.add(
                BreedingGoalTemplate(
                    id = "colored_eggs",
                    title = "Colored Egg Line",
                    description = "Focus on blue/green egg shell structural colors.",
                    mustHave = listOf(TraitTarget(esLocus, targetValue))
                )
            )
        }

        return templates
    }
}

data class BreedingGoalTemplate(
    val id: String,
    val title: String,
    val description: String,
    val mustHave: List<TraitTarget> = emptyList(),
    val niceToHave: List<TraitTarget> = emptyList(),
    val avoid: List<TraitTarget> = emptyList(),
    val diversityWeight: Double = 0.0
)

