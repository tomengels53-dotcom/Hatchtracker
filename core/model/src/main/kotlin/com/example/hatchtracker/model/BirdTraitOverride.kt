package com.example.hatchtracker.model

import com.example.hatchtracker.data.models.TraitCategory

/**
 * Represents a manual trait override for a specific bird.
 * This is used to store bird-level variations (e.g. Mixed breeds with specific egg colors)
 * without affecting the underlying breed standard.
 */
data class BirdTraitOverride(
    val traitId: String,
    val optionId: String,
    val category: TraitCategory,
    val notes: String? = null,
    val overrideTimestamp: Long = System.currentTimeMillis()
)
