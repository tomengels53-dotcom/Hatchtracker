package com.example.hatchtracker.domain.hatchy.routing.services

import com.example.hatchtracker.domain.hatchy.routing.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service that provides species-specific incubation knowledge using structured datasets.
 * Implements indexing and caching for performance.
 */
@Singleton
class IncubationKnowledgeService @Inject constructor(
    private val tempFormatter: TemperatureFormatter
) : IKnowledgeService {

    override suspend fun findMatch(
        query: String,
        species: PoultrySpecies?,
        topic: String?,
        context: HatchyContextSnapshot
    ): KnowledgeMatchResult? {
        val targetSpecies = species ?: PoultrySpecies.CHICKEN // Default to chicken if unknown
        val record = PoultryCoreKnowledge.IncubationPeriods[targetSpecies] ?: return null

        // Topic must be provided by the resolver. Service no longer performs its own detection.
        val matchedTopicKey = topic ?: return null
        
        val content = when (matchedTopicKey) {
            "DURATION", "INCUBATION_PERIOD", "HATCH_TIMING" -> "${targetSpecies.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} eggs typically take ${record.totalDays} days to hatch. Ensure your incubator is stable throughout this period."
            "HUMIDITY" -> "For ${targetSpecies.name.lowercase()}s, keep humidity at ${record.genericHumidity}% for the first ${record.lockdownDay} days, then increase to ${record.lockdownHumidity}% for lockdown (days ${record.lockdownDay}-${record.totalDays})."
            "TEMPERATURE" -> {
                val tempStr = tempFormatter.format(record.idealTempF)
                "Maintain a steady temperature of $tempStr in your incubator for ${targetSpecies.name.lowercase()}s."
            }
            "LOCKDOWN" -> "Lockdown for ${targetSpecies.name.lowercase()}s starts on day ${record.lockdownDay}. Stop turning the eggs and increase humidity."
            else -> return null
        }

        return KnowledgeMatchResult(
            content = content,
            confidence = 0.9,
            source = AnswerSource.POULTRY_KNOWLEDGE_BASE,
            evidence = EvidenceMetadata(
                matchScore = 0.9,
                matchedTopic = "INCUBATION_KNOWLEDGE",
                matchedSubtype = matchedTopicKey,
                matchedSpecies = targetSpecies,
                knowledgeKey = "incubation_${targetSpecies.name.lowercase()}_$matchedTopicKey"
            )
        )
    }
}
