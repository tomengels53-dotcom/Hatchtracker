package com.example.hatchtracker.domain.hatchy.routing.services

import com.example.hatchtracker.domain.hatchy.routing.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service that provides general poultry husbandry, health, and care knowledge.
 */
@Singleton
class PoultryKnowledgeService @Inject constructor(
    private val tempFormatter: TemperatureFormatter
) : IKnowledgeService {

    override suspend fun findMatch(
        query: String,
        species: PoultrySpecies?,
        topic: String?,
        context: HatchyContextSnapshot
    ): KnowledgeMatchResult? {
        val q = query.lowercase()
        
        // 1. Try to match specific structured health facts
        val healthMatch = PoultryCoreKnowledge.HealthBasics.find { fact ->
            q.contains(fact.symptom.lowercase()) || fact.causes.any { q.contains(it.lowercase()) }
        }

        if (healthMatch != null) {
            return KnowledgeMatchResult(
                content = "${healthMatch.symptom}: Possible causes include ${healthMatch.causes.joinToString(", ")}. Recommended action: ${healthMatch.action}",
                confidence = 0.95,
                source = AnswerSource.POULTRY_KNOWLEDGE_BASE,
                evidence = EvidenceMetadata(
                    matchScore = 0.95,
                    matchedTopic = "POULTRY_HEALTH",
                    matchedSubtype = healthMatch.symptom,
                    matchedSpecies = species ?: PoultrySpecies.CHICKEN
                )
            )
        }

        // 2. General fallbacks for common topics
        val content = when {
            q.contains("not laying") || q.contains("stop laying") -> 
                "A drop in laying can happen for several reasons: season (shorter days), age (over 2 years), stress, nutrition, or molt."
            q.contains("nutrition") || q.contains("feed") -> 
                "Proper nutrition is key. Use starter for chicks, grower for teenagers, and layer pellets (16% protein) for active hens."
            else -> return null
        }

        return KnowledgeMatchResult(
            content = content,
            confidence = 0.85,
            source = AnswerSource.POULTRY_KNOWLEDGE_BASE,
            evidence = EvidenceMetadata(
                matchScore = 0.85,
                matchedTopic = "GENERAL_POULTRY",
                matchedSpecies = species ?: PoultrySpecies.CHICKEN
            )
        )
    }
}

