package com.example.hatchtracker.domain.hatchy.routing.services

import com.example.hatchtracker.domain.hatchy.routing.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service providing guidance on chick nursery and brooder care.
 * Consumes structured facts from PoultryCoreKnowledge.
 */
@Singleton
class NurseryKnowledgeService @Inject constructor(
    private val tempFormatter: TemperatureFormatter
) : IKnowledgeService {

    override suspend fun findMatch(
        query: String,
        species: PoultrySpecies?,
        topic: String?,
        context: HatchyContextSnapshot
    ): KnowledgeMatchResult? {
        val targetSpecies = species ?: PoultrySpecies.CHICKEN
        val matchedTopic = topic ?: detectTopic(query) ?: return null

        val content = when (matchedTopic) {
            "TEMPERATURE" -> {
                val week1F = PoultryCoreKnowledge.BrooderTempsF[1] ?: 95.0
                val tempStr = tempFormatter.format(week1F)
                "For ${targetSpecies.name.lowercase()}s, start the brooder at $tempStr. Reduce the temperature by 5°F (approx 3°C) each week as they grow."
            }
            "FEEDING" -> "Start your ${targetSpecies.name.lowercase()}s on high-protein chick crumbles (18-20%). Ensure they have clean, dip-able water at all times."
            "COOP_TRANSITION" -> "Chicks are generally ready to move to the coop around 6-8 weeks, once they are fully feathered and the outside lows are manageable."
            "EARLY_CARE" -> "Early care is about the 'Big Three': Heat, Hydration, and Hygiene. Keep the brooder dry and draft-free."
            else -> return null
        }

        return KnowledgeMatchResult(
            content = content,
            confidence = 0.9,
            source = AnswerSource.POULTRY_KNOWLEDGE_BASE,
            evidence = EvidenceMetadata(
                matchScore = 0.9,
                matchedTopic = "NURSERY_GUIDANCE",
                matchedSubtype = matchedTopic,
                matchedSpecies = targetSpecies
            )
        )
    }

    private fun detectTopic(query: String): String? {
        val q = query.lowercase()
        return when {
            q.contains("temp") || q.contains("heat") || q.contains("warm") -> "TEMPERATURE"
            q.contains("feed") || q.contains("eat") || q.contains("diet") -> "FEEDING"
            q.contains("coop") || q.contains("outdoor") || q.contains("move") -> "COOP_TRANSITION"
            q.contains("care") || q.contains("raise") || q.contains("start") -> "EARLY_CARE"
            else -> null
        }
    }
}
