package com.example.hatchtracker.domain.breeding.hatchy

import com.example.hatchtracker.model.Bird
import com.example.hatchtracker.model.genetics.*
import com.example.hatchtracker.model.breeding.*

/**
 * Builds human-readable explanations for genetic predictions.
 * Used by HatchyGeneticsRouter for Q&A.
 */
object GeneticsExplanationBuilder {

    /**
     * Builds a goal-aware advisory explanation based on the genetic insight contract.
     */
    fun buildAdvisoryExplanation(
        contract: GeneticInsightAdvisoryContract,
        report: GeneticInsightReport
    ): String {
        val sb = StringBuilder()

        // 1. Observation (Summary)
        val summary = if (contract.insightSummaryCodes.isNotEmpty()) {
            "I've analyzed the genetics for this pairing. The main finding is **${contract.insightSummaryCodes.first().replace("_", " ").lowercase()}**."
        } else {
            "I've analyzed the genetics for this pairing. The results look stable."
        }
        sb.append(summary).append("\n\n")

        // 2. Meaning (Warning/Confidence)
        if (contract.topWarningCode != GeneticInsightAdvisoryContract.WARNING_NONE) {
            sb.append("**Caution**: I've detected a risk regarding **${contract.topWarningCode.replace("_", " ").lowercase()}**. ")
        }

        if (contract.confidenceBand == InsightConfidence.LOW) {
            sb.append("Note that my confidence in this prediction is low due to limited pedigree or composition data. ")
        }
        
        if (contract.whyUnavailableCode != GeneticInsightAdvisoryContract.UNAVAILABLE_NONE) {
            val reason = when (contract.whyUnavailableCode) {
                GeneticAdvisoryCodes.MISSING_PEDIGREE -> "missing pedigree history"
                GeneticAdvisoryCodes.MISSING_BREED_COMPOSITION -> "incomplete breed composition"
                else -> "insufficient metadata"
            }
            sb.append("Some insights are restricted because of $reason.")
        }
        sb.append("\n\n")

        // 3. Action Guidance
        if (contract.topActionCategory != GeneticInsightAdvisoryContract.ACTION_NONE) {
            sb.append("### Recommended Action\n")
            sb.append("I recommend focusing on **${contract.topActionCategory.replace("_", " ").lowercase()}** to optimize your breeding goals.")
        }

        return sb.toString()
    }

    fun buildExplanation(
        sire: Bird, 
        dam: Bird, 
        malePred: PhenotypeResult, 
        femalePred: PhenotypeResult, 
        generalPred: PhenotypeResult
    ): String {
        val sb = StringBuilder()
        val sireLabel = displayName(sire)
        val damLabel = displayName(dam)
        
        sb.append("Here's the breakdown for $sireLabel (Sire) x $damLabel (Dam):\n\n")
        
        // 1. Auto-Sexing (Barring)
        val malesBarred = malePred.probabilities.any { it.phenotypeId == "barred" }
        val femalesBarred = femalePred.probabilities.any { it.phenotypeId == "barred" }
        
        if (malesBarred != femalesBarred) {
            sb.append("**Auto-Sexing Alert!**\n")
            if (malesBarred && !femalesBarred) {
                 sb.append("- **Males**: Will likely have the Barred trait (Head spot at hatch).\n")
                 sb.append("- **Females**: Will NOT have Barring (Solid color).\n")
                 sb.append("This is a classic Sex-Link pairing. You can sort chicks by head spot.\n\n")
            } else {
                 sb.append("- **Males**: Non-Barred.\n")
                 sb.append("- **Females**: Barred.\n")
                 sb.append("This is a Reverse Sex-Link pairing.\n\n")
            }
        }
        
        // 2. Egg Color
        val greenProb = generalPred.probabilities.find { it.phenotypeId == "egg_green" }?.probability ?: 0.0
        val blueProb = generalPred.probabilities.find { it.phenotypeId == "egg_blue" }?.probability ?: 0.0
        val brownProb = generalPred.probabilities.find { it.phenotypeId == "egg_brown" }?.probability ?: 0.0
        
        if (greenProb > 0) {
             sb.append("**Egg Color**:\n")
             sb.append("- ${(greenProb * 100).toInt()}% chance of Green/Olive eggs (Blue shell + Brown pigment).\n")
             if (blueProb > 0) sb.append("- ${(blueProb * 100).toInt()}% chance of Blue eggs.\n")
             if (brownProb > 0) sb.append("- ${(brownProb * 100).toInt()}% chance of Brown eggs.\n")
             sb.append("\n")
        }
        
        // 3. Special Traits
        val nakedNeckProb = generalPred.probabilities.find { it.phenotypeId == "naked_neck" }?.probability ?: 0.0
        if (nakedNeckProb > 0) {
            sb.append("**Naked Neck**: ${(nakedNeckProb * 100).toInt()}% of offspring will have the Naked Neck trait.\n\n")
        }
        
        // 4. Assumptions
        val assumptions = (malePred.assumptions + femalePred.assumptions + generalPred.assumptions).distinct()
        if (assumptions.isNotEmpty()) {
            sb.append("*Note: Calculations include assumptions based on breed standards: ${assumptions.joinToString(", ")}.*")
        }
        
        return sb.toString()
    }

    private fun displayName(bird: Bird): String {
        return bird.breed.takeIf { it.isNotBlank() }
            ?: "Bird ${bird.localId}"
    }
}
