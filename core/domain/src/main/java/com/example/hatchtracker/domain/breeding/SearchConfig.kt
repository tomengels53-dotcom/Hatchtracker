package com.example.hatchtracker.domain.breeding

/**
 * Configuration for the Breeding Strategy Search Engine.
 */
data class SearchConfig(
    val beamWidth: Int = 20,
    val maxCandidatesPerGeneration: Int = 100,
    val topKPlans: Int = 5,
    val maxGenerations: Int = 10,
    val maxEvaluationCount: Int = 10000 // Higher cap for deeper search
)
