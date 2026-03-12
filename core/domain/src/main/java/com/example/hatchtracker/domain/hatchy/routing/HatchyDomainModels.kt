package com.example.hatchtracker.domain.hatchy.routing

enum class HatchyDomain {
    BREEDING,
    INCUBATION,
    NURSERY,
    FINANCE,
    EQUIPMENT,
    GENERAL_POULTRY,
    FLOCK,
    APP_DATA,
    SYSTEM
}

/**
 * Type-safe domain models for Hatchy entity and topic matching.
 * Replaces string-based identifiers for better maintainability and logic.
 */

enum class PoultrySpecies {
    CHICKEN,
    DUCK,
    GOOSE,
    TURKEY,
    QUAIL,
    GUINEA_FOWL,
    PEAFOWL,
    UNKNOWN
}

sealed class PoultryTopic {
    object Care : PoultryTopic()
    object Health : PoultryTopic()
    object Behavior : PoultryTopic()
    object Housing : PoultryTopic()
    object Feeding : PoultryTopic()
    object Other : PoultryTopic()
}

sealed class IncubationTopic {
    object Duration : IncubationTopic()
    object Humidity : IncubationTopic()
    object Temperature : IncubationTopic()
    object Candling : IncubationTopic()
    object Turning : IncubationTopic()
    object Lockdown : IncubationTopic()
    object HatchDay : IncubationTopic()
    object Health : IncubationTopic()
    object Other : IncubationTopic()
}

sealed class NurseryStatusTopic {
    object ActiveCount : NurseryStatusTopic()
    object ReadyToMove : NurseryStatusTopic()
    object BrooderStatus : NurseryStatusTopic()
    object Losses : NurseryStatusTopic()
    object AgeGroups : NurseryStatusTopic()
    object Other : NurseryStatusTopic()
}

sealed class NurseryGuidanceTopic {
    object Temperature : NurseryGuidanceTopic()
    object Feeding : NurseryGuidanceTopic()
    object CoopTransition : NurseryGuidanceTopic()
    object Other : NurseryGuidanceTopic()
}

sealed class FinanceSummaryTopic {
    object TotalSpend : FinanceSummaryTopic()
    object CategoryBreakdown : FinanceSummaryTopic()
    object FlockCost : FinanceSummaryTopic()
    object MonthlyTrend : FinanceSummaryTopic()
    object RecentExpenses : FinanceSummaryTopic()
    object Other : FinanceSummaryTopic()
}

sealed class FinanceHelpTopic {
    object LogExpense : FinanceHelpTopic()
    object EditEntry : FinanceHelpTopic()
    object Other : FinanceHelpTopic()
}

sealed class EquipmentStatusTopic {
    object ActiveDevices : EquipmentStatusTopic()
    object SensorStatus : EquipmentStatusTopic()
    object Capacity : EquipmentStatusTopic()
    object Maintenance : EquipmentStatusTopic()
    object Alerts : EquipmentStatusTopic()
    object Other : EquipmentStatusTopic()
}

sealed class EquipmentHelpTopic {
    object AddDevice : EquipmentHelpTopic()
    object Other : EquipmentHelpTopic()
}

sealed class BreedingTopic {
    object LineBreeding : BreedingTopic()
    object CrossBreeding : BreedingTopic()
    object Selection : BreedingTopic()
    object Genetics : BreedingTopic()
    object Stability : BreedingTopic()
    object Advice : BreedingTopic()
    object Simulation : BreedingTopic()
    object Outcome : BreedingTopic()
    object Other : BreedingTopic()
}

enum class BreedingGoal {
    EGG_PRODUCTION,
    MEAT_PRODUCTION,
    DUAL_PURPOSE,
    SHOW_ORNAMENTAL,
    TEMPERAMENT,
    PLUMAGE,
    HEALTH_VIGOR,
    UNKNOWN
}

enum class EquipmentCategory {
    INCUBATOR,
    BROODER,
    COOP,
    FEEDER,
    WATERER,
    SENSOR,
    OTHER
}

enum class FinancePeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    YEARLY,
    CUSTOM
}

/**
 * Structured outcome from a resolver execution.
 */
sealed class ResolverOutcome {
    data class Resolved(val answer: HatchyAnswer) : ResolverOutcome()
    object NotApplicable : ResolverOutcome()
    data class InsufficientEvidence(val fallbackAnswer: HatchyAnswer? = null) : ResolverOutcome()
}

/**
 * Debug trace for Hatchy routing decisions.
 */
data class RoutingTrace(
    val query: String,
    val primaryQuestionMode: String?,
    val secondaryQuestionMode: String?,
    val modeConfidence: Double?,
    val appAnchorScore: Double?,
    val realWorldAnchorScore: Double?,
    val userDataAnchorScore: Double?,
    val classifiedDomain: String,
    val selectedSubtype: String?,
    val candidates: List<String>,
    val rejected: Map<String, String>,
    val selectedResolver: String?,
    val confidenceInputs: Map<String, Double>,
    val evidenceKeys: List<String>,
    val scoreComponents: Map<String, Double> = emptyMap()
)
