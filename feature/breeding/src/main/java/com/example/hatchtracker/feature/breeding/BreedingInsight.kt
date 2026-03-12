package com.example.hatchtracker.feature.breeding

import com.example.hatchtracker.model.UiText

enum class InsightSeverity {
    INFO, WARNING, CRITICAL
}

data class BreedingInsight(
    val severity: InsightSeverity,
    val title: UiText,
    val body: UiText,
    val actionHint: UiText? = null
)
