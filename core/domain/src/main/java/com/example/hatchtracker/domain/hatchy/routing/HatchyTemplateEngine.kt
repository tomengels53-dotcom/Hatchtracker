package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.data.models.BreedingExplanation
import com.example.hatchtracker.data.models.ConfidenceLevel
import com.example.hatchtracker.data.models.ExplanationDetail
import com.example.hatchtracker.data.models.ExplanationWarning
import com.example.hatchtracker.data.models.WarningSeverity
import com.example.hatchtracker.data.models.WarningType
import com.example.hatchtracker.model.RecommendedPair

/**
 * The "Brain" of the Hatchy persona.
 * Translates raw data into friendly, educational advice.
 */
object HatchyTemplateEngine {

    fun generateExplanation(
        pairId: String,
        maleName: String,
        femaleName: String,
        details: List<ExplanationDetail>,
        warnings: List<ExplanationWarning>
    ): BreedingExplanation {
        val summary = generateSummary(maleName, femaleName, details, warnings)
        
        return BreedingExplanation(
            pairId = pairId,
            summary = summary,
            details = details,
            warnings = warnings,
            improvementTips = generateTips(warnings)
        )
    }

    fun generateSummary(
        maleName: String,
        femaleName: String,
        details: List<ExplanationDetail>,
        warnings: List<ExplanationWarning>
    ): String {
        // 1. Check for critical warnings first
        val critical = warnings.find { it.severity == WarningSeverity.CRITICAL }
        if (critical != null) {
            return "Now, you listen here. I'd be very cautious with this pair. ${critical.message}. We've always got to put the birds' welfare first, don't we?"
        }

        // 2. Assess overall confidence
        val lowConfidenceCount = details.count { it.confidenceLevel == ConfidenceLevel.LOW }
        if (lowConfidenceCount > details.size / 2) {
            return "Genetics can be as flighty as a pullet in a storm! Since we're still gathering data on these parents, my confidence is a bit low. These are just some likely patterns."
        }

        // 3. Highlight top positive trait
        val bestTrait = details.find { it.probabilityLabel.contains("Certain") || it.probabilityLabel.contains("Likely") }
        if (bestTrait != null) {
            return "Based on what I've seen, there's a strong potential for ${bestTrait.prediction.lowercase()} in this cross. Of course, environment and a bit of luck play their part too."
        }
        
        return "This pair shows some interesting potential. Take a look at the confidence ranges below to see how the traits might fall."
    }
    
    fun fromRecommendedPair(pair: RecommendedPair): BreedingExplanation {
        val details = pair.predictedTraits.map { trait ->
            ExplanationDetail(
                traitName = trait, 
                prediction = "Likely Present", 
                probabilityLabel = "Very Likely", 
                isInferred = false,
                confidenceLevel = ConfidenceLevel.MEDIUM
            )
        }
        
        val warnings = pair.warnings.map { warning ->
             ExplanationWarning(warning, WarningSeverity.CAUTION, WarningType.GENERAL)
        }

        return generateExplanation(
            pairId = "${pair.male.localId}_${pair.female.localId}",
            maleName = pair.male.breed.takeIf { it.isNotBlank() } ?: "Bird ${pair.male.localId}",
            femaleName = pair.female.breed.takeIf { it.isNotBlank() } ?: "Bird ${pair.female.localId}",
            details = details,
            warnings = warnings
        )
    }

    private fun generateTips(warnings: List<ExplanationWarning>): List<String> {
        val tips = mutableListOf<String>()
        if (warnings.any { it.type == WarningType.LOW_CONFIDENCE }) {
            tips.add("To help me get closer to the truth, try logging your hatch results. Every chick helps the model!")
        }
        if (warnings.any { it.type == WarningType.INBREEDING }) {
            tips.add("Remember, a healthy flock is a diverse one. Bringing in some fresh genetics might be the ethical choice here.")
        }
        tips.add("Keep your incubation parameters tight—even the best genetics can't overcome a cold spot in the hatcher.")
        return tips
    }

    fun translateProbability(probability: Double): String {
        return when {
            probability >= 0.9 -> "Almost Certain"
            probability >= 0.7 -> "Very Likely"
            probability >= 0.4 -> "About 50/50"
            probability >= 0.2 -> "Unlikely"
            else -> "Rare Surprise"
        }
    }
    
    fun getConfidenceMessage(level: ConfidenceLevel): String {
         return when (level) {
            ConfidenceLevel.FIXED -> "Official Standard (Very Reliable)"
            ConfidenceLevel.HIGH -> "Strong Evidence"
            ConfidenceLevel.MEDIUM -> "Inferred from Relatives"
            ConfidenceLevel.LOW -> "Best Guess (Needs Data)"
        }
    }
}
