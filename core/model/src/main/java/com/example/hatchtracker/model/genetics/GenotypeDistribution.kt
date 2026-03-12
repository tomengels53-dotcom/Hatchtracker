package com.example.hatchtracker.model.genetics

data class GenotypeDistribution(
    val locusId: String,
    // Map of GenotypeAtLocus to Probability (0.0 - 1.0)
    val outcomes: Map<GenotypeAtLocus, Double>
) {
    /**
     * Helper to get total probability of having at least one copy of an allele.
     */
    fun probabilityOfAllele(allele: String): Double {
        return outcomes.entries.sumOf { (genotype, prob) ->
            if (genotype.alleles.contains(allele)) prob else 0.0
        }
    }
    
    /**
     * Helper to get probability of being homozygous for an allele.
     */
    fun probabilityHomozygous(allele: String): Double {
        return outcomes.entries.sumOf { (genotype, prob) ->
             if (genotype.alleles.size == 2 && genotype.alleles.all { it == allele }) prob else 0.0
        }
    }
}
