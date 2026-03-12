package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.HatchyTopic

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Routes user queries to either the knowledge base or specialized aggregate data queries.
 * Enforces AI safety by preventing over-fetching or individual record leakage.
 */
@Singleton
class HatchyQueryRouter @Inject constructor(
    private val assistantRepository: HatchyAssistantRepository
) {
    /**
     * Determines if a query can be answered with local aggregate data.
     * Returns a string context for the LLM if data is found.
     */
    suspend fun fetchDataContext(intent: HatchyIntentResult, rawQuery: String): String? {
        val q = rawQuery.lowercase()
        
        // Only provide data context for specific intents to minimize leakage
        return when (intent.intent) {
            HatchyIntent.LIFECYCLE, HatchyIntent.APP_NAVIGATION -> {
                if (q.contains("how many") || q.contains("count") || q.contains("total")) {
                    assistantRepository.getGlobalFlockSummary()
                } else null
            }
            HatchyIntent.BREEDING_GUIDANCE, HatchyIntent.CROSSBREED_OUTCOME -> {
                assistantRepository.getBreedingSummary()
            }
            else -> null
        }
    }
}
