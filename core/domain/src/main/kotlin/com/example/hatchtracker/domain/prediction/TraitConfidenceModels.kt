package com.example.hatchtracker.domain.prediction

/**
 * Represents the dynamic confidence state of a specific trait for a breed.
 */
data class TraitConfidenceState(
    val id: String = "", // Composite key: "breedId_traitId"
    val breedId: String = "",
    val traitId: String = "",
    
    // Scoring
    val confidenceScore: Double = 0.0, // 0.0 to 100.0 (Bayesian posterior probability * 100)
    val confidenceLevel: ConfidenceLevel = ConfidenceLevel.LOW,
    
    // Evidence Counters
    val supportingObservations: Int = 0, // Count of verified "confirmed" votes
    val conflictingObservations: Int = 0, // Count of verified "disputed" votes
    val multiGenConfirmations: Int = 0, // Bonus for traits persisting across generations
    
    // Isolation Metrics
    val isolationScore: Double = 1.0, // 0.0 (High interference) to 1.0 (Strict isolation)
    
    // Metadata
    val lastUpdated: Long = System.currentTimeMillis(),
    val events: List<ConfidenceEvent> = emptyList() // Audit trail of major score changes
)

enum class ConfidenceLevel(val weight: Double) {
    LOW(0.25),      // < 50: Speculative
    MEDIUM(0.50),   // 50-75: Inferred likely
    HIGH(0.75),     // 75-90: Reliable recommendation
    FIXED(1.0)      // > 90: Accepted Official Standard (Requires Admin Lock)
}

data class ConfidenceEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val delta: Double = 0.0,
    val reason: String = "" // e.g. "Community Consensus +5", "Conflict Detected -10"
)
