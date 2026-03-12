package com.example.hatchtracker.domain.model

/**
 * A render-safe, privacy-aware snapshot of an internal app entity.
 * Used for linking entities in Community posts and Marketplace listings.
 */
data class EntityPassportSnapshot(
    val entityType: String, // BIRD, FLOCK, INCUBATION, FLOCKLET, DEVICE, BREEDING_PLAN
    val entityId: String,
    val title: String,
    val subtitle: String,
    val heroImageUrl: String?,
    val snapshotVersion: Int = 1,
    val generatedAt: Long = System.currentTimeMillis(),
    
    /**
     * Strictly typed display metadata (e.g., "Breed" -> "Leghorn", "Status" -> "Active").
     * Constrained to String/String to ensure stable serialization and rendering.
     */
    val displayMetadata: Map<String, String> = emptyMap()
)

/**
 * Context in which an entity is being shared, determining the privacy mask.
 */
enum class ShareContext {
    SOCIAL_SHOWCASE, // General community post
    SOCIAL_HELP,     // Asking a question (may need more health/environment stats)
    MARKETPLACE      // Selling (focused on genetics, pedigree, and health)
}
