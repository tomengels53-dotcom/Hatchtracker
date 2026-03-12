package com.example.hatchtracker.domain.hatchy.routing

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hatchy Intent Classification Hardening
 * Uses localized bundles and scoring-based matching.
 */
@Singleton
class HatchyIntentClassifier @Inject constructor(
    private val bundleRepository: KeywordBundleRepository
) {

    fun classify(query: String, locale: String): HatchyIntentResult {
        val q = normalize(query)
        val bundles = bundleRepository.getBundlesFor(locale)

        // 1. Paywall Bypass Check (Static Priority)
        val bypassBundle = bundles.find { it.intent == HatchyIntent.PAYWALL_BYPASS_ATTEMPT }
        bypassBundle?.keywords?.forEach { k ->
            if (q.contains(k)) {
                return HatchyIntentResult(
                    intent = HatchyIntent.PAYWALL_BYPASS_ATTEMPT,
                    confidence = 1.0,
                    matchedKeywords = listOf(k),
                    bypassScore = 10
                )
            }
        }

        // 2. Score-based matching for other intents
        val scores = mutableMapOf<HatchyIntent, Double>()
        val matches = mutableMapOf<HatchyIntent, MutableList<String>>()

        bundles.forEach { bundle ->
            if (bundle.intent == HatchyIntent.PAYWALL_BYPASS_ATTEMPT) return@forEach
            
            var totalScore = 0.0

            // Adjust scoring based on specific intent and keywords
            if (bundle.intent == HatchyIntent.BREED_INFO) {
                // Increase BREED_INFO score for explicit breed-related keywords
                val breedKeywords = listOf("breed", "rassen", "ras", "soort", "type") // Example keywords
                if (breedKeywords.any { q.contains(it) }) totalScore += 0.7
                val infoKeywords = listOf("info", "informatie", "details", "over", "about", "tell me", "what is") // Example keywords
                if (infoKeywords.any { q.contains(it) }) totalScore += 0.4
            } else if (bundle.intent == HatchyIntent.BREEDING_GUIDANCE || bundle.intent == HatchyIntent.INCUBATION_GUIDANCE || bundle.intent == HatchyIntent.NURSERY_GUIDANCE) {
                val adviceKeywords = listOf("how to", "how do i", "start", "begin", "advice", "guide")
                if (adviceKeywords.any { q.contains(it) }) totalScore += 0.5
            } else if (bundle.intent.name.endsWith("_STATUS") || bundle.intent.name.endsWith("_SUMMARY")) {
                val statusKeywords = listOf("status", "count", "how many", "show me", "summary", "total")
                if (statusKeywords.any { q.contains(it) }) totalScore += 0.5
            }

            bundle.keywords.forEach { keyword ->
                val normalizedK = normalize(keyword)
                if (q.contains(normalizedK)) {
                    // Exact word boundary matches get higher weight
                    val weight = if (hasWordBoundaryMatch(q, normalizedK)) 1.0 else 0.5
                    totalScore += weight
                    matches.getOrPut(bundle.intent) { mutableListOf() }.add(keyword)
                }
            }
            if (totalScore > 0) {
                scores[bundle.intent] = totalScore
            }
        }

        val topIntent = scores.maxByOrNull { it.value }
        
        return if (topIntent != null) {
            val intent = topIntent.key
            val module = if (intent == HatchyIntent.APP_NAVIGATION) detectModule(q) else null
            HatchyIntentResult(
                intent = intent,
                module = module,
                confidence = topIntent.value,
                matchedKeywords = matches[intent] ?: emptyList()
            )
        } else {
            HatchyIntentResult(HatchyIntent.OTHER)
        }
    }

    private fun normalize(input: String): String {
        return input.lowercase()
            .trim()
            .replace("\\s+".toRegex(), " ")
            // Simple diacritic removal could be added here if needed
    }

    private fun hasWordBoundaryMatch(query: String, keyword: String): Boolean {
        val pattern = "\\b${Regex.escape(keyword)}\\b".toRegex()
        return pattern.containsMatchIn(query)
    }

    private fun detectModule(query: String): String? {
        // Module detection also benefits from normalization
        return when {
            query.contains("flock") || query.contains("koppel") -> "flock"
            query.contains("incubat") || query.contains("hatch") || query.contains("uitkomst") -> "incubation"
            query.contains("nursery") || query.contains("brooder") || query.contains("opfok") || query.contains("kuiken") -> "nursery"
            query.contains("finance") || query.contains("money") || query.contains("sale") || query.contains("geld") || query.contains("verkoop") -> "finance"
            query.contains("breeding") || query.contains("fokken") -> "breeding"
            query.contains("support") || query.contains("help") -> "support"
            query.contains("admin") -> "admin"
            else -> null
        }
    }
}
