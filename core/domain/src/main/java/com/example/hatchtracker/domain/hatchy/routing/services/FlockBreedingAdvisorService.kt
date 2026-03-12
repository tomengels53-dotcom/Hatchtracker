package com.example.hatchtracker.domain.hatchy.routing.services

import com.example.hatchtracker.domain.hatchy.routing.*
import com.example.hatchtracker.data.repository.BreedStandardRepository
import com.example.hatchtracker.data.repository.BreedingAdvisor
import com.example.hatchtracker.model.Species
import com.example.hatchtracker.model.TraitLevel
import com.example.hatchtracker.model.PrimaryUsage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service providing recommendations for the user's current flocks.
 * Eliminates hardcoded breed advice in favor of repository-backed insights.
 */
@Singleton
class FlockBreedingAdvisorService @Inject constructor(
    private val breedRepository: BreedStandardRepository,
    private val breedingAdvisor: BreedingAdvisor
) {
    fun getRecommendation(
        goal: BreedingGoal?,
        species: PoultrySpecies?,
        context: HatchyContextSnapshot
    ): RecommendationResult {
        val targetSpecies = when (species) {
            PoultrySpecies.CHICKEN -> Species.CHICKEN
            PoultrySpecies.DUCK -> Species.DUCK
            PoultrySpecies.QUAIL -> Species.QUAIL
            else -> Species.CHICKEN
        }

        val allBreeds = breedRepository.getBreedsBySpecies(targetSpecies.name)
        
        val summary = when (goal) {
            BreedingGoal.EGG_PRODUCTION -> {
                val topLayers = allBreeds
                    .filter { it.normalizedPrimaryUsage.contains(PrimaryUsage.LAYER) || (it.eggProductionPerYear ?: 0) > 200 }
                    .sortedByDescending { it.eggProductionPerYear ?: 0 }
                    .take(3)
                    .joinToString(", ") { it.name }
                
                if (topLayers.isNotEmpty()) {
                    "For maximizing egg production, I'd suggest focusing on your high-producing lines like $topLayers."
                } else {
                    "For egg production, look for established layer breeds with high annual yields in the repository."
                }
            }
            BreedingGoal.TEMPERAMENT -> {
                val docileBreeds = allBreeds
                    .filter { it.humanFriendliness == TraitLevel.HIGH }
                    .take(3)
                    .joinToString(", ") { it.name }
                
                if (docileBreeds.isNotEmpty()) {
                    "To improve temperament, prioritize your most docile breeds such as $docileBreeds."
                } else {
                    "Focus on selecting birds with proven calm temperaments and high docility ratings."
                }
            }
            else -> "To improve your flock's quality, focus on selecting birds that best match the breed standard for your target traits."
        }

        return RecommendationResult(
            candidates = emptyList(),
            reasoning = summary,
            confidence = 0.9,
            source = AnswerSource.BREEDING_ENGINE,
            evidence = EvidenceMetadata(
                matchScore = 0.9,
                matchedTopic = "FLOCK_RECOMMENDATION",
                matchedSubtype = goal?.toString() ?: "GENERAL"
            )
        )
    }
}
