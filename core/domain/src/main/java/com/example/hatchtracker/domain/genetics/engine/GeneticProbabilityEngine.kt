package com.example.hatchtracker.domain.genetics.engine

import com.example.hatchtracker.domain.genetics.GeneticLocusCatalog
import com.example.hatchtracker.model.Sex
import com.example.hatchtracker.model.Species
import com.example.hatchtracker.model.genetics.*

/**
 * Core Deterministic Engine.
 * Calculates Punnett Squares and Sex-Linked probability distributions.
 */
object GeneticProbabilityEngine {

    /**
     * Predicts offspring genotype distributions for all loci relevant to the species.
     * @param species The species being bred
     * @param maleGenotype Sire's genotype calls
     * @param femaleGenotype Dam's genotype calls
     * @param offspringSex Optional filter for offspring sex
     * @return Map of LocusId -> GenotypeDistribution
     */
    fun predict(
        species: Species = Species.CHICKEN,
        maleGenotype: Map<String, GenotypeCall>,
        femaleGenotype: Map<String, GenotypeCall>,
        offspringSex: Sex? = null
    ): Map<String, GenotypeDistribution> {
        val results = mutableMapOf<String, GenotypeDistribution>()
        val loci = GeneticLocusCatalog.lociForSpecies(species)

        for (locus in loci) {
            val maleCall = maleGenotype[locus.locusId] ?: createUnknownCall(locus, true)
            val femaleCall = femaleGenotype[locus.locusId] ?: createUnknownCall(locus, false)

            val distribution = calculateLocus(locus, maleCall, femaleCall, offspringSex)
            results[locus.locusId] = distribution
        }

        return results
    }

    private fun calculateLocus(
        locus: LocusDefinition,
        sire: GenotypeCall,
        dam: GenotypeCall,
        offspringSex: Sex?
    ): GenotypeDistribution {
        val outcomes = mutableMapOf<GenotypeAtLocus, Double>()
        
        val sireAlleles = normalizeAlleles(sire.alleles, 2, locus.defaultWildtype)
        val damAlleles = if (locus.inheritance == InheritanceType.Z_LINKED) {
            normalizeAlleles(dam.alleles, 1, locus.defaultWildtype)
        } else {
             normalizeAlleles(dam.alleles, 2, locus.defaultWildtype)
        }

        if (locus.inheritance == InheritanceType.Z_LINKED) {
            // Z-Linked Crossing (Sire ZZ, Dam ZW)
            sireAlleles.forEach { sAllele ->
                // Case 1: Dam gives Z (Male Offspring)
                if (offspringSex == null || offspringSex == Sex.MALE) {
                    damAlleles.forEach { dAllele -> 
                        val genotypeKey = createSortedGenotype(locus.locusId, listOf(sAllele, dAllele))
                        val prob = if (offspringSex == Sex.MALE) 0.5 else 0.25
                        outcomes[genotypeKey] = (outcomes[genotypeKey] ?: 0.0) + prob
                    }
                }
                
                // Case 2: Dam gives W (Female Offspring)
                if (offspringSex == null || offspringSex == Sex.FEMALE) {
                    val genotypeKey = createSortedGenotype(locus.locusId, listOf(sAllele))
                    val prob = if (offspringSex == Sex.FEMALE) 0.5 else 0.25
                    outcomes[genotypeKey] = (outcomes[genotypeKey] ?: 0.0) + prob
                }
            }
        } else {
            // Autosomal Crossing (2x2 Punnett)
            sireAlleles.forEach { sAllele ->
                damAlleles.forEach { dAllele ->
                    val genotypeKey = createSortedGenotype(locus.locusId, listOf(sAllele, dAllele))
                     outcomes[genotypeKey] = (outcomes[genotypeKey] ?: 0.0) + 0.25
                }
            }
        }
        
        return GenotypeDistribution(locus.locusId, outcomes)
    }

    private fun createSortedGenotype(locusId: String, alleles: List<String>): GenotypeAtLocus {
        return GenotypeAtLocus(locusId, alleles.sorted())
    }

    private fun normalizeAlleles(alleles: List<String>, count: Int, default: String): List<String> {
        if (alleles.size == count) return alleles
        if (alleles.size > count) return alleles.take(count)
        val mutable = alleles.toMutableList()
        while (mutable.size < count) mutable.add(default)
        return mutable
    }

    private fun createUnknownCall(locus: LocusDefinition, isMale: Boolean): GenotypeCall {
        val alleles = if (locus.inheritance == InheritanceType.Z_LINKED && !isMale) {
            listOf(locus.defaultWildtype)
        } else {
            listOf(locus.defaultWildtype, locus.defaultWildtype)
        }
        return GenotypeCall(locus.locusId, alleles, Certainty.UNKNOWN)
    }
}

