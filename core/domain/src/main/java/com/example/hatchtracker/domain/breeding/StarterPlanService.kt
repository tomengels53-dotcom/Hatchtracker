package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.BreedingTarget
import com.example.hatchtracker.domain.breeding.plan.BreedingPlanDraft
import com.example.hatchtracker.domain.breeding.plan.PlanConstraints
import com.example.hatchtracker.domain.breeding.plan.ProgramMode
import com.example.hatchtracker.domain.breeding.starter.ReferenceTemplateCatalog
import com.example.hatchtracker.domain.breeding.starter.VirtualBirdFactory
import com.example.hatchtracker.model.Sex
import com.example.hatchtracker.model.Species
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StarterPlanService @Inject constructor(
    private val searchEngine: StrategySearchEngine
) {

    suspend fun generateStarterPlan(
        species: Species,
        goalTemplate: BreedingGoalTemplate,
        constraints: PlanConstraints = PlanConstraints()
    ): BreedingPlanDraft? {
        val templates = ReferenceTemplateCatalog.getTemplatesForSpecies(species)
        if (templates.isEmpty()) return null

        val foundationStock = templates.flatMap { template ->
            listOf(
                VirtualBirdFactory.createFromTemplate(template, Sex.MALE),
                VirtualBirdFactory.createFromTemplate(template, Sex.FEMALE)
            )
        }

        val searchConfig = SearchConfig(
            maxGenerations = constraints.maxGenerations,
            beamWidth = 30
        )

        val bestPlan = searchEngine.search(
            species = species,
            population = foundationStock,
            template = goalTemplate,
            config = searchConfig,
            constraints = constraints
        ).maxByOrNull { it.overallScore } ?: return null

        val steps = BreedingProgramConverter.convert(bestPlan).steps

        return BreedingPlanDraft(
            id = java.util.UUID.randomUUID().toString(),
            planType = ProgramMode.STARTER_GOAL_ONLY,
            species = species,
            goal = BreedingTarget(
                requiredTraits = goalTemplate.mustHave,
                preferredTraits = goalTemplate.niceToHave,
                excludedTraits = goalTemplate.avoid
            ),
            summaryRationale = bestPlan.summaryRationale,
            steps = steps
        )
    }
}

