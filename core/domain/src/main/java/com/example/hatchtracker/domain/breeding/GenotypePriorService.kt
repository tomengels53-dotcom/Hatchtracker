package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.model.GeneticProfile
import com.example.hatchtracker.domain.genetics.GeneticLocusCatalog
import com.example.hatchtracker.model.Species
import com.example.hatchtracker.model.genetics.Certainty
import com.example.hatchtracker.model.genetics.GenotypeCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GenotypePriorService @Inject constructor() {

    /**
     * Fills in missing genotype data with wildtype/baseline priors.
     * Marks certainty as UNKNOWN/INFERRED to trigger penalties in scorer.
     */
    fun buildPriors(
        species: Species,
        profile: GeneticProfile
    ): GeneticProfile {
        val existingCalls = (profile.genotypeCalls ?: emptyMap()).toMutableMap()
        val allLoci = GeneticLocusCatalog.lociForSpecies(species)

        allLoci.forEach { locus ->
            if (!existingCalls.containsKey(locus.locusId)) {
                val alleles = if (locus.inheritance.name == "Z_LINKED") {
                    listOf(locus.defaultWildtype)
                } else {
                    listOf(locus.defaultWildtype, locus.defaultWildtype)
                }
                existingCalls[locus.locusId] = GenotypeCall(
                    locusId = locus.locusId,
                    alleles = alleles,
                    certainty = Certainty.UNKNOWN
                )
            }
        }

        return profile.copy(genotypeCalls = existingCalls)
    }
}

