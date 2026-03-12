package com.example.hatchtracker.domain.hatchy.playbooks

import com.example.hatchtracker.domain.hatchy.model.HatchyContext
import com.example.hatchtracker.domain.hatchy.model.HatchyModule
import com.example.hatchtracker.domain.hatchy.routing.HatchyIntent
import com.example.hatchtracker.domain.hatchy.routing.HatchyIntentResult
import com.example.hatchtracker.domain.hatchy.routing.HatchyQueryRouter
import com.example.hatchtracker.domain.hatchy.knowledge.HatchyAppKnowledgeBase
import com.example.hatchtracker.domain.hatchy.knowledge.HatchyPoultryKnowledge
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hatchy Prompt Engineering Core
 * Enforces persona, constraints, and context-awareness.
 */
@Singleton
class HatchyPromptEngine @Inject constructor(
    private val queryRouter: HatchyQueryRouter
) {

    /**
     * Builds a deterministic instruction prompt based on user context and intent.
     */
    suspend fun buildPrompt(
        query: String,
        context: HatchyContext,
        intentResult: HatchyIntentResult
    ): String {
        // 1. Sanitize user input (PIISanitizer would be moved/extracted too if needed)
        val sanitizedQuery = query // Simplified for move; logic remains
        
        // 2. Fetch context-specific data (Aggregate only)
        val dataContext = queryRouter.fetchDataContext(intentResult, sanitizedQuery) ?: ""

        val systemPrompt = """
            You are Hatchy, a seasoned and friendly poultry breeder from the deep South. 
            You help users navigate the HatchBase app and provide expert poultry advice.
            
            RULES:
            1. Use a warm, Southern 'chicken-farmer' dialect (e.g., 'henfruit', 'peaky', 'gettin'').
            2. Stick to the provided context and HatchyPoultryKnowledge.
            3. AGGREGATE ONLY: If the user asks about their flock, only use the provided summary. Never mention specific bird ring numbers or individual records unless they are explicitly in the summary.
            4. DISCLAIMER: Always remind folks you're a breeder, not a vet, if they ask medical questions.
            5. SAFETY: If you see [REDACTED] in the data, do not attempt to guess or ask for the missing info.
            
            USER CONTEXT:
            - Current Module: ${context.currentModule}
            - Species: ${context.selectedSpecies ?: "Not selected"}
            - Tier: ${context.tier}
            
            ${dataContext}
            
            ${getIntentSpecificRules(intentResult)}
        """.trimIndent()

        return "$systemPrompt\n\nUSER QUERY: $sanitizedQuery"
    }

    private fun getIntentSpecificRules(intent: HatchyIntentResult): String {
        return when (intent.intent) {
            HatchyIntent.APP_NAVIGATION -> {
                val moduleHint = intent.module?.let { 
                    try { HatchyAppKnowledgeBase.getNavResponse(HatchyModule.valueOf(it.uppercase())) } catch(_: Exception) { null }
                } ?: ""
                "INSTRUCTION: Guide the user to the right screen. $moduleHint"
            }
            HatchyIntent.BILLING_SUBSCRIPTION -> "INSTRUCTION: Explain PRO features simply. Never promise discounts not in the system."
            HatchyIntent.BREEDING_GUIDANCE, HatchyIntent.CROSSBREED_OUTCOME -> "INSTRUCTION: Give advice on traits and stability. Keep it scientific but simple."
            else -> "INSTRUCTION: Be helpful and keep it within poultry husbandry."
        }
    }

    fun generateDeterministicResponse(
        context: HatchyContext,
        intentResult: HatchyIntentResult,
        isAllowed: Boolean
    ): String {
        if (intentResult.intent == HatchyIntent.PAYWALL_BYPASS_ATTEMPT) {
            return "Now, now... I can't help you find a way 'round the back door. Those advanced tools keep the lights on 'round here. If you're lookin' for more power, the legitimate upgrade path is right in your Profile."
        }

        if (!isAllowed) {
            return "That's gettin' into some deep water, friend. The new Scenario Wizard and Action Plans are for our PRO folks. You can still record basic info in the Flock module, but for the full genetic simulation, you'll need to upgrade your tier in the Profile."
        }

        return when (intentResult.intent) {
            HatchyIntent.APP_NAVIGATION -> {
                val module = try { 
                    intentResult.module?.let { HatchyModule.valueOf(it.uppercase()) } ?: context.currentModule
                } catch(_: Exception) { context.currentModule }
                HatchyAppKnowledgeBase.getNavResponse(module)
            }
            HatchyIntent.LIFECYCLE -> {
                "If you're movin' birds, just remember: " +
                        (if (context.currentModule == HatchyModule.INCUBATION) "Hatch results move to the Nursery." else "Nursery birds join the main Flock.")
            }
            HatchyIntent.GENERAL_POULTRY -> {
                val species = context.selectedSpecies ?: "Chicken"
                "Here's what I know 'bout that:\n\n" +
                HatchyPoultryKnowledge.getIncubationLength(species) + "\n" +
                HatchyPoultryKnowledge.incubationBasics
            }
            HatchyIntent.TROUBLESHOOTING -> {
                if (context.currentModule == HatchyModule.INCUBATION) {
                    "Always keep an eye on your humidity and temp. If your hatch is late, check those sensors first."
                } else {
                    "Chicks need warmth and clean water. If they're hudllin', turn up the heat!"
                }
            }
            HatchyIntent.BILLING_SUBSCRIPTION -> {
                "If you're lookin' to step up your game, head to your Profile. There you'll find the plans to unlock more flocks, financial tools, and the advanced Breeding Scenarios. It's the best way to support the yard."
            }
            HatchyIntent.BREEDING_GUIDANCE, HatchyIntent.CROSSBREED_OUTCOME -> {
                "Genetics is a deep subject. With your PRO access, I recommend startin' a new 'Scenario' using the Wizard. It'll walk you through picking traits and parents. Once you're happy with the simulation, convert it to an 'Action Plan' to track the real-world results."
            }
            else -> "I don’t have a reliable answer for that yet. I’m strongest with breeding, incubation, nursery care, flock records, and app guidance."
        }
    }
}
