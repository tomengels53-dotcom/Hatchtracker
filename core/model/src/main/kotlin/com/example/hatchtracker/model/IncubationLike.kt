package com.example.hatchtracker.model

/**
 * Minimal incubation contract for pure Kotlin helpers.
 */
interface IncubationLike {
    val id: Long
    val species: String
    val startDate: String
    val hatchCompleted: Boolean
    val lifecycleStage: com.example.hatchtracker.model.BirdLifecycleStage
}

