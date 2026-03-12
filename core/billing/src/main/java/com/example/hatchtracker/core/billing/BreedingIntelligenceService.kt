package com.example.hatchtracker.core.billing

import com.example.hatchtracker.billing.Entitlements
import com.example.hatchtracker.model.*
import com.example.hatchtracker.domain.breeding.BreedingPredictionService
import com.example.hatchtracker.domain.breeding.SuggestedScenario
import com.example.hatchtracker.domain.breeding.StrategySearchEngine
import com.example.hatchtracker.domain.breeding.BreedingGoalTemplate
import com.example.hatchtracker.domain.breeding.SearchConfig
import com.example.hatchtracker.data.models.TraitTarget
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking

import com.example.hatchtracker.model.UiText
import com.example.hatchtracker.core.common.R as ComR

/**
 * Gated service for breeding intelligence features.
 * Enforces PRO-only access to strategic recommendations and advanced features.
 */
@Singleton
class BreedingIntelligenceService @Inject constructor(
    private val entitlements: Entitlements,
    private val breedingPredictionService: BreedingPredictionService,
    private val strategySearchEngine: StrategySearchEngine
) {
    /**
     * Get basic breeding recommendation for a pair.
     * Available to ALL users but with limited information.
     */
    fun getBasicPairRecommendation(
        male: Bird,
        female: Bird,
        incubations: List<Incubation>
    ): BasicRecommendation {
        var score = 50

        if (male.species != female.species) {
            return BasicRecommendation(
                male = male,
                female = female,
                score = 0,
                basicSummary = UiText.StringResource(
                    ComR.string.breeding_error_incompatible_species,
                    male.species.name,
                    female.species.name
                )
            )
        }

        if (male.motherId != null && male.motherId == female.motherId) {
            score -= 80
        }
        if (male.localId == female.fatherId || female.localId == male.fatherId) {
            score -= 100
        }

        val femaleIncubations = incubations.filter { it.birdId == female.localId }
        if (femaleIncubations.isNotEmpty()) {
            val totalEggs = femaleIncubations.sumOf { it.eggsCount }
            if (totalEggs > 0) {
                val rate = femaleIncubations.sumOf { it.hatchedCount }.toFloat() / totalEggs
                if (rate > 0.6f) score += 10
            }
        }

        val summary = when {
            score >= 70 -> UiText.StringResource(ComR.string.breeding_basic_match_good, male.species.name)
            score >= 50 -> UiText.StringResource(ComR.string.breeding_basic_match_acceptable, male.species.name)
            score >= 30 -> UiText.StringResource(ComR.string.breeding_basic_match_low)
            else -> UiText.StringResource(ComR.string.breeding_basic_match_not_recommended)
        }

        return BasicRecommendation(
            male = male,
            female = female,
            score = score.coerceIn(0, 100),
            basicSummary = summary
        )
    }

    /**
     * Get strategic breeding recommendation with detailed rationale.
     * PRO-ONLY feature.
     */
    fun getStrategicRecommendation(
        male: Bird,
        female: Bird,
        incubations: List<Incubation>,
        goals: List<BreedingGoal>
    ): StrategicRecommendation? {
        if (!entitlements.canAccessPROIntelligence()) {
            return null
        }

        val plan = runBlocking {
            strategySearchEngine.search(
                species = male.species,
                population = listOf(male, female),
                template = goals.toGoalTemplate(),
                config = SearchConfig(topKPlans = 1, maxGenerations = 1, beamWidth = 4)
            ).firstOrNull()
        } ?: return null

        val gen1 = plan.pathway.firstOrNull()
        
        return StrategicRecommendation(
            male = male,
            female = female,
            score = plan.overallScore.toInt(),
            strategicRationale = UiText.DynamicString(plan.summaryRationale), // Plan rationale is usually generated/complex
            goalMatches = goals.filter { _ ->
                 plan.overallScore > 30 
            }.map { it.type },
            traitAnalysis = UiText.DynamicString(gen1?.expectedTraitGains?.joinToString(", ") ?: "Standard Analysis"),
            riskFactors = if (plan.overallScore < 0) listOf("Risk Penalty Applied") else emptyList(),
            expectedOutcomes = gen1?.expectedTraitGains ?: emptyList()
        )
    }

    /**
     * Get top strategic recommendations from available birds.
     * PRO-ONLY feature.
     */
    fun getTopStrategicRecommendations(
        birds: List<Bird>,
        incubations: List<Incubation>,
        goals: List<BreedingGoal>,
        limit: Int = 10
    ): List<StrategicRecommendation> {
        if (!entitlements.canAccessPROIntelligence()) {
            return emptyList()
        }
        
        val activeBirds = birds.filter { it.status == "active" }
        val females = activeBirds.filter { it.sex == Sex.FEMALE } 
        val males = activeBirds.filter { it.sex == Sex.MALE }
        
        val recommendations = mutableListOf<StrategicRecommendation>()
        
        females.groupBy { it.species }.forEach { (species, speciesFemales) ->
             val speciesMales = males.filter { it.species == species }
             if (speciesMales.isEmpty()) return@forEach
             
             speciesFemales.forEach { female ->
                  speciesMales.forEach { male ->
                      val rec = getStrategicRecommendation(male, female, incubations, goals)
                      if (rec != null && rec.score > 30) {
                          recommendations.add(rec)
                      }
                  }
             }
        }
        
        return recommendations.sortedByDescending { it.score }.take(limit)
    }

    fun buildBreedingScenarios(
        birds: List<Bird>,
        incubations: List<Incubation>
    ): List<SuggestedScenario> {
        return emptyList() 
    }

    fun canAccessStrategicFeatures(): Boolean {
        return entitlements.canAccessPROIntelligence()
    }

    fun getMaxBreedsPerBatch(): Int {
        return entitlements.maxBreeds()
    }

    private fun List<BreedingGoal>.toGoalTemplate(): BreedingGoalTemplate {
        val required = mapNotNull { goal ->
            val value = goal.targetValue?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            TraitTarget(traitId = value, valueId = value)
        }

        return BreedingGoalTemplate(
            id = "billing_${System.currentTimeMillis()}",
            title = "Strategic Pair Analysis",
            description = "Derived from selected breeding goals",
            mustHave = required
        )
    }
}
