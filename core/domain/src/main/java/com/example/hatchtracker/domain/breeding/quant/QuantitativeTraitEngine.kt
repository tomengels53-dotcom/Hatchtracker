package com.example.hatchtracker.domain.breeding.quant

import java.util.Random
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Engine for predicting quantitative traits (polygenic) using the Infinitesimal Model.
 * Supporting:
 * - Additive Genetic Variance (Heritability)
 * - Environmental Variance (Noise)
 * - Inbreeding Depression
 * 
 * MUST be deterministic based on seed.
 */
@Singleton
class QuantitativeTraitEngine @Inject constructor() {

    fun predictTrait(
        traitId: String,
        sireValue: Double,
        damValue: Double,
        heritability: Double, // h^2
        inbreedingCoefficient: Double,
        rngSeed: Long
    ): Double {
        val random = Random(rngSeed)
        
        // 1. Mid-parent value (Additive expectation)
        val midParent = (sireValue + damValue) / 2.0
        
        // 2. Mendelian Sampling Variance (Segregation variance)
        // Var(MS) = (1/2) * V_A * (1 - F_parents_avg)
        // Simplified: Standard deviation of segregation = sqrt(0.5) * genetic_std_dev
        // We assume genetic_std_dev is normalized or passed in. 
        // For MVP, we use a heuristic based on heritability.
        // A trait with h^2=0.5 implies 50% variance is genetic.
        
        val segregationStdDev = 1.0 // Placeholder unit variance
        val segregationEffect = random.nextGaussian() * segregationStdDev * sqrt(0.5)
        
        // 3. Environmental Variance (Noise)
        // V_E = V_P - V_A
        val envStdDev = sqrt(1.0 - heritability) // Assuming phenotypic variance = 1
        val envEffect = random.nextGaussian() * envStdDev
        
        // 4. Inbreeding Depression (Linear penalty)
        // Reduces value for fitness traits (size, vigor)
        val inbreedingPenalty = inbreedingCoefficient * 10.0 // Heuristic slope
        
        return midParent + segregationEffect + envEffect - inbreedingPenalty
    }
    
    /**
     * Batch prediction for a virtual litter.
     */
    fun predictLitter(
        traitId: String,
        sireValue: Double,
        damValue: Double,
        heritability: Double,
        inbreedingCoefficient: Double,
        litterSize: Int,
        baseSeed: Long
    ): List<Double> {
        return (0 until litterSize).map { index ->
            predictTrait(
                traitId, 
                sireValue, 
                damValue, 
                heritability, 
                inbreedingCoefficient, 
                rngSeed = baseSeed + index
            )
        }
    }
}
