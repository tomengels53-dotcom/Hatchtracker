package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.model.genetics.GenotypeCall
import com.example.hatchtracker.model.genetics.GenotypeDistribution
import com.example.hatchtracker.model.genetics.PhenotypeResult
import com.example.hatchtracker.domain.breeding.quant.QuantitativeTraitId

/**
 * Structured result for breeding predictions.
 */
data class BreedingPredictionResult(
    val sireGenotypes: Map<String, GenotypeCall> = emptyMap(),
    val damGenotypes: Map<String, GenotypeCall> = emptyMap(),
    val offspringDistributions: Map<String, GenotypeDistribution> = emptyMap(),
    val phenotypeResult: PhenotypeResult,
    val phenotypeOutcomes: List<PhenotypeOutcome> = emptyList(),
    val quantitativePredictions: List<QuantitativePrediction> = emptyList(),
    val lineStability: LineStabilitySnapshot? = null
)

data class QuantitativePrediction(
    val traitKey: String,
    val mean: Double,
    val variance: Double,
    val heritability: Double,
    val unit: String = "",
    val rangeMin: Double = 0.0,
    val rangeMax: Double = 1.0
)
