package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.BreedingScenario

data class ScenarioComparisonResult(
    val traitTradeOffs: Map<String, List<TraitComparison>>, // TraitId -> [Scenario1Value, Scenario2Value...]
    val riskSummary: List<RiskComparison>,
    val timelineOverlay: List<TimelineStep>,
    val recommendation: HatchyRecommendation
)

data class TraitComparison(
    val scenarioName: String,
    val score: Float, // 0.0 to 1.0 (how well it meets priorities)
    val predictedMagnitude: String // e.g. "High", "Deep Green"
)

data class RiskComparison(
    val scenarioName: String,
    val maxSeverity: String,
    val primaryRiskDesc: String
)

data class TimelineStep(
    val generationIndex: Int,
    val scenarioMilestones: Map<String, String> // ScenarioId -> Key Outcome at this step
)

data class HatchyRecommendation(
    val winnerScenarioId: String,
    val rationale: String,
    val winningFactor: String // e.g. "Lowest Risk", "Fastest Progress"
)

object ScenarioComparisonManager {
    
    fun compareScenarios(scenarios: List<BreedingScenario>): Result<ScenarioComparisonResult> {
        if (scenarios.size < 2 || scenarios.size > 4) {
            return Result.failure(Exception("Select 2 to 4 scenarios to compare."))
        }
        
        val species = scenarios.map { it.species }.distinct()
        if (species.size > 1) {
            return Result.failure(Exception("All scenarios must be of the same species (${species.joinToString()})."))
        }
        
        val depths = scenarios.map { it.generations.size }.distinct()
        if (depths.size > 1) {
            return Result.failure(Exception("All scenarios must have the same generation depth for comparison."))
        }
        
        // Logic for calculating trade-offs, risks, and recommendations would go here
        // For now returning a mock/template structure
        return Result.success(calculateComparison(scenarios))
    }
    
    private fun calculateComparison(scenarios: List<BreedingScenario>): ScenarioComparisonResult {
        val traits = scenarios.flatMap { it.traitPriorities.keys }.distinct()
        
        // 1. Calculate Trait Trade-offs
        val tradeOffs = traits.associateWith { traitId ->
            scenarios.map { scenario ->
                val priority = scenario.traitPriorities[traitId]?.priority ?: 0
                // Mocking score calculation based on priority and generation outcomes
                val score = (priority.toFloat() / 5f) * (0.8f + (0.2f * scenario.generations.size / 3f))
                TraitComparison(
                    scenarioName = scenario.name,
                    score = score.coerceIn(0f, 1f),
                    predictedMagnitude = if (score > 0.8f) "Very High" else if (score > 0.5f) "Moderate" else "Low"
                )
            }
        }

        // 2. Risk Summary
        val riskSummary = scenarios.map { scenario ->
            val highestRisk = scenario.riskWarnings.maxByOrNull { 
                when(it.severity) { "Lethal" -> 4; "High" -> 3; "Medium" -> 2; else -> 1 }
            }
            RiskComparison(
                scenarioName = scenario.name,
                maxSeverity = highestRisk?.severity ?: "None",
                primaryRiskDesc = highestRisk?.description ?: "Stable genetic path"
            )
        }

        // 3. Timeline Overlay
        val maxGen = scenarios.maxOf { it.generations.size }
        val timeline = (1..maxGen).map { genIndex ->
            TimelineStep(
                generationIndex = genIndex,
                scenarioMilestones = scenarios.associate { 
                    it.id to (it.generations.getOrNull(genIndex - 1)?.predictedOutcomes?.firstOrNull() ?: "Stabilizing")
                }
            )
        }

        // 4. Hatcher Recommendation
        // Score scenarios: avg(trait score) - risk penalty
        val scenarioScores = scenarios.map { scenario ->
            val avgTraitScore = traits.map { t -> 
                val p = scenario.traitPriorities[t]?.priority ?: 0
                p.toFloat() / 5f 
            }.average().toFloat()
            
            val riskPenalty = when (riskSummary.find { it.scenarioName == scenario.name }?.maxSeverity) {
                "Lethal" -> 1.0f
                "High" -> 0.4f
                "Medium" -> 0.1f
                else -> 0.0f
            }
            
            scenario.id to (avgTraitScore - riskPenalty)
        }
        
        val winner = scenarioScores.maxByOrNull { it.second } ?: (scenarios.first().id to 0f)
        val winningScenario = scenarios.find { it.id == winner.first }!!
        
        return ScenarioComparisonResult(
            traitTradeOffs = tradeOffs,
            riskSummary = riskSummary,
            timelineOverlay = timeline,
            recommendation = HatchyRecommendation(
                winnerScenarioId = winningScenario.id,
                winningFactor = if (riskSummary.find { it.scenarioName == winningScenario.name }?.maxSeverity == "None") "Lowest Risk" else "Balanced Performance",
                rationale = "Hatchy recommends ${winningScenario.name} because it achieves your ${winningScenario.traitPriorities.keys.firstOrNull() ?: "goals"} with the least amount of genetic drag."
            )
        )
    }
}
