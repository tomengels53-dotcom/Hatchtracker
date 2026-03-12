package com.example.hatchtracker.model

/**
 * Represents the official or community-defined characteristics of a specific breed.
 */
data class BreedStandard(
    val id: String = "", // Firestore Document ID
    val name: String = "",
    val origin: String = "",
    val species: String = "Chicken", // Link to IncubationProfile via species name
    val eggColor: String = "",
    val acceptedColors: List<String> = emptyList(),
    val weightRoosterKg: Double = 0.0,
    val weightHenKg: Double = 0.0,
    val size: String = "Unknown", // "Small", "Medium", "Large"
    val weightClass: String = "Unknown", // "Light", "Medium", "Heavy"
    val eggProduction: Int = 0, // Eggs per year
    val official: Boolean = false,
    
    // Genetic and Classification Data
    val recognizedBy: List<String> = emptyList(),
    val category: String? = null, // "large_fowl" | "bantam" | "ornamental"
    val isTrueBantam: Boolean = false,
    val ornamentalPurpose: List<String> = emptyList(),
    
    // Physical Markers for Genetic Inference
    val combType: String = "single", // "single", "pea", "rose", "walnut", "v-shape"
    val featherType: String = "normal", // "normal", "frizzle", "silkied", "feather-footed"
    val skinColor: String = "yellow", // "yellow", "white", "black" (fibromelanosis)
    val earlobeColor: String = "red", // "red", "white"
    
    // Genetics B2/Phase 2: Hybrid & Crossbreeding Metadata
    val breedType: com.example.hatchtracker.model.genetics.BreedType = com.example.hatchtracker.model.genetics.BreedType.HERITAGE,
    val geneticTags: List<String> = emptyList(),
    val crossbreedingNotes: String? = null,
    
    val geneticProfile: GeneticProfile = GeneticProfile(),

    // --- Normalized Trait Fields ---

    // Egg / Laying traits
    val eggProductionPerYear: Int? = null,
    val eggSize: String? = null, // "Small", "Medium", "Large", "Extra Large"
    val layingStartAgeWeeks: Int? = null,
    val winterLayingAbility: TraitLevel? = null,
    val broodinessLevel: TraitLevel? = null,
    val normalizedEggColor: EggColor? = null,

    // Temperament / Behavior
    val temperament: String? = null, // Keeping for now, or could use an enum if list is stable
    val noiseLevel: TraitLevel? = null,
    val humanFriendliness: TraitLevel? = null,
    val flockCompatibility: TraitLevel? = null,
    val motheringAbility: TraitLevel? = null,
    val normalizedTemperament: TemperamentLevel? = null,
    val normalizedBodySize: BodySizeClass? = null,

    // Climate / Hardiness
    val coldHardiness: TraitLevel? = null,
    val heatTolerance: TraitLevel? = null,
    val incubationDurationDays: Int? = null,
    val nurseryGraduationDays: Int? = null,
    val freeRangeAbility: TraitLevel? = null,
    val confinementTolerance: TraitLevel? = null,
    val foragingAbility: TraitLevel? = null,

    // Utility / Purpose
    val primaryUse: List<String> = emptyList(), // "layer", "meat", "dual_purpose", "ornamental", "conservation"
    val growthRate: GrowthRate? = null,
    val carcassQuality: TraitLevel? = null,
    val normalizedPrimaryUsage: List<PrimaryUsage> = emptyList(),

    // Physical / Conformation
    val legColor: String? = null,
    val shankFeathering: Boolean? = null,
    val crestType: String? = null,
    val muffBeard: Boolean? = null,
    val isRumpless: Boolean? = null,
    val isTufted: Boolean? = null,
    val normalizedCombType: CombType? = null,

    // Health / Robustness
    val diseaseResistance: String? = null, // "Low", "Medium", "High"
    val parasiteResistance: String? = null, // "Low", "Medium", "High"
    val lifespanRangeYears: String? = null,

    // Reproduction
    val fertilityRate: TraitLevel? = null,
    val hatchability: TraitLevel? = null,
    val roosterTemperament: String? = null, // Keeping free-form for nuance or use temperament enum if preferred

    // Architectural Metadata for scalability
    val lastUpdated: Long? = null,
    val metadata: Map<String, String> = emptyMap(), // For future extensibility (e.g. "source_url", "contributor")
    val tags: List<String> = emptyList(), // For searching and categorization (e.g. "threatened", "heritage")

    // Species-Specific Traits
    val duckTraits: DuckTraits? = null,
    val gooseTraits: GooseTraits? = null,
    val turkeyTraits: TurkeyTraits? = null,
    val quailTraits: QuailTraits? = null
) {
    val eggProductionLabel: String
        get() = when {
            eggProduction > 240 -> "Prolific"
            eggProduction >= 150 -> "Normal"
            else -> "Not Effective"
        }
}

/**
 * Species-specific extension traits for Waterfowl (Ducks).
 */
data class DuckTraits(
    val waterAffinityLevel: WaterAffinity? = null,
    val flightAbility: FlightAbility? = null,
    val seasonalLayingPattern: SeasonalPattern? = null,
    val shellColorDuckScale: String? = null, // "White", "Green", "Blue", "Sooty"
    val fatDepositionRate: TraitLevel? = null
)

/**
 * Species-specific extension traits for Geese.
 */
data class GooseTraits(
    val guardingInstinct: TraitLevel? = null,
    val grazingDependency: TraitLevel? = null,
    val territorialAggression: TerritorialBehavior? = null,
    val pairBondStrength: BondStrength? = null
)

/**
 * Species-specific extension traits for Turkeys.
 */
data class TurkeyTraits(
    val breastMeatYield: YieldLevel? = null,
    val displayAggression: TraitLevel? = null,
    val matingSuccessNatural: SuccessRate? = null,
    val growthBurstPattern: GrowthPattern? = null
)

/**
 * Species-specific extension traits for Quail.
 */
data class QuailTraits(
    val earlyMaturityRate: GrowthRate? = null,
    val eggFrequencyCycle: EggFrequency? = null,
    val colonyDensityTolerance: TraitLevel? = null,
    val stressSensitivity: TraitLevel? = null
)

// --- Controlled Enums for Normalized Traits ---

enum class TraitLevel { LOW, MEDIUM, HIGH }
enum class WaterAffinity { TERRESTRIAL, SEMI_AQUATIC, AQUATIC_DEPENDENT }
enum class FlightAbility { NONE, POOR, GOOD, STRONG }
enum class SeasonalPattern { SPRING_ONLY, EXTENDED_SPRING, INTERMITTENT, YEAR_ROUND }
enum class TerritorialBehavior { CALM, PROTECTIVE, AGGRESSIVE }
enum class BondStrength { WEAK, MODERATE, LIFELONG }
enum class YieldLevel { LOW, MEDIUM, HIGH, EXTREME }
enum class SuccessRate { NONE, LOW, AVERAGE, EXCELLENT }
enum class GrowthPattern { STEADY, DELAYED, RAPID_INITIAL }
enum class GrowthRate { SLOW, MEDIUM, FAST, VERY_FAST }
enum class EggFrequency { DAILY, CLUTCH_BASED, SEASONAL }

