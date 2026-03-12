package com.example.hatchtracker.domain.breeding.plan

data class PlanConstraints(
    val maxGenerations: Int = 10,
    val requiredMinProbability: Double = 0.65,
    val avoidMaxProbability: Double = 0.30,
    val diversityWeight: Double = 1.0,
    val limitSireReuse: Boolean = true,
    val preferShorter: Boolean = true
)
