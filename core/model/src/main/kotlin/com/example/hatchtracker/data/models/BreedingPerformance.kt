package com.example.hatchtracker.data.models

data class BreedingPerformance(
    val totalEggsSet: Int,
    val totalChicksHatched: Int,
    val avgHatchRate: Float,
    val infertilityRate: Float,
    val successfulIncubationsCount: Int
)

