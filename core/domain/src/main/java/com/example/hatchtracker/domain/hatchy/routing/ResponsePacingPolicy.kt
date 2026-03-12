package com.example.hatchtracker.domain.hatchy.routing

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResponsePacingPolicy @Inject constructor() {

    /**
     * Determines an immediate thinking label based on initial interpretation.
     */
    fun determineThinkingLabel(interpretation: QueryInterpretation): String {
        val hasUserData = interpretation.questionMode.userDataAnchorScore > 0.5 || 
                         interpretation.entities.any { it.type == EntityType.USER_DATA_REF }

        return when (interpretation.intent) {
            HatchyIntent.BREEDING_GUIDANCE, HatchyIntent.BREED_INFO, HatchyIntent.BREED_COMPARISON, HatchyIntent.CROSSBREED_OUTCOME -> {
                if (hasUserData) "Checking your flock..." else "Preparing breeding guidance..."
            }
            HatchyIntent.INCUBATION_STATUS, HatchyIntent.INCUBATION_GUIDANCE -> {
                if (hasUserData) "Reviewing incubation status..." else "Looking up incubation guidance..."
            }
            HatchyIntent.NURSERY_STATUS, HatchyIntent.NURSERY_GUIDANCE -> {
                if (hasUserData) "Checking the nursery..." else "Preparing nursery guidance..."
            }
            HatchyIntent.FINANCE_SUMMARY, HatchyIntent.FINANCE_HELP -> {
                if (hasUserData) "Calculating your summary..." else "Looking up finance help..."
            }
            HatchyIntent.EQUIPMENT_STATUS, HatchyIntent.EQUIPMENT_HELP -> {
                if (hasUserData) "Checking your equipment..." else "Finding equipment help..."
            }
            HatchyIntent.APP_NAVIGATION -> {
                "Finding the right screen..."
            }
            HatchyIntent.POULTRY_HEALTH, HatchyIntent.GENERAL_POULTRY -> {
                "Reviewing poultry knowledge..."
            }
            else -> "Hatchy is thinking..."
        }
    }

    /**
     * Determines the minimum delay for a response based on its complexity and source.
     */
    fun calculateMinimumDelay(answer: HatchyAnswer): Long {
        // Navigation/Workflow - fastest
        if (answer.type == AnswerType.NAVIGATION) return 300L

        // Simulation or Recommendation - complex
        if (answer.type == AnswerType.RECOMMENDATION || answer.source == AnswerSource.BREEDING_ENGINE) {
            return 1200L
        }

        // User data queries - medium
        if (answer.source == AnswerSource.USER_DATA) {
            return 800L
        }

        // Knowledge base queries - simple
        if (answer.source == AnswerSource.POULTRY_KNOWLEDGE_BASE || answer.source == AnswerSource.APP_KNOWLEDGE_BASE) {
            return 500L
        }

        // Fallback or others
        return 400L
    }
}
