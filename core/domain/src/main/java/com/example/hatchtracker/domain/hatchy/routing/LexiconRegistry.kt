package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.*

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LexiconRegistry @Inject constructor() : ILexiconRegistry {

    override fun normalize(query: String): String {
        return HatchyNormalization.normalize(query)
    }

    override fun matchTopics(query: String): Map<HatchyTopic, Double> {
        val normalizedQuery = normalize(query)
        val scores = mutableMapOf<HatchyTopic, Double>()
        
        // Use consolidated topic maps from HatchyLexicon
        val allTopicMappings = listOf(
            HatchyLexicon.IncubationTopics,
            HatchyLexicon.BreedingTopics,
            HatchyLexicon.NurseryStatusTopics,
            HatchyLexicon.NurseryGuidanceTopics,
            HatchyLexicon.FinanceSummaryTopics,
            HatchyLexicon.FinanceHelpTopics,
            HatchyLexicon.EquipmentStatusTopics,
            HatchyLexicon.EquipmentHelpTopics,
            HatchyLexicon.PoultryTopics,
            HatchyLexicon.TraitTopics
        )

        // Add phrases from the main topicLexicon (which can have multiple topics per phrase)
        topicLexicon.forEach { (phrase, topics) ->
            val normalizedPhrase = normalize(phrase)
            updateScores(normalizedQuery, normalizedPhrase, topics, scores)
        }

        // Add phrases from single-topic mappings
        allTopicMappings.forEach { map ->
            map.forEach { (phrase, topic) ->
                val normalizedPhrase = normalize(phrase)
                val mappedTopic = when (topic) {
                    is KnowledgeTopic -> topic
                    else -> topic.toHatchyTopic()
                } ?: return@forEach
                updateScores(normalizedQuery, normalizedPhrase, setOf(mappedTopic), scores)
            }
        }
        
        val maxScore = scores.values.maxOrNull() ?: return emptyMap()
        if (maxScore > 0.0) {
            scores.replaceAll { _, v -> (v / maxScore).coerceAtMost(1.0) }
        }
        
        return scores
    }

    private fun updateScores(
        normalizedQuery: String,
        normalizedPhrase: String,
        topics: Set<HatchyTopic>,
        scores: MutableMap<HatchyTopic, Double>
    ) {
        if (normalizedQuery == normalizedPhrase) {
            topics.forEach { topic ->
                scores[topic] = (scores[topic] ?: 0.0) + 1.0
            }
        } else if (normalizedQuery.contains(normalizedPhrase)) {
            val weight = if (normalizedPhrase.contains(" ")) 0.8 else 0.6
            topics.forEach { topic ->
                scores[topic] = (scores[topic] ?: 0.0) + weight
            }
        }
    }

    private fun Any.toHatchyTopic(): HatchyTopic? = when (this) {
        IncubationTopic.Duration -> KnowledgeTopic.INCUBATION_PERIOD
        IncubationTopic.Humidity -> KnowledgeTopic.HUMIDITY
        IncubationTopic.Temperature -> KnowledgeTopic.TEMPERATURE
        IncubationTopic.Turning -> KnowledgeTopic.TURNING
        IncubationTopic.Lockdown -> KnowledgeTopic.LOCKDOWN
        IncubationTopic.HatchDay -> KnowledgeTopic.HATCH_TIMING
        NurseryStatusTopic.ActiveCount -> DataTopic.ACTIVE_CHICK_COUNT
        NurseryStatusTopic.ReadyToMove -> KnowledgeTopic.READY_TO_MOVE
        NurseryStatusTopic.Losses -> DataTopic.LOSSES_SUMMARY
        NurseryStatusTopic.AgeGroups -> DataTopic.AGE_GROUP_SUMMARY
        NurseryGuidanceTopic.Temperature -> KnowledgeTopic.BROODER_TEMPERATURE
        NurseryGuidanceTopic.CoopTransition -> KnowledgeTopic.READY_TO_MOVE
        NurseryGuidanceTopic.Feeding -> KnowledgeTopic.EARLY_CHICK_CARE
        FinanceSummaryTopic.TotalSpend -> DataTopic.TOTAL_SPEND
        FinanceSummaryTopic.CategoryBreakdown -> DataTopic.CATEGORY_BREAKDOWN
        FinanceSummaryTopic.FlockCost -> DataTopic.FLOCK_COST
        FinanceSummaryTopic.MonthlyTrend -> DataTopic.MONTHLY_TREND
        FinanceSummaryTopic.RecentExpenses -> DataTopic.CATEGORY_BREAKDOWN
        FinanceHelpTopic.LogExpense, FinanceHelpTopic.EditEntry -> WorkflowTopic.LOG_EXPENSE
        EquipmentStatusTopic.ActiveDevices -> DataTopic.ACTIVE_DEVICES
        EquipmentStatusTopic.SensorStatus -> DataTopic.SENSOR_STATUS
        EquipmentStatusTopic.Maintenance -> KnowledgeTopic.MAINTENANCE_DUE
        EquipmentHelpTopic.AddDevice -> WorkflowTopic.ADD_EQUIPMENT
        BreedingTopic.LineBreeding -> KnowledgeTopic.BREEDING_STRATEGY
        BreedingTopic.CrossBreeding, BreedingTopic.Outcome -> KnowledgeTopic.CROSSBREED_RECOMMENDATION
        BreedingTopic.Selection, BreedingTopic.Advice -> KnowledgeTopic.GOAL_BASED_PAIRING
        BreedingTopic.Genetics -> KnowledgeTopic.TRAIT_INHERITANCE
        BreedingTopic.Stability -> KnowledgeTopic.GENERATION_VARIATION
        BreedingTopic.Simulation -> KnowledgeTopic.CROSSBREED_RECOMMENDATION
        else -> null
    }

    override fun matchGoals(query: String): List<BreedingGoal> {
        val normalizedQuery = normalize(query)
        val goals = mutableSetOf<BreedingGoal>()
        for ((phrase, goal) in goalLexicon) {
            if (normalizedQuery.contains(normalize(phrase))) {
                goals.add(goal)
            }
        }
        return goals.toList()
    }

    override fun matchAnchors(query: String): Map<String, Double> {
        val normalizedQuery = normalize(query)
        val matches = mutableMapOf<String, Double>()
        for ((phrase, anchor) in anchorLexicon) {
            if (normalizedQuery.contains(normalize(phrase))) {
                matches[anchor] = 1.0
            }
        }
        return matches
    }

    // Kotlin-backed maps for topics
    private val topicLexicon: Map<String, Set<HatchyTopic>> = mapOf(
        // Breeding Knowledge
        "what breeds should i cross" to setOf(KnowledgeTopic.CROSSBREED_RECOMMENDATION),
        "cross breeds" to setOf(KnowledgeTopic.CROSSBREED_RECOMMENDATION),
        "what to cross" to setOf(KnowledgeTopic.CROSSBREED_RECOMMENDATION),
        
        "inherit" to setOf(KnowledgeTopic.TRAIT_INHERITANCE),
        "pass down" to setOf(KnowledgeTopic.TRAIT_INHERITANCE),
        "genetics" to setOf(KnowledgeTopic.TRAIT_INHERITANCE),
        "gene" to setOf(KnowledgeTopic.TRAIT_INHERITANCE),
        
        "2nd generation" to setOf(KnowledgeTopic.GENERATION_VARIATION),
        "generation variation" to setOf(KnowledgeTopic.GENERATION_VARIATION),
        "next generation" to setOf(KnowledgeTopic.GENERATION_VARIATION),
        "f2" to setOf(KnowledgeTopic.GENERATION_VARIATION),
        "not the same color" to setOf(KnowledgeTopic.GENERATION_VARIATION),
        
        "strategy" to setOf(KnowledgeTopic.BREEDING_STRATEGY),
        "line breeding" to setOf(KnowledgeTopic.BREEDING_STRATEGY),
        "inbreeding" to setOf(KnowledgeTopic.BREEDING_STRATEGY),
        
        "excellent egg layers" to setOf(KnowledgeTopic.GOAL_BASED_PAIRING),
        "breeding goal" to setOf(KnowledgeTopic.GOAL_BASED_PAIRING),
        "purpose of breeding" to setOf(KnowledgeTopic.GOAL_BASED_PAIRING),
        "pair for" to setOf(KnowledgeTopic.GOAL_BASED_PAIRING),
        "best for" to setOf(KnowledgeTopic.GOAL_BASED_PAIRING),
        
        // --- Trait Map Topics ---
        "egg color" to setOf(KnowledgeTopic.EGG_TRAITS),
        "white eggs" to setOf(KnowledgeTopic.EGG_TRAITS),
        "brown eggs" to setOf(KnowledgeTopic.EGG_TRAITS),
        "blue eggs" to setOf(KnowledgeTopic.EGG_TRAITS),
        "green eggs" to setOf(KnowledgeTopic.EGG_TRAITS),
        "egg size" to setOf(KnowledgeTopic.EGG_TRAITS),
        "production" to setOf(KnowledgeTopic.EGG_TRAITS),
        
        "friendly" to setOf(KnowledgeTopic.TEMPERAMENT),
        "docile" to setOf(KnowledgeTopic.TEMPERAMENT),
        "aggressive" to setOf(KnowledgeTopic.TEMPERAMENT),
        "mean" to setOf(KnowledgeTopic.TEMPERAMENT),
        "calm" to setOf(KnowledgeTopic.TEMPERAMENT),
        
        "cold hardy" to setOf(KnowledgeTopic.HARDINESS),
        "winter" to setOf(KnowledgeTopic.HARDINESS),
        "heat tolerant" to setOf(KnowledgeTopic.HARDINESS),
        "summer" to setOf(KnowledgeTopic.HARDINESS),
        
        "meat" to setOf(KnowledgeTopic.UTILITY_PURPOSE),
        "dual purpose" to setOf(KnowledgeTopic.UTILITY_PURPOSE),
        "ornamental" to setOf(KnowledgeTopic.UTILITY_PURPOSE),
        
        "comb type" to setOf(KnowledgeTopic.PHYSICAL_TRAITS),
        "feathering" to setOf(KnowledgeTopic.PHYSICAL_TRAITS),
        "leg color" to setOf(KnowledgeTopic.PHYSICAL_TRAITS),
        
        // Incubation Knowledge
        "temperature" to setOf(KnowledgeTopic.TEMPERATURE, KnowledgeTopic.BROODER_TEMPERATURE),
        "temp" to setOf(KnowledgeTopic.TEMPERATURE, KnowledgeTopic.BROODER_TEMPERATURE),
        "heat" to setOf(KnowledgeTopic.TEMPERATURE, KnowledgeTopic.BROODER_TEMPERATURE),
        "warm" to setOf(KnowledgeTopic.TEMPERATURE, KnowledgeTopic.BROODER_TEMPERATURE),
        
        "humidity" to setOf(KnowledgeTopic.HUMIDITY),
        "moisture" to setOf(KnowledgeTopic.HUMIDITY),
        "wet" to setOf(KnowledgeTopic.HUMIDITY),
        
        "turn" to setOf(KnowledgeTopic.TURNING),
        "rotate" to setOf(KnowledgeTopic.TURNING),
        
        "lockdown" to setOf(KnowledgeTopic.LOCKDOWN),
        "stop turning" to setOf(KnowledgeTopic.LOCKDOWN),
        
        "when do my eggs hatch" to setOf(KnowledgeTopic.HATCH_TIMING),
        "when will my batch hatch" to setOf(KnowledgeTopic.HATCH_TIMING),
        "hatch timing" to setOf(KnowledgeTopic.HATCH_TIMING),
        
        "how long is the incubation period" to setOf(KnowledgeTopic.INCUBATION_PERIOD),
        "incubation period" to setOf(KnowledgeTopic.INCUBATION_PERIOD),
        "days to hatch" to setOf(KnowledgeTopic.INCUBATION_PERIOD),
        "how many days" to setOf(KnowledgeTopic.INCUBATION_PERIOD),
        
        "setup" to setOf(KnowledgeTopic.SETUP_DEVICE),
        "install" to setOf(KnowledgeTopic.SETUP_DEVICE),

        // Nursery Knowledge
        "brooder heat" to setOf(KnowledgeTopic.BROODER_TEMPERATURE),
        "brooder temperature" to setOf(KnowledgeTopic.BROODER_TEMPERATURE),
        "move to coop" to setOf(KnowledgeTopic.READY_TO_MOVE),
        "move chick to coop" to setOf(KnowledgeTopic.READY_TO_MOVE),
        "move chick to the coop" to setOf(KnowledgeTopic.READY_TO_MOVE),
        "ready to move" to setOf(KnowledgeTopic.READY_TO_MOVE),
        "when move" to setOf(KnowledgeTopic.READY_TO_MOVE),
        "care for chicks" to setOf(KnowledgeTopic.EARLY_CHICK_CARE),
        "early care" to setOf(KnowledgeTopic.EARLY_CHICK_CARE),
        "how to raise chicks" to setOf(KnowledgeTopic.EARLY_CHICK_CARE),

        // Equipment Knowledge
        "calibrate" to setOf(KnowledgeTopic.CALIBRATE_DEVICE),
        "calibration" to setOf(KnowledgeTopic.CALIBRATE_DEVICE),
        "accuracy" to setOf(KnowledgeTopic.CALIBRATE_DEVICE),
        
        "clean" to setOf(KnowledgeTopic.CLEAN_DEVICE),
        "wash" to setOf(KnowledgeTopic.CLEAN_DEVICE),
        "sanitize" to setOf(KnowledgeTopic.CLEAN_DEVICE),

        "maintenance" to setOf(KnowledgeTopic.MAINTENANCE_DUE),
        "broken" to setOf(KnowledgeTopic.MAINTENANCE_DUE),
        "repair" to setOf(KnowledgeTopic.MAINTENANCE_DUE),
        
        // Workflow
        "start incubation" to setOf(WorkflowTopic.START_INCUBATION),
        "start a new incubation" to setOf(WorkflowTopic.START_INCUBATION),
        "new incubation" to setOf(WorkflowTopic.START_INCUBATION),
        "new hatch" to setOf(WorkflowTopic.START_INCUBATION),
        
        "log expense" to setOf(WorkflowTopic.LOG_EXPENSE),
        "log cost" to setOf(WorkflowTopic.LOG_EXPENSE),
        "feed cost" to setOf(WorkflowTopic.LOG_EXPENSE),
        "log feed cost" to setOf(WorkflowTopic.LOG_EXPENSE),
        "add cost" to setOf(WorkflowTopic.LOG_EXPENSE),
        "spent" to setOf(WorkflowTopic.LOG_EXPENSE),

        "add equipment" to setOf(WorkflowTopic.ADD_EQUIPMENT),
        "record hatch" to setOf(WorkflowTopic.RECORD_HATCH),
        "record chick hatch" to setOf(WorkflowTopic.RECORD_HATCH),
        
        // Data
        "active batch" to setOf(DataTopic.ACTIVE_BATCH_STATUS),
        "how is my batch doing" to setOf(DataTopic.ACTIVE_BATCH_STATUS),
        "batch doing" to setOf(DataTopic.ACTIVE_BATCH_STATUS),
        "status" to setOf(DataTopic.ACTIVE_BATCH_STATUS),
        "how many chick" to setOf(DataTopic.LOSSES_SUMMARY),
        "losses" to setOf(DataTopic.LOSSES_SUMMARY),
        "died" to setOf(DataTopic.LOSSES_SUMMARY),
        "total spend" to setOf(DataTopic.TOTAL_SPEND),
        "total spending" to setOf(DataTopic.TOTAL_SPEND),
        "breakdown" to setOf(DataTopic.CATEGORY_BREAKDOWN),
        "how many chicks" to setOf(DataTopic.ACTIVE_CHICK_COUNT),
        "chick count" to setOf(DataTopic.ACTIVE_CHICK_COUNT),
        "total chicks" to setOf(DataTopic.ACTIVE_CHICK_COUNT),
        "how old are my chicks" to setOf(DataTopic.AGE_GROUP_SUMMARY),
        "age group" to setOf(DataTopic.AGE_GROUP_SUMMARY),
        "monthly spend" to setOf(DataTopic.MONTHLY_TREND),
        "spending trend" to setOf(DataTopic.MONTHLY_TREND),
        "this month's cost" to setOf(DataTopic.MONTHLY_TREND),
        "flock cost" to setOf(DataTopic.FLOCK_COST),
        "cost of flock" to setOf(DataTopic.FLOCK_COST),
        "sensor status" to setOf(DataTopic.SENSOR_STATUS),
        "sensor connection" to setOf(DataTopic.SENSOR_STATUS),
        "is sensor online" to setOf(DataTopic.SENSOR_STATUS),
        "active devices" to setOf(DataTopic.ACTIVE_DEVICES),
        "my connected incubators" to setOf(DataTopic.ACTIVE_DEVICES)
    )

    private val goalLexicon: Map<String, BreedingGoal> = mapOf(
        "meat" to BreedingGoal.MEAT_PRODUCTION,
        "table" to BreedingGoal.MEAT_PRODUCTION,
        "egg" to BreedingGoal.EGG_PRODUCTION,
        "layer" to BreedingGoal.EGG_PRODUCTION,
        "laying" to BreedingGoal.EGG_PRODUCTION,
        "dual purpose" to BreedingGoal.DUAL_PURPOSE,
        "both" to BreedingGoal.DUAL_PURPOSE,
        "show" to BreedingGoal.SHOW_ORNAMENTAL,
        "exhibition" to BreedingGoal.SHOW_ORNAMENTAL,
        "pet" to BreedingGoal.TEMPERAMENT,
        "friendly" to BreedingGoal.TEMPERAMENT,
        "hybrid vigor" to BreedingGoal.HEALTH_VIGOR,
        "heterosis" to BreedingGoal.HEALTH_VIGOR,
        "color egg" to BreedingGoal.PLUMAGE,
        "blue egg" to BreedingGoal.PLUMAGE,
        "green egg" to BreedingGoal.PLUMAGE
    )

    private val anchorLexicon: Map<String, String> = mapOf(
        "help" to "SYSTEM_HELP",
        "settings" to "NAV_SETTINGS",
        "profile" to "NAV_PROFILE",
        "flock" to "NAV_FLOCK",
        "inventory" to "NAV_INVENTORY",
        "what is" to "KNOWLEDGE_ANCHOR",
        "how to" to "KNOWLEDGE_ANCHOR",
        "tell me about" to "KNOWLEDGE_ANCHOR",
        "my flock" to "DATA_ANCHOR",
        "my birds" to "DATA_ANCHOR",
        "my chicks" to "DATA_ANCHOR"
    )
}

