package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.BreedingTarget
import com.example.hatchtracker.data.models.TraitTarget
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalEvaluationEngine @Inject constructor(
    private val breedingGoalEvaluator: BreedingGoalEvaluator
) {

    /**
     * Evaluates a list of targets against a prediction result.
     * Returns a weighted match percentage (0.0 - 100.0).
     */
    fun evaluate(
        prediction: BreedingPredictionResult,
        mustHave: List<TraitTarget>,
        niceToHave: List<TraitTarget>,
        avoid: List<TraitTarget>
    ): GoalScore {
        val target = BreedingTarget(
            requiredTraits = mustHave,
            preferredTraits = niceToHave,
            excludedTraits = avoid
        )
        return breedingGoalEvaluator.evaluate(
            outcomes = prediction.phenotypeOutcomes,
            quantitativePredictions = prediction.quantitativePredictions,
            offspringDistributions = prediction.offspringDistributions,
            target = target
        )
    }
}
