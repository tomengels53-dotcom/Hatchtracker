package com.example.hatchtracker.model

import com.example.hatchtracker.model.UiText

import com.example.hatchtracker.model.Bird

/**
 * Strategic breeding recommendation result with detailed rationale.
 * PRO-only feature.
 */
data class StrategicRecommendation(
    val male: Bird,
    val female: Bird,
    val score: Int,
    val strategicRationale: UiText,
    val goalMatches: List<BreedingGoalType>,
    val traitAnalysis: UiText,
    val riskFactors: List<String>,
    val expectedOutcomes: List<String>
)

