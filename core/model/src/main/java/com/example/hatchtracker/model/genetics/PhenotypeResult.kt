package com.example.hatchtracker.model.genetics

import com.example.hatchtracker.data.models.ConfidenceLevel

data class PhenotypeProbability(
    val phenotypeId: String,
    val probability: Double
)

data class PhenotypeResult(
    val probabilities: List<PhenotypeProbability>,
    val assumptions: List<String> = emptyList(),
    val confidence: ConfidenceLevel = ConfidenceLevel.LOW,
    val resolverVersion: Int = 1
) {
    fun probabilityOf(id: String): Double =
        probabilities.firstOrNull { it.phenotypeId == id }?.probability ?: 0.0

    fun has(id: String, atLeast: Double = 0.5): Boolean =
        probabilityOf(id) >= atLeast
}
