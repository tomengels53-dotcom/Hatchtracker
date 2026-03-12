package com.example.hatchtracker.data.models

/**
 * Defines the anchor for identity preservation and backcrossing.
 */
data class BaseLineDefinition(
    val baseBreed: String?,
    val baseFlockId: String?,
    val preserveIdentity: Boolean,
    val identityTolerance: Double = 0.125 // i.e., 87.5% base blood (BC2/BC3 range)
)

/**
 * Profile used for composite bases where multiple breeds are merged.
 */
data class CompositeBaseProfile(
    val dominantTraits: List<String>,
    val targetIdentityTolerance: Double = 0.25, // 75% stability
    val dilutionBenchmark: String // The "anchor" breed name even if composite
)

/**
 * Represents the high-level roadmap for a breeding project.
 */
data class MultiBreedRoadmap(
    val id: String,
    val baseLine: BaseLineDefinition,
    val compositeProfile: CompositeBaseProfile? = null,
    val stages: List<RoadmapStage>,
    val overallGenEstimate: GenEstimate
)

/**
 * Types of stages in a multi-breed plan.
 */
enum class RoadmapStageType {
    INTROGRESS, // Initial cross to acquire donor genes
    BACKCROSS,  // Return to base line to recover phenotype
    INTERCROSS, // Selfing/Sib-mating to allow recessive segregation
    FIXATION,   // Narrowing to homozygous state (Strict)
    STABILIZE   // Establishing performance consistency (Commercial)
}

/**
 * A single stage in the breeding roadmap.
 */
data class RoadmapStage(
    val stageIndex: Int,
    val type: RoadmapStageType,
    val targetTraits: List<String>,
    val sireSource: String, // Breed name or "Base Line" or "Previous Gen"
    val damSource: String,
    val selectionRules: List<String>,
    val expectedOutcome: String,
    val whyThisStage: String,
    val genEstimate: GenEstimate,
    val donorFractionBefore: Double,
    val donorFractionAfter: Double
)
