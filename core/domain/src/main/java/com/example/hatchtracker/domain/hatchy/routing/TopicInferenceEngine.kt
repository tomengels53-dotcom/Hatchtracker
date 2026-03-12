package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.KnowledgeTopic
import com.example.hatchtracker.model.DataTopic
import com.example.hatchtracker.model.WorkflowTopic
import com.example.hatchtracker.model.HatchyTopic


import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TopicInferenceEngine @Inject constructor(
    private val lexiconRegistry: ILexiconRegistry
) {
    /**
     * Infer semantic topics by combining multiple signals:
     * 1. Lexical Topic Matches (keywords)
     * 2. Extracted Entities (contextual boost)
     * 3. Question Mode (intentional bias)
     */
    fun inferTopics(
        query: String,
        entities: List<HatchyEntity>,
        questionMode: QuestionModeResult
    ): TopicInferenceResult {
        // 1. Lexical Signal
        val rawLexicalScores = lexiconRegistry.matchTopics(query)
        val finalScores = rawLexicalScores.toMutableMap()
        
        // 2. Contextual Signal (Entity Boost)
        entities.forEach { entity ->
            when (entity.type) {
                EntityType.INCUBATION_TOPIC -> boostTopics(finalScores, KnowledgeTopic::class.java, 0.2)
                EntityType.NURSERY_GUIDANCE_TOPIC, EntityType.NURSERY_STATUS_TOPIC -> boostTopics(finalScores, KnowledgeTopic::class.java, 0.2)
                EntityType.BREEDING_TOPIC, EntityType.TRAIT -> boostTopics(finalScores, KnowledgeTopic::class.java, 0.2)
                EntityType.FINANCE_SUMMARY_TOPIC, EntityType.FINANCE_PERIOD -> boostTopics(finalScores, DataTopic::class.java, 0.2)
                EntityType.ACTION -> boostTopics(finalScores, WorkflowTopic::class.java, 0.2)
                else -> {}
            }
        }
        
        // 3. Intentional Signal (Question Mode Bias)
        when (questionMode.primaryMode) {
            QuestionMode.USER_DATA_STATUS -> boostTopics(finalScores, DataTopic::class.java, 0.3)
            QuestionMode.APP_WORKFLOW -> boostTopics(finalScores, WorkflowTopic::class.java, 0.3)
            QuestionMode.REAL_WORLD_GUIDANCE -> boostTopics(finalScores, KnowledgeTopic::class.java, 0.3)
            else -> {}
        }
        
        // Final normalization and selection
        val sortedTopics = finalScores.toList()
            .sortedWith(compareByDescending { pair -> pair.component2() })
            
        val primaryTopic = sortedTopics.firstOrNull()?.component1()
        val secondaryTopic = sortedTopics.getOrNull(1)?.component1()
        val rawConfidence = sortedTopics.firstOrNull()?.component2() ?: 0.0
        val confidence = if (rawConfidence > 1.0) 1.0 else rawConfidence
        
        return TopicInferenceResult(
            primaryTopic = primaryTopic,
            secondaryTopic = secondaryTopic,
            topicScores = finalScores,
            confidence = confidence
        )
    }

    private fun <T : HatchyTopic> boostTopics(
        scores: MutableMap<HatchyTopic, Double>,
        topicClass: Class<T>,
        amount: Double
    ) {
        scores.keys.forEach { topic ->
            if (topicClass.isInstance(topic)) {
                scores[topic] = (scores[topic] ?: 0.0) + amount
            }
        }
    }
}
