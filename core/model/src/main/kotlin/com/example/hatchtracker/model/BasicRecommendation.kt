package com.example.hatchtracker.model

import com.example.hatchtracker.model.UiText

/**
 * Result wrapper for basic breeding recommendations (available to all users).
 */
data class BasicRecommendation(
    val male: Bird,
    val female: Bird,
    val score: Int,
    val basicSummary: UiText
)
