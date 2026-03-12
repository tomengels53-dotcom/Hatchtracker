package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.TraitTarget

import com.example.hatchtracker.model.GeneticProfile
import com.example.hatchtracker.model.Sex
import com.example.hatchtracker.model.Species
import com.example.hatchtracker.domain.genetics.GeneticLocusCatalog
import com.example.hatchtracker.domain.genetics.engine.GeneticProbabilityEngine
import com.example.hatchtracker.domain.genetics.infer.GenotypeInferencer
import com.example.hatchtracker.domain.genetics.phenotype.PhenotypeResolver
import com.example.hatchtracker.model.genetics.GenotypeCall
import com.example.hatchtracker.model.genetics.GenotypeDistribution
import com.example.hatchtracker.model.genetics.LocusDefinition
import com.example.hatchtracker.model.genetics.PhenotypeResult
import com.example.hatchtracker.model.genetics.QuantitativeTraitModel
import kotlin.math.max
import kotlin.math.min
import javax.inject.Inject
import javax.inject.Singleton

/**
 * THE ONLY ENTRYPOINT for breeding prediction math.
 * Orchestrates genotype inference, probability engine, and phenotype resolution.
 * 
 * FACADE RULE: Only this service may call GeneticProbabilityEngine and PhenotypeResolver.
 */
@Singleton
class BreedingPredictionService @Inject constructor(
    private val genotypeInferencer: GenotypeInferencer
) {

    /**
     * Returns the list of available genetic loci/traits for a given species.
     */
    fun getAvailableTraits(species: Species): List<LocusDefinition> {
        return GeneticLocusCatalog.lociForSpecies(species)
    }

    /**
     * Unified method to predict breeding outcomes from parent profiles.
     */
    fun predictBreeding(
        species: Species,
        sireProfile: GeneticProfile,
        damProfile: GeneticProfile,
        sireBreedId: String? = null,
        damBreedId: String? = null,
        offspringSex: Sex? = null
    ): BreedingPredictionResult {
        // 1. Resolve parent genotypes
        val sireCalls = genotypeInferencer.inferGenotype(sireProfile, Sex.MALE, species, sireBreedId)
        val damCalls = genotypeInferencer.inferGenotype(damProfile, Sex.FEMALE, species, damBreedId)

        // 2. Predict distributions
        val distributions = GeneticProbabilityEngine.predict(species, sireCalls, damCalls, offspringSex)

        // 3. Resolve phenotypes (Overall)
        val overallPhenotypes = PhenotypeResolver.resolve(species, distributions)

        // 4. Resolve Sex-Aware Phenotypes (if applicable)
        val outcomes = mutableListOf<PhenotypeOutcome>()
        
        if (offspringSex == null) {
            val maleDists = splitDistributions(distributions, Sex.MALE, species)
            val femaleDists = splitDistributions(distributions, Sex.FEMALE, species)
            
            val malePhenos = PhenotypeResolver.resolve(species, maleDists)
            val femalePhenos = PhenotypeResolver.resolve(species, femaleDists)
            
            // Map Overall probabilities and attach male/female subsets
            overallPhenotypes.probabilities.forEach { prob ->
                val mProb = malePhenos.probabilities.find { it.phenotypeId == prob.phenotypeId }?.probability
                val fProb = femalePhenos.probabilities.find { it.phenotypeId == prob.phenotypeId }?.probability
                
                outcomes.add(
                    PhenotypeOutcome(
                        traitId = prob.phenotypeId,
                        valueId = prob.phenotypeId, // Simplified for MVP mapping
                        label = prob.phenotypeId.replace("_", " "),
                        overallProbability = prob.probability,
                        maleProbability = mProb,
                        femaleProbability = fProb
                    )
                )
            }
        } else {
             overallPhenotypes.probabilities.forEach { prob ->
                outcomes.add(
                    PhenotypeOutcome(
                        traitId = prob.phenotypeId,
                        valueId = prob.phenotypeId,
                        label = prob.phenotypeId.replace("_", " "),
                        overallProbability = prob.probability,
                        maleProbability = if (offspringSex == Sex.MALE) prob.probability else null,
                        femaleProbability = if (offspringSex == Sex.FEMALE) prob.probability else null
                    )
                )
             }
        }

        // 5. Quantitative Traits
        val sireQuant = genotypeInferencer.inferQuantitativeTraits(sireProfile, sireBreedId)
        val damQuant = genotypeInferencer.inferQuantitativeTraits(damProfile, damBreedId)
        
        val quantitative = predictQuantitativeTraitsFromResolved(sireQuant, damQuant)

        return BreedingPredictionResult(
            sireGenotypes = sireCalls,
            damGenotypes = damCalls,
            offspringDistributions = distributions,
            phenotypeResult = overallPhenotypes,
            phenotypeOutcomes = outcomes,
            quantitativePredictions = quantitative
        )
    }

    private fun splitDistributions(
        distributions: Map<String, GenotypeDistribution>,
        targetSex: Sex,
        species: Species
    ): Map<String, GenotypeDistribution> {
        val split = mutableMapOf<String, GenotypeDistribution>()
        val loci = GeneticLocusCatalog.lociForSpecies(species)
        
        distributions.forEach { (locusId, dist) ->
            val locusDef = loci.find { it.locusId == locusId }
            if (locusDef?.inheritance == com.example.hatchtracker.model.genetics.InheritanceType.Z_LINKED) {
                val filteredOutcomes = dist.outcomes.filter { (genotype, _) ->
                    if (targetSex == Sex.MALE) genotype.alleles.size == 2
                    else genotype.alleles.size == 1
                }
                val totalWeight = filteredOutcomes.values.sum()
                if (totalWeight > 0) {
                    val normalized = filteredOutcomes.mapValues { it.value / totalWeight }
                    split[locusId] = GenotypeDistribution(locusId, normalized)
                }
            } else {
                // Autosomal: Same for both sexes
                split[locusId] = dist
            }
        }
        return split
    }

    private fun predictQuantitativeTraitsFromResolved(
        sireQuant: Map<String, com.example.hatchtracker.model.genetics.QuantitativeTraitValue>, 
        damQuant: Map<String, com.example.hatchtracker.model.genetics.QuantitativeTraitValue>
    ): List<QuantitativePrediction> {
        val predictions = mutableListOf<QuantitativePrediction>()

        // Find all quantitative traits present in either sire or dam
        val allTraitKeys = sireQuant.keys + damQuant.keys
        
        for (key in allTraitKeys) {
            val sireVal = sireQuant[key]
            val damVal = damQuant[key]
            
            val sireMean = sireVal?.mean ?: 0.5
            val damMean = damVal?.mean ?: 0.5
            
            val sireVariance = sireVal?.variance ?: 0.0
            val damVariance = damVal?.variance ?: 0.0
            
            // Assume default heritability and environmental variance if not globally provided in this scope.
            val heritability = 0.3
            val environmentalVariance = 0.1
            
            // MATH:
            // offspringMean = (parentA.mean + parentB.mean) / 2
            // parentVarianceMean = (parentA.variance + parentB.variance) / 2
            // offspringVariance = (1 - heritability) * environmentalVariance + (heritability * parentVarianceMean)
            // Clamp mean 0.0-1.0 and variance <= 0.25
            
            var offspringMean = (sireMean + damMean) / 2.0
            val parentVarianceMean = (sireVariance + damVariance) / 2.0
            var offspringVariance = ((1.0 - heritability) * environmentalVariance) + (heritability * parentVarianceMean)
            
            // Apply Dominance Modifiers (Deterministic S-curve bias)
            val dominanceStrength = 0.0 // Default, would come from model catalog
            val dominanceDirection = 1  // Default
            val dominanceCenter = 0.5   // Default
            
            if (dominanceStrength > 0.0) {
                val m = offspringMean
                val s = dominanceStrength
                val dir = dominanceDirection
                val c = dominanceCenter
                val mPrime = m + dir * s * (m - c) * (1.0 - kotlin.math.abs(m - c)) * 2.0
                offspringMean = mPrime
            }
            
            offspringMean = max(0.0, min(1.0, offspringMean))
            offspringVariance = max(0.0, min(0.25, offspringVariance))
            
            predictions.add(
                QuantitativePrediction(
                    traitKey = key,
                    mean = offspringMean,
                    variance = offspringVariance,
                    heritability = heritability
                )
            )
        }
        
        return predictions
    }

    /**
     * Internal: Resolves high-level GeneticProfiles into deterministic GenotypeCalls.
     */
    private fun resolveParentGenotypeCalls(
        profile: GeneticProfile,
        sex: Sex,
        species: Species,
        breedId: String? = null
    ): Map<String, GenotypeCall> {
        return genotypeInferencer.inferGenotype(profile, sex, species, breedId)
    }

    /**
     * Internal: Predicts offspring genotype distributions based on parent calls.
     */
    private fun predictOffspringDistributions(
        species: Species,
        sireCalls: Map<String, GenotypeCall>,
        damCalls: Map<String, GenotypeCall>,
        offspringSex: Sex? = null
    ): Map<String, GenotypeDistribution> {
        return GeneticProbabilityEngine.predict(species, sireCalls, damCalls, offspringSex)
    }

    /**
     * Internal: Convenience method to get final phenotype result from distributions.
     */
    private fun resolvePhenotype(
        species: Species,
        distributions: Map<String, GenotypeDistribution>
    ): PhenotypeResult {
        return PhenotypeResolver.resolve(species, distributions)
    }

    fun calculateLineStability(
        species: Species,
        generationIndex: Int,
        prediction: BreedingPredictionResult,
        goal: TraitTarget?,
        risk: BreedingRiskResult,
        diversityMeta: DiversityMeta,
        previous: LineStabilitySnapshot? = null
    ): LineStabilitySnapshot {
        return LineStabilityIndexCalculator.compute(
            species = species,
            generationIndex = generationIndex,
            prediction = prediction,
            goal = goal,
            risk = risk,
            diversityMeta = diversityMeta,
            previous = previous
        )
    }
}

