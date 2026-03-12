package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.ConfidenceLevel
import com.example.hatchtracker.model.Bird
import com.example.hatchtracker.model.BreedingSafeguard
import com.example.hatchtracker.model.Species

enum class RiskLevel {
    HIGH_RISK, CAUTION, NONE
}

data class BreedingRiskResult(
    val riskLevel: RiskLevel,
    val inbreedingRiskScore: Double, // 0.0 to 1.0
    val bottleneckRiskScore: Double, // 0.0 to 1.0
    val effectivePopulation: Double,
    val reasons: List<String>,
    val recommendation: String
)

object GeneticRiskAnalyzer {

    private val GENETIC_FIXATION_WHITELIST = setOf(
        "pea comb", "rose comb", "single comb", "blue egg", "green egg", "dark brown egg",
        "naked neck", "fibromelanosis", "barring", "blue dilution", "splash", "extended black"
    )

    fun analyzeBreedingRisk(
        parentA: Bird,
        parentB: Bird,
        population: List<Bird>
    ): BreedingRiskResult {
        val reasons = mutableListOf<String>()
        var bottleneckScore = 0

        val speciesA = parentA.species
        val speciesB = parentB.species

        if (speciesA != speciesB && speciesA != Species.UNKNOWN && speciesB != Species.UNKNOWN) {
            return BreedingRiskResult(
                riskLevel = RiskLevel.HIGH_RISK,
                inbreedingRiskScore = 1.0,
                bottleneckRiskScore = 1.0,
                effectivePopulation = 1.0,
                reasons = listOf("CRITICAL: Cross-species breeding detected (${speciesA.name} x ${speciesB.name})."),
                recommendation = "DO NOT PROCEED: Interspecies hybrids are not supported in standard logic."
            )
        }

        val birdMap = population.associateBy { it.localId }
        
        // Effective Population Estimate (Ne)
        // Use the canonical breeder formula: Ne = (4 * Nm * Nf) / (Nm + Nf)
        val malesCount = population.count { it.sex == com.example.hatchtracker.model.Sex.MALE }
        val femalesCount = population.count { it.sex == com.example.hatchtracker.model.Sex.FEMALE }
        
        // Handle edge cases: if we have zero of one sex, Ne is essentially 0 for breeding sustainability
        val effectivePop = if (malesCount == 0 || femalesCount == 0) {
            1.0 // Minimal floor to avoid div by zero, but functionally extinct diversity
        } else {
            (4.0 * malesCount * femalesCount) / (malesCount + femalesCount)
        }

        if (effectivePop < 15.0) { // Breeder-grade warning threshold
            bottleneckScore += 25
            reasons.add("Critical low diversity (Ne: ${String.format("%.1f", effectivePop)}). Selection narrowing too fast.")
        } else if (effectivePop < 50.0) {
            bottleneckScore += 15
            reasons.add("Limited genetic reservoir (Effective Population Size Ne: ${String.format("%.1f", effectivePop)}).")
        }

        val coi = AncestryService.calculateCOI(parentA, parentB, birdMap)
        
        if (coi > 0) {
            bottleneckScore += (coi * 300).toInt() // e.g. 0.25 COI -> 75 points
            reasons.add("Lineage overlap detected (COI: ${String.format("%.1f", coi * 100)}%).")
        }

        // Lethality check (delegated to BreedingSafeguardManager for single source of truth)
        if (BreedingSafeguardManager.evaluatePair(parentA, listOf(parentB), birdMap) == BreedingSafeguard.BlockingLethal) {
             bottleneckScore += 100
             reasons.add("CRITICAL: Known lethal gene combination based on breed standards.")
        }

        val fixedA = parentA.geneticProfile.fixedTraits.filter { it in GENETIC_FIXATION_WHITELIST }.toSet()
        val fixedB = parentB.geneticProfile.fixedTraits.filter { it in GENETIC_FIXATION_WHITELIST }.toSet()
        if (fixedA.isNotEmpty() || fixedB.isNotEmpty()) {
            val sharedFixed = fixedA.intersect(fixedB)
            val totalUnique = (fixedA + fixedB).size
            val fixationRatio = if (totalUnique > 0) sharedFixed.size.toDouble() / totalUnique else 0.0
            if (fixationRatio > 0.7) {
                bottleneckScore += 30
                reasons.add("High genetic trait fixation overlap (${(fixationRatio * 100).toInt()}%).")
            }
        }

        if (population.isNotEmpty()) {
            val breedMembers = population.filter { it.breedId == parentA.breedId && it.species == parentA.species }
            if (breedMembers.size > 5) {
                val traitOccurrences = mutableMapOf<String, Int>()
                breedMembers.forEach { bird ->
                    bird.geneticProfile.fixedTraits.filter { it in GENETIC_FIXATION_WHITELIST }.forEach { trait ->
                        traitOccurrences[trait] = traitOccurrences.getOrDefault(trait, 0) + 1
                    }
                }
                val overFixedTraits = traitOccurrences.filter { it.value.toDouble() / breedMembers.size > 0.9 }
                if (overFixedTraits.isNotEmpty()) {
                    bottleneckScore += 10
                    reasons.add("Genetic drift warning: ${overFixedTraits.size} traits are fixed in >90% of local population.")
                }
            }
        }

        val repeatedSires = detectSireRepetition(parentA, parentB, population)
        if (repeatedSires.isNotEmpty()) {
            bottleneckScore += 25 * repeatedSires.size
            reasons.add("Genetic bottleneck: sire repeats in lineage.")
        }

        if (parentA.geneticProfile.confidenceLevelEnum == ConfidenceLevel.LOW &&
            parentB.geneticProfile.confidenceLevelEnum == ConfidenceLevel.LOW
        ) {
            bottleneckScore += 15
            reasons.add("Trait instability: both parents are LOW confidence.")
        }

        val riskLevel = when {
            bottleneckScore >= 70 -> RiskLevel.HIGH_RISK
            bottleneckScore >= 30 -> RiskLevel.CAUTION
            else -> RiskLevel.NONE
        }

        val recommendation = when {
            riskLevel == RiskLevel.HIGH_RISK -> "DO NOT PROCEED: Extreme bottleneck risk or lethality detected. Introduce unrelated stock immediately."
            effectivePop < 20.0 -> "PROCEED WITH CAUTION: Genetic diversity is too low for long-term survival. Plan for an outcross."
            riskLevel == RiskLevel.CAUTION -> "PROCEED WITH CAUTION: Lines are narrowing. Consider split-line breeding."
            else -> "SAFE: Diversity and stability are acceptable for this generation."
        }

        return BreedingRiskResult(
            riskLevel = riskLevel, 
            inbreedingRiskScore = (coi * 4.0).coerceIn(0.0, 1.0), // Scale 0.25 COI to 1.0 penalty impact
            bottleneckRiskScore = (1.0 - (effectivePop / 50.0)).coerceIn(0.0, 1.0),
            effectivePopulation = effectivePop, 
            reasons = reasons, 
            recommendation = recommendation
        )
    }

    private fun detectSireRepetition(a: Bird, b: Bird, population: List<Bird>): List<Long> {
        val sireIds = mutableListOf<Long>()
        val visited = mutableSetOf<Long>()

        fun collectSires(bird: Bird?) {
            if (bird == null) return
            if (!visited.add(bird.localId)) return
            bird.fatherId?.let {
                sireIds.add(it)
                collectSires(population.find { p -> p.localId == it })
            }
            bird.motherId?.let { collectSires(population.find { p -> p.localId == it }) }
        }
        collectSires(a)
        collectSires(b)
        return sireIds.groupBy { it }.filter { it.value.size > 1 }.keys.toList()
    }

    private fun hasSharedGreatGrandparent(a: Bird, b: Bird, population: List<Bird>): Boolean {
        return getGreatGrandparents(a, population).intersect(getGreatGrandparents(b, population)).isNotEmpty()
    }

    private fun getGreatGrandparents(bird: Bird, population: List<Bird>): Set<Long> {
        val gParents = getGrandparents(bird, population)
        val ggParents = mutableSetOf<Long>()
        gParents.forEach { id ->
            val gp = population.find { it.localId == id }
            gp?.motherId?.let { ggParents.add(it) }
            gp?.fatherId?.let { ggParents.add(it) }
        }
        return ggParents
    }

    private fun hasSharedParent(a: Bird, b: Bird): Boolean {
        if (a.motherId != null && a.motherId == b.motherId) return true
        if (a.fatherId != null && a.fatherId == b.fatherId) return true
        return false
    }

    private fun hasSharedGrandparent(a: Bird, b: Bird, population: List<Bird>): Boolean {
        return getGrandparents(a, population).intersect(getGrandparents(b, population)).isNotEmpty()
    }

    private fun getGrandparents(bird: Bird, population: List<Bird>): Set<Long> {
        val grandparents = mutableSetOf<Long>()
        val mother = population.find { it.localId == bird.motherId }
        val father = population.find { it.localId == bird.fatherId }
        mother?.motherId?.let { grandparents.add(it) }
        mother?.fatherId?.let { grandparents.add(it) }
        father?.motherId?.let { grandparents.add(it) }
        father?.fatherId?.let { grandparents.add(it) }
        return grandparents
    }
}

