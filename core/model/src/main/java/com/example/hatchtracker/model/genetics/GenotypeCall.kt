package com.example.hatchtracker.model.genetics

enum class Certainty {
    CONFIRMED, // User explicitly set or observed breeding result
    ASSUMED,   // Derived from Breed Standard
    UNKNOWN    // Missing or invalid data
}

data class GenotypeCall(
    val locusId: String,
    val alleles: List<String>,
    val certainty: Certainty
)
