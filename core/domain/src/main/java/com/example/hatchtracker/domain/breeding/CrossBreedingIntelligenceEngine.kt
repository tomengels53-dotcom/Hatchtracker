package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.BreedingGoal
import com.example.hatchtracker.model.Bird
import com.example.hatchtracker.model.Incubation
import com.example.hatchtracker.model.StrategicRecommendation
import com.example.hatchtracker.model.UiText
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backward-compatible shim for legacy DI/classpath references.
 * New strategic logic lives in StrategySearchEngine/BreedingIntelligenceService.
 */
@Singleton
class CrossBreedingIntelligenceEngine @Inject constructor() {

    fun getStrategicPairRecommendation(
        male: Bird,
        female: Bird,
        incubations: List<Incubation>,
        goals: List<BreedingGoal>
    ): StrategicRecommendation {
        val score = if (male.species == female.species) 50 else -9999
        val risks = if (male.species == female.species) emptyList() else listOf("Cross-species breeding detected")
        return StrategicRecommendation(
            male = male,
            female = female,
            score = score,
            strategicRationale = UiText.DynamicString("Legacy engine compatibility mode"),
            goalMatches = goals.map { it.type }.distinct(),
            traitAnalysis = UiText.DynamicString("Use BreedingIntelligenceService for full analysis"),
            riskFactors = risks,
            expectedOutcomes = emptyList()
        )
    }

    fun getTopRecommendations(
        birds: List<Bird>,
        incubations: List<Incubation>,
        goals: List<BreedingGoal>,
        limit: Int = 10
    ): List<StrategicRecommendation> {
        val males = birds.filter { it.sex.name == "MALE" }
        val females = birds.filter { it.sex.name == "FEMALE" }
        return males.flatMap { male ->
            females.map { female ->
                getStrategicPairRecommendation(male, female, incubations, goals)
            }
        }.sortedByDescending { it.score }.take(limit)
    }

    fun buildBreedingScenarios(
        birds: List<Bird>,
        incubations: List<Incubation>
    ): List<SuggestedScenario> = emptyList()
}

