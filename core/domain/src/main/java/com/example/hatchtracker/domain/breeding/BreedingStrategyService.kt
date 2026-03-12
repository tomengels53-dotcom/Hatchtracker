package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.StartingSituation
import com.example.hatchtracker.data.models.StrategyConfig
import com.example.hatchtracker.data.models.StrategyMode
import com.example.hatchtracker.data.models.TraitDomain
import com.example.hatchtracker.data.models.GoalSpec
import com.example.hatchtracker.data.models.BaseLineDefinition
import com.example.hatchtracker.data.models.MultiBreedRoadmap

import com.example.hatchtracker.data.models.ScenarioEntryMode
import com.example.hatchtracker.domain.breeding.plan.PlanConstraints
import com.example.hatchtracker.model.Bird
import com.example.hatchtracker.model.Species
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

@Singleton
class BreedingStrategyService @Inject constructor(
    private val populationProvider: PopulationProvider,
    private val searchEngine: StrategySearchEngine,
    private val roadmapArchitect: StagedCrossArchitect
) {

    fun buildStrategy(request: StrategyRequest): Flow<List<BreedingProgram>> = flow {
        val baseSources = when (request.mode) {
            ScenarioEntryMode.FORWARD -> {
                populationProvider.getEligibleSources(request.species, request.selectedFlockIds).first()
            }
            ScenarioEntryMode.ASSISTED -> {
                val owned = populationProvider.getOwnedSources(request.species, request.selectedFlockIds).first()
                val global = populationProvider.getGlobalBreedSources(request.species).first()
                owned + global
            }
            ScenarioEntryMode.SCRATCH -> {
                populationProvider.getGlobalBreedSources(request.species).first()
            }
        }

        val population = baseSources.filterIsInstance<Bird>().toMutableList()

        request.forcedSire?.let { sire ->
            if (population.none { it.syncId == sire.syncId }) population.add(sire)
        }
        request.forcedDam?.let { dam ->
            if (population.none { it.syncId == dam.syncId }) population.add(dam)
        }

        if (population.isEmpty()) {
            emit(emptyList())
            return@flow
        }

        // Multi-Breed Roadmap Generation (PHASE 4)
        val roadmap = if (request.configuration.startingSituation == StartingSituation.IMPROVE_FLOCK) {
            val baseLine = BaseLineDefinition(
                baseBreed = population.firstOrNull()?.breed, // Simplified base detection
                baseFlockId = request.selectedFlockIds.firstOrNull(),
                preserveIdentity = request.configuration.strategyMode == StrategyMode.STRICT_LINE_BREEDING
            )
            roadmapArchitect.createRoadmap(
                species = request.species,
                baseLine = baseLine,
                goals = request.configuration.goalSpecs,
                mode = request.configuration.strategyMode
            )
        } else null

        val plans = searchEngine.search(
            species = request.species,
            population = population,
            template = request.template,
            config = request.config,
            constraints = request.constraints,
            roadmapContext = roadmap?.let { RoadmapConstraintContext(it.baseLine, it.stages) }
        )

        emit(plans)
    }
}

data class StrategyRequest(
    val species: Species,
    val mode: ScenarioEntryMode,
    val selectedFlockIds: List<String> = emptyList(),
    val template: BreedingGoalTemplate,
    val configuration: StrategyConfig = StrategyConfig(),
    val forcedSire: Bird? = null,
    val forcedDam: Bird? = null,
    val config: SearchConfig = SearchConfig(),
    val constraints: PlanConstraints = PlanConstraints()
)

