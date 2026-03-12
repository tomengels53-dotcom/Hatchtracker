package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.model.Bird
import com.example.hatchtracker.model.genetics.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HeterosisEstimator @Inject constructor(
    private val confidenceEvaluator: InsightConfidenceEvaluator
) {
    fun estimate(
        sire: Bird,
        dam: Bird,
        breedComposition: List<BreedContribution>
    ): HeterosisEstimate? {
        // Heterosis is typically maximal in F1 crosses of unrelated heritage lines.
        if (sire.breedId == dam.breedId && breedComposition.size == 1) return null
        
        val inputs = mutableListOf("sire.breedId", "dam.breedId")
        val assumptions = mutableListOf("panmictic-vigor-baseline")
        
        // F1 detection: Primary cross if parents are different breeds
        val isFirstCross = sire.breedId != dam.breedId
        if (!isFirstCross) return null
        
        val evidence = confidenceEvaluator.evaluate(
            inputs = inputs,
            assumptions = assumptions,
            derivationPath = "hybrid-vigor-general-model",
            potentialMissing = listOf("speciesHeterosisFactor")
        )

        return HeterosisEstimate(
            presence = true,
            score = 0.12, // 12% baseline hybrid vigor boost
            impactedTraits = listOf("Growth Rate", "Egg Yield", "Overall Vigor", "Survivability"),
            evidence = evidence
        )
    }
}
