package com.example.hatchtracker.domain.genetics.infer

import com.example.hatchtracker.model.GeneticProfile
import com.example.hatchtracker.domain.genetics.GeneticLocusCatalog
import com.example.hatchtracker.model.Species
import com.example.hatchtracker.model.Sex
import com.example.hatchtracker.model.genetics.GenotypeCall
import com.example.hatchtracker.model.genetics.Certainty
import com.example.hatchtracker.model.genetics.InheritanceType
import com.example.hatchtracker.model.genetics.LocusDefinition
import com.example.hatchtracker.data.repository.BreedStandardRepository
import com.example.hatchtracker.domain.genetics.BreedTraitGeneticsMapper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Infers explicit GenotypeCalls from GeneticProfiles and Breed Standards.
 * Bridges the gap between "Catalog Data" and "Deterministic Genetics".
 */
@Singleton
class GenotypeInferencer @Inject constructor(
    private val breedRepository: BreedStandardRepository,
    private val traitMapper: BreedTraitGeneticsMapper
) {

    fun inferGenotype(
        profile: GeneticProfile, 
        sex: Sex, 
        species: Species = Species.CHICKEN,
        breedId: String? = null
    ): Map<String, GenotypeCall> {
        // 1. Start with explicit V2 genotype calls if they exist (Manual overrides)
        val finalCalls = profile.genotypeCalls?.toMutableMap() ?: mutableMapOf()

        // 2. Fetch breed standard if breedId is provided
        val standard = breedId?.let { breedRepository.getBreedById(it) }
        
        // 3. Project breed standard into genetic signals
        val breedSignals = standard?.let { traitMapper.mapToGenotypeCalls(it) } ?: emptyMap()

        // 4. Resolve traits from profile (fixedTraits + knownGenes)
        val combinedHeuristics = (profile.knownGenes + profile.fixedTraits).map { it.lowercase() }.toSet()
        val isMale = sex == Sex.MALE

        // 5. Build final map
        val relevantLoci = GeneticLocusCatalog.lociForSpecies(species)

        relevantLoci.forEach { locus ->
            // Priority: Explicit Override > Breed Signal > Heuristic Inference > Wildtype
            val existing = finalCalls[locus.locusId]
            if (existing != null && existing.certainty != Certainty.UNKNOWN) return@forEach

            val breedCall = breedSignals[locus.locusId]
            if (breedCall != null) {
                finalCalls[locus.locusId] = breedCall
                return@forEach
            }

            finalCalls[locus.locusId] = inferLocusFromHeuristics(locus, combinedHeuristics, isMale, species)
        }

        return finalCalls
    }

    private fun inferLocusFromHeuristics(
        locus: LocusDefinition, 
        traits: Set<String>, 
        isMale: Boolean,
        species: Species
    ): GenotypeCall {
        val alleles = mutableListOf<String>()
        var certainty = Certainty.UNKNOWN

        if (species == Species.CHICKEN) {
            // Blue Egg logic
            if (locus.locusId == GeneticLocusCatalog.LOCUS_O) {
                if (traits.contains("blue egg") || traits.contains("green egg") || traits.contains("olive egg")) {
                    alleles.add("O")
                    alleles.add("o") 
                    certainty = Certainty.ASSUMED
                }
            }
            
            // Barring logic
            if (locus.locusId == GeneticLocusCatalog.LOCUS_B) {
                if (traits.contains("barred") || traits.contains("cuckoo")) {
                    alleles.add("B")
                    certainty = Certainty.ASSUMED
                    if (isMale) alleles.add("B") else alleles.add("b+")
                }
            }

            // Blue logic
            if (locus.locusId == GeneticLocusCatalog.LOCUS_BL) {
                 if (traits.contains("splash")) {
                     alleles.addAll(listOf("Bl", "Bl"))
                     certainty = Certainty.ASSUMED
                 } else if (traits.contains("blue")) {
                     alleles.addAll(listOf("Bl", "bl+"))
                     certainty = Certainty.ASSUMED
                 }
            }
        }

        // DEFAULT: Fill with Wildtype
        if (alleles.isEmpty()) {
            alleles.add(locus.defaultWildtype)
            if (locus.inheritance == InheritanceType.AUTOSOMAL || (locus.inheritance == InheritanceType.Z_LINKED && isMale)) {
                 alleles.add(locus.defaultWildtype)
            }
            certainty = Certainty.ASSUMED 
        }

        // Z-Linked Adjustment
        if (locus.inheritance == InheritanceType.Z_LINKED && !isMale && alleles.size > 1) {
             return GenotypeCall(locus.locusId, listOf(alleles.first()), certainty)
        }
        
        return GenotypeCall(locus.locusId, alleles, certainty)
    }

    fun inferQuantitativeTraits(
        profile: GeneticProfile,
        breedId: String? = null
    ): Map<String, com.example.hatchtracker.model.genetics.QuantitativeTraitValue> {
        val finalQuant = profile.quantitativeTraits?.toMutableMap() ?: mutableMapOf()
        
        val standard = breedId?.let { breedRepository.getBreedById(it) }
        val breedQuant = standard?.let { traitMapper.mapToQuantitativeTraits(it) } ?: emptyMap()
        
        // Merge: Profile values take precedence over breed standards
        breedQuant.forEach { (key, breedVal) ->
            if (!finalQuant.containsKey(key)) {
                finalQuant[key] = breedVal
            }
        }
        
        return finalQuant
    }
}
