package com.example.hatchtracker.core.common

import com.example.hatchtracker.core.common.R
import com.example.hatchtracker.model.UiText

/**
 * Centralized logic for Hatchy's guidance across the Breeding lifecycle.
 * Use this to retrieve persona-aligned advice for various modules.
 */
object HatchyGuidanceEngine {

    private const val UPGRADE_PROMPT = "That's gettin' into some deep water, friend. My 'Master Breeder' insights on genetics are for our PRO folks. You might want to think about upgradin' to unlock the full potential of your flock."

    /**
     * Phase 1: Scenario Creation Advice
     */
    fun getScenarioAdvice(isPro: Boolean, species: String, goals: List<String>): UiText {
        if (!isPro) return UiText.DynamicString(UPGRADE_PROMPT)
        return UiText.DynamicString("Now, startin' a new $species project is a big step. Focusin' on ${goals.joinToString()} is a fine idea, but remember\u2014nature's genetics have a way of throwin' curveballs. Don't rush into it.")
    }

    /**
     * Phase 2: Trait Feasibility Analysis
     */
    fun analyzeFeasibility(isPro: Boolean, traitName: String, confidence: Float): UiText {
        if (!isPro) return UiText.DynamicString(UPGRADE_PROMPT)
        return if (confidence < 0.4f) {
            UiText.DynamicString("Hmm, that $traitName trait is a bit of a mystery right now. My confidence is only ${(confidence * 100).toInt()}%. We'll likely need more data before we can place any real bets.")
        } else {
            UiText.DynamicString("I'm seein' some solid patterns for $traitName. Still, environment affects every feathers, so stay practical.")
        }
    }

    /**
     * Phase 3: Implementation Guidance
     */
    fun getImplementationTip(isPro: Boolean): UiText {
        if (!isPro) return UiText.DynamicString(UPGRADE_PROMPT)
        return UiText.DynamicString("Stable lines are built on patience. Don't go backcrossin' too heavy until you've seen the first few generations' vigor.")
    }

    /**
     * Phase 4: Incubation Recommendations
     * Available to ALL users.
     */
    fun getIncubationTip(day: Int): UiText {
        return when {
            day <= 7 -> UiText.StringResource(R.string.hatchy_advice_incubation_early, day)
            day >= 18 -> UiText.StringResource(R.string.hatchy_advice_incubation_late, day)
            else -> UiText.StringResource(R.string.hatchy_advice_incubation_mid, day)
        }
    }

    /**
     * Phase 5: Post-Hatch Evaluation Feedback
     * Available to ALL users (Basic success rates).
     */
    fun getEvaluationFeedback(successRate: Float): UiText {
        return if (successRate > 0.8f) {
            UiText.StringResource(R.string.hatchy_advice_evaluation_success, (successRate * 100).toInt())
        } else {
            UiText.StringResource(R.string.hatchy_advice_evaluation_failure, (successRate * 100).toInt())
        }
    }
    /**
     * Phase 6: Nursery/Brooder Management
     * Available to ALL users.
     */
    fun getNurseryAdvice(species: String, targetTemp: Double): UiText {
        return UiText.StringResource(R.string.hatchy_advice_nursery_temp, species, targetTemp.toInt())
    }

    fun getNurseryAdvice(daysOld: Int, currentTemp: Double, targetTemp: Double): UiText {
        return UiText.StringResource(R.string.hatchy_advice_nursery_status, daysOld, currentTemp.toInt(), targetTemp.toInt())
    }

    /**
     * Phase 7: General Flock Management
     * Available to ALL users.
     */
    fun getFlockAdvice(purpose: String, birdCount: Int): UiText {
        return when (purpose.lowercase()) {
            "eggs", "production" -> UiText.StringResource(R.string.hatchy_advice_flock_layers, birdCount)
            "breeding" -> UiText.StringResource(R.string.hatchy_advice_flock_breeding)
            "meat" -> UiText.StringResource(R.string.hatchy_advice_flock_meat)
            else -> UiText.StringResource(R.string.hatchy_advice_flock_mixed, birdCount)
        }
    }
}
