package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.*
import com.example.hatchtracker.domain.hatchy.HatchyResponseComposer
import com.example.hatchtracker.domain.hatchy.routing.services.EvidenceMetadata
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves questions about breed characteristics and comparisons.
 */
@Singleton
class BreedKnowledgeResolver @Inject constructor(
    private val breedRepository: IBreedStandardRepository,
    responseComposer: HatchyResponseComposer
) : KnowledgeResolver(responseComposer) {

    override val capabilities: ResolverCapabilities = ResolverCapabilities(
        supportedIntents = setOf(HatchyIntent.BREED_INFO, HatchyIntent.BREED_COMPARISON),
        supportedTopics = setOf(
            KnowledgeTopic.EGG_TRAITS,
            KnowledgeTopic.TEMPERAMENT,
            KnowledgeTopic.HARDINESS,
            KnowledgeTopic.UTILITY_PURPOSE,
            KnowledgeTopic.PHYSICAL_TRAITS,
            KnowledgeTopic.HEALTH_ROBUSTNESS,
            // Ontology Topics
            KnowledgeTopic.EGG_COLOR,
            KnowledgeTopic.EGG_PRODUCTION_RATE,
            KnowledgeTopic.BROODINESS_LEVEL,
            KnowledgeTopic.TEMPERAMENT_DOCILITY,
            KnowledgeTopic.COLD_HARDINESS,
            KnowledgeTopic.HEAT_TOLERANCE,
            KnowledgeTopic.BODY_SIZE,
            KnowledgeTopic.MEAT_YIELD,
            KnowledgeTopic.PLUMAGE_COLOR,
            KnowledgeTopic.COMB_TYPE,
            KnowledgeTopic.WATER_BEHAVIOUR,
            KnowledgeTopic.GUARDING_BEHAVIOUR,
            KnowledgeTopic.EARLY_MATURITY,
            KnowledgeTopic.FLOCK_COMPATIBILITY
        ),
        optionalEntities = setOf(EntityType.BREED, EntityType.POULTRY_SPECIES, EntityType.TRAIT),
        preferredQuestionModes = setOf(QuestionMode.REAL_WORLD_GUIDANCE),
        priority = ResolverPriority.GENERAL_KNOWLEDGE,
        allowsApproximateMatch = true,
        canReturnConstrainedFallback = true
    )

    @Inject
    lateinit var traitExtractor: TraitValueExtractor

    override suspend fun resolveQuery(
        interpretation: QueryInterpretation,
        context: HatchyContextSnapshot
    ): ResolverOutcome {
        val breeds = interpretation.entities.filter { it.type == EntityType.BREED }
        val traits = interpretation.entities.filter { it.type == EntityType.TRAIT }

        val answer = when {
            breeds.size == 1 -> {
                val breed = breedRepository.getBreedById(breeds[0].value)
                if (breed != null) {
                    val evidence = EvidenceMetadata(
                        matchScore = 1.0,
                        dataSourceId = "breed_repository",
                        knowledgeKey = "breed_${breed.id}",
                        matchedTopic = "BREED_DETAILS",
                        matchedSubtype = breed.name
                    )
                    val traitInfo = when (interpretation.topicResult.primaryTopic as? KnowledgeTopic) {
                        KnowledgeTopic.EGG_TRAITS -> "It's an excellent layer, producing ~${breed.eggProductionPerYear ?: "unknown"} ${breed.eggSize ?: ""} eggs per year."
                        KnowledgeTopic.TEMPERAMENT -> "Known for being ${breed.temperament ?: "fairly standard"} in temperament."
                        KnowledgeTopic.HARDINESS -> "${if (breed.coldHardiness == com.example.hatchtracker.model.TraitLevel.HIGH) "It's exceptionally cold hardy." else "It handles climate changes well."}"
                        else -> "originally from ${breed.origin} and is known for its ${breed.eggColor} eggs."
                    }
                    HatchyAnswer(
                        text = "The ${breed.name} is a fine choice! $traitInfo ${if ((breed.weightRoosterKg ?: 0.0) > 4.0) "It's a sturdy, large breed too." else ""}",
                        type = AnswerType.BREED_INFO,
                        confidence = calibrateConfidence(
                            intentScore = interpretation.confidence,
                            entityScore = breeds[0].confidence,
                            serviceScore = 1.0,
                            weights = scoringWeights.let { Triple(it.topicMatch, it.entities, 0.4) }
                        ),
                        source = AnswerSource.BREED_REPOSITORY,
                        relatedEntities = listOf(breeds[0]),
                        debugMetadata = toDebugMetadata("SINGLE_BREED", evidence)
                    )
                } else null
            }
            breeds.size > 1 -> {
                val breedNames = breeds.joinToString(" and ") { it.originalText }
                val evidence = EvidenceMetadata(
                    matchScore = 0.8,
                    dataSourceId = "breed_repository_multi",
                    candidateCount = breeds.size,
                    matchedTopic = "BREED_COMPARISON"
                )
                HatchyAnswer(
                    text = "Comparing $breedNames? Those are both excellent choices. Let me check the specifics for you.",
                    type = AnswerType.BREED_INFO,
                    confidence = calibrateConfidence(
                        intentScore = interpretation.confidence,
                        entityScore = breeds.map { it.confidence }.average(),
                        serviceScore = 0.8
                    ),
                    source = AnswerSource.BREED_REPOSITORY,
                    relatedEntities = breeds,
                    debugMetadata = toDebugMetadata("MULTI_BREED", evidence)
                )
            }
            breeds.isEmpty() && interpretation.questionMode.primaryMode != QuestionMode.APP_WORKFLOW -> {
                val speciesEntity = interpretation.entities.find { it.type == EntityType.POULTRY_SPECIES }
                val mentionedSpecies = speciesEntity?.let { name ->
                    com.example.hatchtracker.domain.hatchy.routing.PoultrySpecies.values().find { it.name.equals(name.value, ignoreCase = true) }
                }

                val traitExtractions = traitExtractor.extract(interpretation.rawQuery, mentionedSpecies)

                if (traitExtractions.isNotEmpty()) {
                    val allBreeds = breedRepository.getAllBreeds()
                    val rankedBreeds = allBreeds.map { breed ->
                        val score = calculateMatchScore(breed, traitExtractions, mentionedSpecies)
                        breed to score
                    }.filter { it.second > 0.0 }
                     .sortedByDescending { it.second }
                     .take(5)

                    if (rankedBreeds.isNotEmpty()) {
                        val breedNames = rankedBreeds.joinToString(", ") { it.first.name }
                        val topBreedScore = rankedBreeds.first().second

                        val text = if (topBreedScore >= 1.0) {
                            "Based on your request, I recommend looking at these breeds: $breedNames. They are known for ${traitExtractions.joinToString(" and ") { it.matchedPhrase }}."
                        } else {
                            "I found some breeds that might match your interest in ${traitExtractions.getOrNull(0)?.matchedPhrase}: $breedNames."
                        }

                        val evidence = EvidenceMetadata(
                            matchScore = topBreedScore,
                            matchedTopic = traitExtractions.first().topic.name,
                            dataSourceId = "breed_trait_ontology_map"
                        )

                        HatchyAnswer(
                            text = text,
                            type = AnswerType.BREED_INFO,
                            confidence = calibrateConfidence(
                                intentScore = interpretation.confidence,
                                entityScore = topBreedScore,
                                serviceScore = 1.0
                            ),
                            source = AnswerSource.BREED_REPOSITORY,
                            relatedEntities = rankedBreeds.map { HatchyEntity(EntityType.BREED, it.first.id, it.first.name) },
                            debugMetadata = toDebugMetadata("TRAIT_ONTOLOGY_SEARCH", evidence)
                        )
                    } else {
                        null
                    }
                } else {
                    // Existing legacy trait fallback
                    val traits = interpretation.entities.filter { it.type == EntityType.TRAIT }
                    if (traits.isNotEmpty() || (interpretation.topicResult.primaryTopic as? KnowledgeTopic) == KnowledgeTopic.TRAIT_INHERITANCE) {
                        val matchedTopic = interpretation.topicResult.primaryTopic as? KnowledgeTopic

                        val resultText = when (matchedTopic) {
                            KnowledgeTopic.EGG_TRAITS -> "If you're looking for high egg production, breeds like the White Leghorn, ISA Brown, and Rhode Island Red are top choices."
                            KnowledgeTopic.TEMPERAMENT -> "For friendly, docile birds, check out the Buff Orpington, Silkies, or Australorps."
                            KnowledgeTopic.HARDINESS -> "For cold climates, Chanteclers and Buckeyes are exceptionally hardy."
                            else -> "If you're looking for the best for ${traits.getOrNull(0)?.originalText ?: "this trait"}, I'd recommend checking out established favorites."
                        }

                        val evidence = EvidenceMetadata(
                            matchScore = 0.8,
                            matchedTopic = matchedTopic?.name ?: traits.getOrNull(0)?.value ?: "trait",
                            dataSourceId = "breed_trait_map"
                        )
                        HatchyAnswer(
                            text = resultText,
                            type = AnswerType.BREED_INFO,
                            confidence = calibrateConfidence(
                                intentScore = interpretation.confidence,
                                entityScore = traits.getOrNull(0)?.confidence ?: 0.7,
                                serviceScore = 0.8
                            ),
                            source = AnswerSource.BREED_REPOSITORY,
                            relatedEntities = traits,
                            debugMetadata = toDebugMetadata("TRAIT_SEARCH", evidence)
                        )
                    } else null
                }
            }
            else -> null
        }

        return if (answer != null) resolveTo(answer) else ResolverOutcome.InsufficientEvidence()
    }

    private fun calculateMatchScore(breed: BreedStandard, extractions: List<TraitExtraction>, mentionedSpecies: com.example.hatchtracker.domain.hatchy.routing.PoultrySpecies?): Double {
        if (extractions.isEmpty()) return 0.0

        var score = 0.0
        extractions.forEach { extraction ->
            if (breedMatchesTrait(breed, extraction.constraint)) {
                score += if (extraction.isExactMatch) 1.0 else 0.6
            } else if (isPartialMatch(breed, extraction.constraint)) {
                score += 0.4
            }
        }

        val matchDegree = score / extractions.size
        if (matchDegree == 0.0) return 0.0

        // Species bonus
        val speciesBonus = if (mentionedSpecies != null && breed.species.equals(mentionedSpecies.name, ignoreCase = true)) 1.2 else 1.0

        return (matchDegree * speciesBonus).coerceAtMost(1.0)
    }

    private fun breedMatchesTrait(breed: BreedStandard, constraint: TraitConstraint): Boolean {
        val value = constraint.value
        return when (constraint.dimension) {
            TraitDimension.EGG_COLOR -> breed.normalizedEggColor == value
            TraitDimension.TEMPERAMENT_DOCILITY -> breed.normalizedTemperament == value
            TraitDimension.COLD_HARDINESS -> breed.coldHardiness == TraitLevel.HIGH && value == HardinessLevel.COLD_HARDY
            TraitDimension.HEAT_TOLERANCE -> breed.heatTolerance == TraitLevel.HIGH && value == HardinessLevel.HEAT_TOLERANT
            TraitDimension.BODY_SIZE -> breed.normalizedBodySize == value
            TraitDimension.MEAT_YIELD -> breed.normalizedPrimaryUsage.contains(value)
            TraitDimension.BROODINESS_LEVEL -> breed.broodinessLevel == TraitLevel.HIGH && value == BroodinessLevel.FREQUENT
            TraitDimension.COMB_TYPE -> breed.normalizedCombType == value
            else -> false
        }
    }

    private fun isPartialMatch(breed: BreedStandard, constraint: TraitConstraint): Boolean {
        // Semantic fallback logic (e.g. calm vs docile handled by Lexicon usually,
        // but this adds extra depth for string fields)
        return false
    }
}
