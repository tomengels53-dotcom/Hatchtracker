package com.example.hatchtracker.domain.genetics.phenotype

import com.example.hatchtracker.data.models.ConfidenceLevel
import com.example.hatchtracker.domain.genetics.GeneticLocusCatalog
import com.example.hatchtracker.model.Species
import com.example.hatchtracker.model.genetics.*

/**
 * Deterministic Phenotype Mapper.
 * ONE WAY: Genotype -> Phenotype.
 */
object PhenotypeResolver {

    /**
     * Resolves the probability of observable traits based on genotype distributions.
     */
    fun resolve(
        species: Species = Species.CHICKEN,
        genotypes: Map<String, GenotypeDistribution>
    ): PhenotypeResult {
        val probs = mutableListOf<PhenotypeProbability>()
        val assumptions = mutableListOf<String>()

        if (species == Species.CHICKEN) {
            val distO = genotypes[GeneticLocusCatalog.LOCUS_O]
            val distBr = genotypes[GeneticLocusCatalog.LOCUS_BR]
            val distB = genotypes[GeneticLocusCatalog.LOCUS_B]
            val distBl = genotypes[GeneticLocusCatalog.LOCUS_BL]
            val distNa = genotypes[GeneticLocusCatalog.LOCUS_NA]
            val distFm = genotypes[GeneticLocusCatalog.LOCUS_FM]
            val distE = genotypes[GeneticLocusCatalog.LOCUS_E]

            val pBlueBase = distO?.probabilityOfAllele("O") ?: 0.0
            val pBrownOverlay = distBr?.probabilityOfAllele("BR") ?: 0.0
            val pGreen = minOf(pBlueBase, pBrownOverlay)
            val pBlue = clamp01(pBlueBase - pGreen)
            val pBrown = clamp01(pBrownOverlay - pGreen)
            val pEggColored = clamp01(pBlue + pBrown + pGreen)
            val pWhiteEgg = clamp01(1.0 - pEggColored)

            if (pBlue > 0.0) probs.add(PhenotypeProbability(PhenotypeId.EGG_BLUE, pBlue))
            if (pBrown > 0.0) probs.add(PhenotypeProbability(PhenotypeId.EGG_BROWN, pBrown))
            if (pGreen > 0.0) {
                probs.add(PhenotypeProbability(PhenotypeId.EGG_GREEN, pGreen))
                probs.add(PhenotypeProbability(PhenotypeId.EGG_OLIVE, pGreen))
            }
            if (pWhiteEgg > 0.0) probs.add(PhenotypeProbability(PhenotypeId.EGG_WHITE, pWhiteEgg))

            val pBarred = distB?.probabilityOfAllele("B") ?: 0.0
            if (pBarred > 0.0) probs.add(PhenotypeProbability(PhenotypeId.BARRED, pBarred))

            if (distBl != null) {
                val pSplash = clamp01(distBl.probabilityHomozygous("Bl"))
                val pBlueDilution = clamp01(distBl.probabilityOfAllele("Bl") - pSplash)
                if (pSplash > 0.0) probs.add(PhenotypeProbability(PhenotypeId.SPLASH_DILUTION, pSplash))
                if (pBlueDilution > 0.0) probs.add(PhenotypeProbability(PhenotypeId.BLUE_DILUTION, pBlueDilution))
            }

            val pNakedNeck = distNa?.probabilityOfAllele("Na") ?: 0.0
            if (pNakedNeck > 0.0) probs.add(PhenotypeProbability(PhenotypeId.NAKED_NECK, pNakedNeck))

            val pFibro = distFm?.probabilityOfAllele("Fm") ?: 0.0
            if (pFibro > 0.0) probs.add(PhenotypeProbability(PhenotypeId.FIBROMELANOSIS, pFibro))

            val pExtendedBlack = distE?.probabilityOfAllele("E") ?: 0.0
            if (pExtendedBlack > 0.0) probs.add(PhenotypeProbability(PhenotypeId.EXTENDED_BLACK, pExtendedBlack))
        } else {
            val distC = genotypes["${species.name.uppercase()}__C_Locus"]
            if (distC != null) {
                val pWhite = clamp01(distC.probabilityHomozygous("c"))
                if (pWhite > 0.0) probs.add(PhenotypeProbability(PhenotypeId.WHITE_PLUMAGE, pWhite))
                if (pWhite < 1.0) probs.add(PhenotypeProbability(PhenotypeId.COLORED_PLUMAGE, 1.0 - pWhite))
            } else {
                assumptions.add("No species-specific C locus distribution found for phenotype mapping.")
            }
        }

        return PhenotypeResult(
            probabilities = probs,
            assumptions = assumptions,
            confidence = ConfidenceLevel.HIGH
        )
    }

    private fun clamp01(value: Double): Double = value.coerceIn(0.0, 1.0)
}

