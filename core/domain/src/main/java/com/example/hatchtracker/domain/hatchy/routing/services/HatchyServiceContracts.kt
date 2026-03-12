package com.example.hatchtracker.domain.hatchy.routing.services

import com.example.hatchtracker.domain.hatchy.routing.*

/**
 * Standardized evidence and debug metadata for all service outcomes.
 */
data class EvidenceMetadata(
    val matchScore: Double = 0.0,
    val matchedTopic: String? = null,
    val matchedSubtype: String? = null,
    val matchedSpecies: PoultrySpecies? = null,
    val recordCount: Int? = null,
    val candidateCount: Int? = null,
    val dataSourceId: String? = null,
    val knowledgeKey: String? = null,
    val sourcePath: String? = null,
    val customMetadata: Map<String, Any> = emptyMap()
)

/**
 * Structured result from a knowledge service.
 */
data class KnowledgeMatchResult(
    val content: String,
    val confidence: Double,
    val source: AnswerSource,
    val evidence: EvidenceMetadata
)

/**
 * Structured result from a query service.
 */
data class QueryResolutionResult(
    val data: Map<String, Any>,
    val summary: String,
    val confidence: Double,
    val source: AnswerSource,
    val evidence: EvidenceMetadata
)

/**
 * Structured result from a recommendation service.
 */
data class RecommendationResult(
    val candidates: List<Any>,
    val reasoning: String,
    val confidence: Double,
    val source: AnswerSource,
    val evidence: EvidenceMetadata
)

/**
 * Contract for standardized knowledge lookup.
 * Implementations will use versioned JSON datasets.
 */
interface IKnowledgeService {
    suspend fun findMatch(
        query: String,
        species: PoultrySpecies?,
        topic: String?,
        context: HatchyContextSnapshot
    ): KnowledgeMatchResult?
}
