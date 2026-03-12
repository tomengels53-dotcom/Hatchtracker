package com.example.hatchtracker.domain.prediction

@Deprecated(
    message = "Duplicate model. Use core/model GeneticProfile instead.",
    replaceWith = ReplaceWith("com.example.hatchtracker.model.GeneticProfile")
)
data class GeneticProfile(
    val knownGenes: List<String> = emptyList(),
    val fixedTraits: List<String> = emptyList(),
    val inferredTraits: List<String> = emptyList(),
    val unknownTraits: List<String> = emptyList(),
    val confidenceLevel: ConfidenceLevel = ConfidenceLevel.LOW
)
