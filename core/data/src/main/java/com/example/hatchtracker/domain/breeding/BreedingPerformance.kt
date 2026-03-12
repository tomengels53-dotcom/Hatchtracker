package com.example.hatchtracker.domain.breeding

data class BreedingPerformance(
    val totalEggsSet: Int,
    val totalChicksHatched: Int,
    val avgHatchRate: Float,
    val infertilityRate: Float,
    val successfulIncubationsCount: Int
)
