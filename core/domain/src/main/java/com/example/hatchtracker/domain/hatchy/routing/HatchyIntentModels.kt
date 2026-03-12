package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.HatchyTopic
import com.example.hatchtracker.model.KnowledgeTopic
import com.example.hatchtracker.model.WorkflowTopic
import com.example.hatchtracker.model.DataTopic

enum class HatchyIntent {
    // Breeding Domain
    BREED_INFO,
    BREED_COMPARISON,
    CROSSBREED_OUTCOME,
    BREEDING_GUIDANCE,
    USER_FLOCK_RECOMMENDATION,

    // Incubation Domain
    INCUBATION_GUIDANCE,
    INCUBATION_STATUS,

    // Nursery Domain
    NURSERY_GUIDANCE,
    NURSERY_STATUS,

    // Finance Domain
    FINANCE_HELP,
    FINANCE_SUMMARY,

    // Equipment Domain
    EQUIPMENT_HELP,
    EQUIPMENT_STATUS,

    // General Poultry Knowledge
    GENERAL_POULTRY,
    POULTRY_HEALTH,

    // App & User Data
    APP_NAVIGATION,
    USER_DATA_QUERY,
    
    // System & Lifecycle
    LIFECYCLE,
    BILLING_SUBSCRIPTION,
    PAYWALL_BYPASS_ATTEMPT,
    TROUBLESHOOTING,
    OTHER,
    FALLBACK
}

data class HatchyIntentResult(
    val intent: HatchyIntent,
    val module: String? = null,
    val entities: List<HatchyEntity> = emptyList(),
    val confidence: Double = 0.0,
    val matchedKeywords: List<String> = emptyList(),
    val bypassScore: Int = 0,
    val questionModeResult: QuestionModeResult? = null
)

data class HatchyEntity(
    val type: EntityType,
    val value: String,
    val originalText: String,
    val confidence: Double = 1.0,
    val metadata: Map<String, Any> = emptyMap()
)

enum class EntityType {
    BREED,
    SPECIES,
    POULTRY_SPECIES,
    POULTRY_TOPIC,
    INCUBATION_TOPIC,
    NURSERY_STATUS_TOPIC,
    NURSERY_GUIDANCE_TOPIC,
    FINANCE_SUMMARY_TOPIC,
    FINANCE_HELP_TOPIC,
    EQUIPMENT_STATUS_TOPIC,
    EQUIPMENT_HELP_TOPIC,
    BREEDING_TOPIC,
    BREEDING_GOAL,
    FINANCE_PERIOD,
    EQUIPMENT_CAT,
    SYMPTOM,
    TRAIT,
    MODULE,
    USER_DATA_REF, // flock, bird, batch, etc.
    TIME_PERIOD,
    DATE,
    ACTION
}

data class HatchyAnswer(
    val text: String,
    val type: AnswerType,
    val confidence: AnswerConfidence,
    val source: AnswerSource,
    val suggestedActions: List<HatchyAction> = emptyList(),
    val relatedEntities: List<HatchyEntity> = emptyList(),
    val secondaryActionHint: String? = null,
    val debugMetadata: Map<String, Any>? = null
)

enum class AnswerType {
    NAVIGATION,
    BREED_INFO,
    CROSSBREEDING,
    GUIDANCE,
    POULTRY_KNOWLEDGE,
    INCUBATION,
    NURSERY,
    FINANCE,
    EQUIPMENT,
    RECOMMENDATION,
    FALLBACK
}

enum class AnswerConfidence {
    HIGH, MEDIUM, LOW, VERY_LOW
}

enum class AnswerSource {
    USER_DATA,
    BREED_REPOSITORY,
    BREEDING_ENGINE,
    POULTRY_KNOWLEDGE_BASE,
    APP_KNOWLEDGE_BASE,
    FALLBACK
}

data class HatchyAction(
    val label: String,
    val route: String? = null,
    val actionId: String? = null,
    val params: Map<String, String> = emptyMap()
)

// Topics moved to :core:model to resolve circular dependency

data class TopicInferenceResult(
    val primaryTopic: HatchyTopic?,
    val secondaryTopic: HatchyTopic?,
    val topicScores: Map<HatchyTopic, Double>,
    val confidence: Double
)

data class QueryInterpretation(
    val rawQuery: String,
    val questionMode: QuestionModeResult,
    val entities: List<HatchyEntity>,
    val intent: HatchyIntent,
    val topicResult: TopicInferenceResult,
    val inferredGoals: List<BreedingGoal>,
    val confidence: Double,
    val module: String? = null
)

data class ResolverCapabilities(
    val supportedIntents: Set<HatchyIntent> = emptySet(),
    val supportedTopics: Set<HatchyTopic> = emptySet(),
    val requiredEntities: Set<EntityType> = emptySet(),
    val optionalEntities: Set<EntityType> = emptySet(),
    val allowsApproximateMatch: Boolean = false,
    val preferredQuestionModes: Set<QuestionMode> = emptySet(),
    val priority: Int = ResolverPriority.KNOWLEDGE,
    val requiresUserData: Boolean = false,
    val supportsFollowUpContext: Boolean = false,
    val canReturnConstrainedFallback: Boolean = false
)

sealed interface HatchyProcessEvent {
    data class Thinking(val label: String?) : HatchyProcessEvent
    data class Done(val answer: HatchyAnswer) : HatchyProcessEvent
}
