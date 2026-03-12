package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.model.StrategicRecommendation

/**
 * Suggested breeding scenario with multiple pairing options.
 * PRO-only feature.
 */
data class SuggestedScenario(
    val scenarioName: String,
    val description: String,
    val recommendations: List<StrategicRecommendation>,
    val overallStrategy: String
)
