package com.example.hatchtracker.domain.hatchy

import com.example.hatchtracker.domain.hatchy.routing.AnswerConfidence
import com.example.hatchtracker.domain.hatchy.routing.AnswerSource
import com.example.hatchtracker.domain.hatchy.routing.AnswerType
import com.example.hatchtracker.domain.hatchy.routing.HatchyAnswer
import com.example.hatchtracker.domain.hatchy.routing.HatchyContextSnapshot
import com.example.hatchtracker.domain.hatchy.routing.QueryInterpretation
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enforces the Hatchy persona and adapts the answer to the current context.
 */
@Singleton
class HatchyResponseComposer @Inject constructor() {

    fun compose(answer: HatchyAnswer, context: HatchyContextSnapshot, interpretation: QueryInterpretation): HatchyAnswer {
        var southernText = applyPersona(answer.text)
        
        if (interpretation.questionMode.primaryMode == com.example.hatchtracker.domain.hatchy.routing.QuestionMode.MIXED) {
            val module = interpretation.module ?: context.currentModule
            val hint = getAppHintForModule(module)
            if (hint != null) {
                southernText = "$southernText\n\n$hint"
            }
        }
        
        return answer.copy(text = southernText)
    }

    private fun applyPersona(text: String): String {
        // Seasoned breeder tone: expert, friendly, neutral.
        // No heavy dialect replacements. Just ensuring it starts with a friendly greeting or tone if appropriate.
        return text.replace("you are", "you're")
    }
    
    private fun getAppHintForModule(module: String): String? {
        return when (module.lowercase()) {
            "flock" -> "You can record this in the Flock module."
            "incubation" -> "You can track this batch in the Incubation module."
            "nursery" -> "You can update your chicks' status in the Nursery module."
            "finance" -> "You can log this expense in the Finance module."
            "equipment" -> "You can manage your devices in the Equipment module."
            else -> null
        }
    }
}
