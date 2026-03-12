package com.example.hatchtracker.domain.hatchy

import com.example.hatchtracker.domain.hatchy.routing.AnswerConfidence
import com.example.hatchtracker.domain.hatchy.routing.AnswerType
import com.example.hatchtracker.domain.hatchy.routing.HatchyAnswer
import com.example.hatchtracker.domain.hatchy.routing.HatchyIntent
import com.example.hatchtracker.domain.hatchy.routing.HatchyIntentResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Prevents wrong answers from being shown with high confidence.
 */
@Singleton
class HatchyConfidenceEvaluator @Inject constructor() {

    fun evaluate(answer: HatchyAnswer, intentResult: HatchyIntentResult): HatchyAnswer {
        // 1. Intent match is too low
        if (intentResult.confidence < 0.2 && answer.confidence == AnswerConfidence.HIGH) {
            return answer.copy(confidence = AnswerConfidence.MEDIUM)
        }
        
        // 2. Fallback consistency
        if (answer.type == AnswerType.FALLBACK && answer.confidence == AnswerConfidence.HIGH) {
             return answer.copy(confidence = AnswerConfidence.LOW)
        }

        // 3. Health Security / Medical Safety
        if (intentResult.intent == HatchyIntent.POULTRY_HEALTH || answer.text.contains("vet") || answer.text.contains("professional")) {
            if (answer.confidence == AnswerConfidence.HIGH) {
                return answer.copy(confidence = AnswerConfidence.MEDIUM)
            }
        }

        // 4. Unknown breed detection in multi-entity intents
        if (intentResult.intent == HatchyIntent.CROSSBREED_OUTCOME && intentResult.entities.size < 2) {
            // If user asked to cross breeds but we only found one, the answer should mention the unknown breed.
            // This is handled by BreedingSimulationResolver mostly, but we can lower confidence here if not mentioned.
        }

        return answer
    }
}
