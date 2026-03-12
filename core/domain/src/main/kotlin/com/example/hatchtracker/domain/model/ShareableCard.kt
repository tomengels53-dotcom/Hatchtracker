package com.example.hatchtracker.domain.model

/**
 * Model for branded result cards optimized for social sharing.
 */
data class ShareableCard(
    val title: String,
    val subtitle: String,
    val stats: List<CardStat>,
    val imageUrl: String?,
    val brandingText: String = "Tracked in HatchBase",
    val cardType: CardType,
    val privacyDisclaimer: String? = null
)

data class CardStat(
    val label: String,
    val value: String,
    val isPrimary: Boolean = false
)

enum class CardType {
    HATCH_RESULT,
    BREEDING_PROJECT,
    FLOCK_SUMMARY,
    BIRD_PASSPORT,
    MARKETPLACE_LISTING
}
