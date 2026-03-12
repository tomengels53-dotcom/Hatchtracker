package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.domain.repo.FlockRepository
import com.example.hatchtracker.model.BasicRecommendation
import com.example.hatchtracker.model.Bird
import com.example.hatchtracker.model.BreedStandard
import com.example.hatchtracker.model.BreedingGoal
import com.example.hatchtracker.model.BreedingSafeguard
import com.example.hatchtracker.model.Flock
import com.example.hatchtracker.model.Incubation
import com.example.hatchtracker.model.StrategicRecommendation
import com.example.hatchtracker.model.UiText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

data class BreedingSelectionData(
    val flocks: List<Flock>,
    val birdsByFlock: Map<Long, List<Bird>>,
    val incubations: List<Incubation>
)

data class BreedingPairAdvice(
    val basicRecommendation: BasicRecommendation?,
    val strategicRecommendation: StrategicRecommendation?,
    val safeguard: BreedingSafeguard,
    val excessiveMaleReuse: Boolean,
    val lowDiversityDetected: Boolean,
    val hatchyAdvice: UiText?
)

@Singleton
class BreedingFacade @Inject constructor(
    private val flockRepository: FlockRepository,
    private val birdRepository: BirdRepository
) {
    fun getSelectionData(): Flow<BreedingSelectionData> = combine(
        flockRepository.allActiveFlocks,
        birdRepository.allBirds
    ) { flocks, birds ->
        val grouped = birds.filter { it.flockId != null }
            .groupBy { it.flockId!! }

        BreedingSelectionData(flocks, grouped, emptyList())
    }

    fun evaluatePair(
        male: Bird?,
        females: List<Bird>,
        incubations: List<Incubation>,
        selectedGoals: Set<String>,
        isPRO: Boolean,
        birdsMap: Map<Long, Bird>
    ): BreedingPairAdvice {
        val excessiveReuse = if (male != null) {
            incubations.count { it.fatherBirdId == male.localId } >= 5
        } else false

        val maleTraits = male?.geneticProfile?.fixedTraits?.toSet() ?: emptySet()
        val lowDiversity = if (maleTraits.isNotEmpty() && females.isNotEmpty()) {
            females.map { female ->
                val femaleTraits = female.geneticProfile.fixedTraits.toSet()
                maleTraits.intersect(femaleTraits).size.toFloat() / maleTraits.size.toFloat()
            }.any { it > 0.75f }
        } else false

        val basicRec = if (male != null && females.isNotEmpty()) {
            BasicRecommendation(
                male = male,
                female = females.first(),
                score = if (male.species == females.first().species) 75 else 0,
                basicSummary = UiText.DynamicString("Compatibility estimate based on current selected pair.")
            )
        } else null

        val strategicRec = if (male != null && females.isNotEmpty() && isPRO) {
            val goals = selectedGoals.mapNotNull { goal ->
                runCatching { com.example.hatchtracker.model.BreedingGoalType.valueOf(goal) }
                    .getOrNull()
                    ?.let { BreedingGoal(it, priority = 3) }
            }
            StrategicRecommendation(
                male = male,
                female = females.first(),
                score = 70,
                strategicRationale = UiText.DynamicString("Strategic recommendation compatibility mode."),
                goalMatches = goals.map { it.type },
                traitAnalysis = UiText.DynamicString("Use breeding history and logged outcomes to refine confidence."),
                riskFactors = emptyList(),
                expectedOutcomes = emptyList()
            )
        } else null

        val safeguard = BreedingSafeguardManager.evaluatePair(
            male, 
            females,
            birdsMap
        )

        val hatchyAdvice = if (male != null && selectedGoals.isNotEmpty()) {
            val goalsText = selectedGoals
                .map { it.lowercase().replace('_', ' ') }
                .joinToString()
            if (isPRO) {
                UiText.DynamicString("Focus pair: ${male.species.name} for goals: $goalsText.")
            } else {
                UiText.DynamicString("Upgrade to unlock strategic scenario advice.")
            }
        } else null

        return BreedingPairAdvice(
            basicRecommendation = basicRec,
            strategicRecommendation = strategicRec,
            safeguard = safeguard,
            excessiveMaleReuse = excessiveReuse,
            lowDiversityDetected = lowDiversity,
            hatchyAdvice = hatchyAdvice
        )
    }

    suspend fun confirmBreeding(
        male: Bird,
        females: List<Bird>,
        goals: Set<String>
    ): Long {
        return System.currentTimeMillis()
    }

    fun getBreedById(id: String): BreedStandard? = null
    fun getBreedsForSpecies(species: String): List<BreedStandard> = emptyList()
}
