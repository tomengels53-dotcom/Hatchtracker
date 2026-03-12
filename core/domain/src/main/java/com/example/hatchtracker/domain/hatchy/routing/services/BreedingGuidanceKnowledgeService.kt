package com.example.hatchtracker.domain.hatchy.routing.services

import com.example.hatchtracker.domain.hatchy.routing.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service providing general breeding knowledge and guidance.
 */
@Singleton
class BreedingGuidanceKnowledgeService @Inject constructor() : IKnowledgeService {

    override suspend fun findMatch(
        query: String,
        species: PoultrySpecies?,
        topic: String?,
        context: HatchyContextSnapshot
    ): KnowledgeMatchResult? {
        val matchedTopic = topic ?: detectTopic(query) ?: return null
        
        val content = when (matchedTopic) {
            "LINE_BREEDING" -> "Line breeding involves breeding related individuals to fix certain traits. It requires careful selection to avoid inbreeding depression."
            "SELECTION" -> "Select for health, vigor, and breed-specific traits like egg size or meat quality. Always cull birds with physical defects."
            "GENETICS" -> {
                val fact = PoultryCoreKnowledge.GeneticsFacts["Sex-Linked"]
                "Basic genetics: $fact"
            }
            "GENERATION_VARIATION" -> {
                val f2Fact = PoultryCoreKnowledge.GeneticsFacts["F2"]
                "When breeding hybrids (like F1s), expect high variation in the F2 generation. $f2Fact"
            }
            else -> return null
        }

        return KnowledgeMatchResult(
            content = content,
            confidence = 1.0,
            source = AnswerSource.POULTRY_KNOWLEDGE_BASE,
            evidence = EvidenceMetadata(
                matchScore = 1.0,
                matchedTopic = "BREEDING_GUIDANCE",
                matchedSubtype = matchedTopic
            )
        )
    }

    private fun detectTopic(query: String): String? {
        val q = query.lowercase()
        return when {
            q.contains("line") || q.contains("inbreed") -> "LINE_BREEDING"
            q.contains("select") || q.contains("cull") -> "SELECTION"
            q.contains("sex link") || q.contains("gene") || q.contains("genetic") -> "GENETICS"
            q.contains("f2") || q.contains("generation") || q.contains("variation") -> "GENERATION_VARIATION"
            else -> null
        }
    }
}

