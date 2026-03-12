package com.example.hatchtracker.data.models

enum class DisclaimerType {
    GLOBAL_GENETICS,      // App-wide warning (shown on first breed screen open)
    BREEDING_RECOMMENDATION, // Shown inline on breeding results
    TRAIT_CONFIDENCE,     // "Why is this low confidence?"
    COMMUNITY_DATA        // "This data came from users, not science"
}

data class LegalDisclaimer(
    val id: String = "", // e.g., "global_genetics_v1"
    val type: DisclaimerType = DisclaimerType.GLOBAL_GENETICS,
    val version: Int = 1,
    val title: String = "",
    val text: String = "",
    val effectiveDate: Long = System.currentTimeMillis(),
    val requiresReaccept: Boolean = true // If true, forces modal on next view
)

data class DisclaimerAcknowledgement(
    val disclaimerId: String = "", // Matches legalDisclaimers document ID
    val disclaimerVersion: Int = 0,
    val acknowledgedAt: Long = System.currentTimeMillis()
)

