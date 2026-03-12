package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.Bird
import com.example.hatchtracker.data.models.ConfidenceLevel
import com.example.hatchtracker.model.Bird as DomainBird
import com.example.hatchtracker.model.Sex as DomainSex
import com.example.hatchtracker.model.Species
import com.example.hatchtracker.model.breeding.InheritedFrom
import com.example.hatchtracker.model.breeding.PredictionTier
import com.example.hatchtracker.model.breeding.TraitPrediction
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of a mating recommendation calculation.
 */
data class MatingRecommendation(
    val sire: Bird,
    val dam: Bird,
    val overallScore: Double, // 0 - 1.0
    val traitProbability: Double,
    val diversityScore: Double,
    val confidenceScore: Double,
    val explanation: String,
    val traitPrediction: TraitPrediction?
)

/**
 * Mating Advisor Service
 * 
 * Suggests optimal breeding pairs based on the canonical breeding prediction pipeline.
 */
@Singleton
class MatingAdvisor @Inject constructor(
    private val breedingPredictionService: BreedingPredictionService
) {

    /**
     * Recommends top N mating pairs for a target trait.
     */
    fun recommendPairs(
        targetTraitId: String,
        flock: List<Bird>,
        avoidInbreeding: Boolean = true,
        maxRelatednessScore: Double = 0.5,
        preferredConfidence: ConfidenceLevel = ConfidenceLevel.MEDIUM,
        limit: Int = 5
    ): List<MatingRecommendation> {
        val sires = flock.filter { it.sex == com.example.hatchtracker.data.models.Sex.MALE }
        val dams = flock.filter { it.sex == com.example.hatchtracker.data.models.Sex.FEMALE }
        val domainFlock = flock.map { it.toDomain() }
        val domainBirdById = domainFlock.associateBy { it.localId }
        
        val recommendations = mutableListOf<MatingRecommendation>()

        for (sire in sires) {
            for (dam in dams) {
                // 1. Inbreeding Filter
                val sireDomain = domainBirdById[sire.localId] ?: sire.toDomain()
                val damDomain = domainBirdById[dam.localId] ?: dam.toDomain()
                val risk = GeneticRiskAnalyzer.analyzeBreedingRisk(sireDomain, damDomain, domainFlock)
                if (avoidInbreeding && risk.riskLevel == RiskLevel.HIGH_RISK) continue
                
                // 2. Trait Prediction via Canonical Service
                val species = sire.species

                val predictionResult = breedingPredictionService.predictBreeding(
                    species = species,
                    sireProfile = sire.geneticProfile,
                    damProfile = dam.geneticProfile,
                    sireBreedId = sire.breedId,
                    damBreedId = dam.breedId
                )

                // Map PhenotypeResult entries to TraitPrediction (Legacy bridge)
                val targetTraitValue = predictionResult.phenotypeResult.probabilities
                    .firstOrNull { it.phenotypeId == targetTraitId }
                val traitProb = targetTraitValue?.probability ?: 0.0
                
                // 3. Scoring Calculation
                // Diversity Score: Derived from bottleneck risk (1.0 - score/100)
                val diversityScore = 1.0 - (risk.bottleneckRiskScore.toDouble() / 100.0)

                // Confidence Score: Standardized for deterministic engine
                val confidenceScore = if (targetTraitValue != null) 0.9 else 0.0 // Deterministic is usually high confidence

                // Overall Weighted Score: 40% Trait, 30% Diversity, 30% Confidence
                val overallScore = (traitProb * 0.4) + (diversityScore * 0.3) + (confidenceScore * 0.3)

                // 4. Generate Explanation
                val explanation = buildString {
                    append("Mating Score: ${(overallScore * 100).toInt()}/100. ")
                    if (traitProb > 0.5) append("Strong inheritance for '$targetTraitId'. ")
                    append(risk.recommendation)
                }

                recommendations.add(MatingRecommendation(
                    sire = sire,
                    dam = dam,
                    overallScore = overallScore,
                    traitProbability = traitProb,
                    diversityScore = diversityScore,
                    confidenceScore = confidenceScore,
                    explanation = explanation,
                    traitPrediction = targetTraitValue?.let { 
                        TraitPrediction(
                            traitId = targetTraitId,
                            trait = targetTraitId,
                            inheritedFrom = InheritedFrom.INFERRED,
                            probability = it.probability,
                            tier = if (it.probability >= 0.8) PredictionTier.HIGH else PredictionTier.MEDIUM,
                            confidence = ConfidenceLevel.HIGH,
                            explanation = "Derived from deterministic breeding prediction."
                        )
                    }
                ))
            }
        }

        return recommendations
            .filter { it.overallScore > 0 }
            .sortedByDescending { it.overallScore }
            .take(limit)
    }

    private fun Bird.toDomain(): DomainBird = DomainBird(
        localId = localId,
        syncId = syncId,
        flockId = flockId,
        species = species,
        breed = breed,
        breedId = breedId,
        sex = when (sex) {
            com.example.hatchtracker.data.models.Sex.MALE -> DomainSex.MALE
            com.example.hatchtracker.data.models.Sex.FEMALE -> DomainSex.FEMALE
            else -> DomainSex.UNKNOWN
        },
        hatchDate = hatchDate,
        generation = generation,
        motherId = motherId,
        fatherId = fatherId,
        incubationId = incubationId,
        hatchBatchId = hatchBatchId,
        color = color,
        notes = notes,
        status = status,
        lastUpdated = lastUpdated,
        imagePath = imagePath,
        lifecycleStage = lifecycleStage,
        geneticProfile = geneticProfile,
        cloudId = cloudId,
        ownerUserId = ownerUserId,
        serverUpdatedAt = serverUpdatedAt,
        localUpdatedAt = localUpdatedAt,
        deleted = deleted,
        syncState = syncState,
        costBasisCents = costBasisCents,
        costBasisSourceRef = costBasisSourceRef
    )
}

