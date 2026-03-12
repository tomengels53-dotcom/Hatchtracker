package com.example.hatchtracker.model.genetics

data class QuantitativeTraitModel(
    val traitKey: String,
    val heritability: Double,
    val environmentalVariance: Double,
    val dominanceStrength: Double = 0.0,   // 0.0 to 1.0
    val dominanceDirection: Int = 1,       // +1 or -1
    val dominanceCenter: Double = 0.5      // 0.0 to 1.0
)

data class QuantitativeTraitValue(
    val mean: Double,
    val variance: Double = 0.0
)
