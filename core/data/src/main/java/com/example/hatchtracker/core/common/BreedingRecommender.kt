package com.example.hatchtracker.core.common

import com.example.hatchtracker.data.models.Bird
import com.example.hatchtracker.data.models.Incubation
import com.example.hatchtracker.data.models.Sex
import com.example.hatchtracker.data.models.BreedingGoal
import com.example.hatchtracker.data.models.BreedingGoalType

data class Recommendation(
    val male: Bird,
    val female: Bird,
    val score: Int,
    val rationale: String,
    val goalMatches: List<BreedingGoalType> = emptyList()
)

object BreedingRecommender {

    fun getRecommendations(
        birds: List<Bird>,
        incubations: List<Incubation>,
        goals: List<BreedingGoal> = emptyList()
    ): List<Recommendation> {
        val completed = incubations.filter { it.hatchCompleted }
        val females = birds.filter { it.sex == Sex.FEMALE && it.status == "active" }
        val males = birds.filter { it.sex == Sex.MALE && it.status == "active" }

        val recommendations = mutableListOf<Recommendation>()

        females.groupBy { it.species }.forEach { (species, speciesFemales) ->
            val speciesMales = males.filter { it.species == species }
            if (speciesMales.isEmpty()) return@forEach

            speciesFemales.forEach { female ->
                speciesMales.forEach { male ->
                    val result = getPairRecommendation(male, female, completed, goals)
                    if (result.score > 0) {
                        recommendations.add(result)
                    }
                }
            }
        }

        return recommendations.sortedByDescending { it.score }.take(10)
    }

    fun getPairRecommendation(
        male: Bird,
        female: Bird,
        incubations: List<Incubation>,
        goals: List<BreedingGoal>
    ): Recommendation {
        var score = 50 // Base score
        val matchReasons = mutableListOf<String>()
        val matchedGoals = mutableListOf<BreedingGoalType>()

        // 1. Basic Fertility & Hatch Success (Historical data)
        val femaleIncubations = incubations.filter { it.birdId == female.localId }
        val maleIncubations = incubations.filter { it.fatherBirdId == male.localId }

        if (femaleIncubations.isNotEmpty()) {
            val totalEggs = femaleIncubations.sumOf { it.eggsCount }
            if (totalEggs > 0) {
                val rate = femaleIncubations.sumOf { it.hatchedCount }.toFloat() / totalEggs
                if (rate > 0.7f) {
                    score += 15
                    matchReasons.add("Proven high hatch rate hen (${(rate * 100).toInt()}%).")
                }
            }
        }

        if (maleIncubations.isNotEmpty()) {
            val totalEggs = maleIncubations.sumOf { it.eggsCount }
            if (totalEggs > 0) {
                val fertility = 1f - (maleIncubations.sumOf { it.infertileCount }.toFloat() / totalEggs)
                if (fertility > 0.8f) {
                    score += 15
                    matchReasons.add("High fertility sire (${(fertility * 100).toInt()}%).")
                }
            }
        }

        // 2. Goal-Based Scoring & Explanations
        goals.forEach { goal ->
            when (goal.type) {
                BreedingGoalType.EGG_COLOR -> {
                    val femaleTrait = female.geneticProfile.fixedTraits.find { it.contains("Egg", ignoreCase = true) }
                    val maleTrait = male.geneticProfile.inferredTraits.find { it.contains("Egg", ignoreCase = true) }
                    if (femaleTrait != null || maleTrait != null) {
                        score += 20 * goal.priority
                        matchedGoals.add(goal.type)
                        matchReasons.add("Aligns with Egg Color goal.")
                    }
                }
                BreedingGoalType.SIZE -> {
                    val maleSize = male.geneticProfile.fixedTraits.find { it.contains("Size", ignoreCase = true) }
                    val femaleSize = female.geneticProfile.fixedTraits.find { it.contains("Size", ignoreCase = true) }
                    if (maleSize != null || femaleSize != null) {
                        score += 15 * goal.priority
                        matchedGoals.add(goal.type)
                        matchReasons.add("Focuses on target bird size.")
                    }
                }
                BreedingGoalType.TEMPERAMENT -> {
                    val maleTemp = male.geneticProfile.fixedTraits.find { it.contains("Calm", ignoreCase = true) }
                    val femaleTemp = female.geneticProfile.fixedTraits.find { it.contains("Calm", ignoreCase = true) }
                    if (maleTemp != null && femaleTemp != null) {
                        score += 25 * goal.priority
                        matchedGoals.add(goal.type)
                        matchReasons.add("Both parents show calm temperament, likely to improve offspring stability.")
                    } else if (maleTemp != null || femaleTemp != null) {
                        score += 10 * goal.priority
                        matchReasons.add("One calm parent helps mitigate temperament risks.")
                    }
                }
                BreedingGoalType.GENETIC_DIVERSITY -> {
                    if (male.breed != female.breed) {
                        score += 25 * goal.priority
                        matchedGoals.add(goal.type)
                        matchReasons.add("Encourages diversity through cross-breed pairing.")
                    } else {
                        val overlap = male.geneticProfile.fixedTraits.toSet()
                            .intersect(female.geneticProfile.fixedTraits.toSet()).size
                        if (overlap > 2) {
                            score -= 10 * goal.priority
                            matchReasons.add("High trait overlap reduces diversity score.")
                        }
                    }
                }
                BreedingGoalType.BREED_STABILIZATION -> {
                    if (male.breed == female.breed && male.breed != "Mixed") {
                        score += 30 * goal.priority
                        matchedGoals.add(goal.type)
                        matchReasons.add("Reinforces ${male.breed} characteristics.")
                    }
                }
                BreedingGoalType.SEX_LINKED_SORTING -> {
                    // Logic handled in PRO engine, basic pass here
                }
                BreedingGoalType.EXOTIC_FEATURES -> {
                   // Logic handled in PRO engine, basic pass here
                }
            }
        }

        // 3. Prevent Inbreeding
        if (male.motherId != null && male.motherId == female.motherId) {
            score -= 80
            matchReasons.add("CRITICAL: Same mother detected!")
        }
        if (male.localId == female.fatherId || female.localId == male.fatherId) {
            score -= 100
            matchReasons.add("CRITICAL: Parent-child pairing detected!")
        }

        return Recommendation(
            male = male,
            female = female,
            score = score.coerceIn(0, 100),
            rationale = if (matchReasons.isEmpty()) "Standard pairing for ${male.species}." else matchReasons.joinToString(" "),
            goalMatches = matchedGoals.distinct()
        )
    }

    fun getUnderperformingBirds(
        birds: List<Bird>,
        incubations: List<Incubation>
    ): List<Bird> {
        val completed = incubations.filter { it.hatchCompleted }
        return birds.filter { bird ->
            val birdIncubations = completed.filter { it.birdId == bird.localId || it.fatherBirdId == bird.localId }
            if (birdIncubations.size < 2) return@filter false
            
            val totalEggs = birdIncubations.sumOf { it.eggsCount }
            val totalHatched = birdIncubations.sumOf { it.hatchedCount }
            val rate = if (totalEggs > 0) totalHatched.toFloat() / totalEggs else 0f
            
            rate < 0.3f
        }
    }
}

