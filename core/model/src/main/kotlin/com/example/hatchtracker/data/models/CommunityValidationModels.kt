package com.example.hatchtracker.data.models

/**
 * Represents a user's observation of a specific trait in a hatched bird.
 */
data class TraitObservation(
    val id: String = "",
    val breedId: String = "", // e.g., "ameraucana"
    val traitId: String = "", // e.g., "blue_egg_gene"
    val observedValue: String = "", // e.g., "present", "absent", "heterozygous"
    val confidence: Double = 0.0, // User's self-reported confidence (0.0 - 1.0)
    
    // Context
    val parentPairId: String = "",
    val incubationProfileId: String? = null,
    val environmentalNotes: String? = null, // e.g., "Temp spike day 18"
    
    // Evidence
    val evidenceType: EvidenceType = EvidenceType.NONE,
    val photoUrls: List<String> = emptyList(),
    
    // Metadata
    val userId: String = "", // Hashed or obfuscated for public view
    val timestamp: Long = System.currentTimeMillis()
)

enum class EvidenceType {
    NONE, HATCH_LOG, PHOTO, GENETIC_TEST
}

/**
 * Represents a community vote on a specific observation.
 */
data class TraitVote(
    val observationId: String = "",
    val userId: String = "", // Voter ID
    val voteTokens: Int = 1, // Weighted by reputation
    val agree: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Tracks the aggregate confidence of a trait for a specific breed.
 */
data class TraitConfidence(
    val breedId: String = "",
    val traitId: String = "",
    val currentScore: Double = 0.0, // 0.0 to 100.0
    val status: TraitStatus = TraitStatus.UNKNOWN,
    val history: List<ConfidencePoint> = emptyList()
)

data class ConfidencePoint(
    val timestamp: Long = System.currentTimeMillis(),
    val score: Double = 0.0,
    val reason: String = "" // e.g. "Vote spike", "Admin override"
)

enum class TraitStatus {
    UNKNOWN, INFERRED, FIXED, OFFICIAL
}

