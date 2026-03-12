package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.model.*


import javax.inject.Inject
import javax.inject.Singleton

data class IntentBundle(
    val intent: HatchyIntent,
    val keywords: List<String>
)

/**
 * Hardening: Localized keyword bundles for intent classification.
 */
@Singleton
class KeywordBundleRepository @Inject constructor() {

    private val bundles = mapOf(
        "en" to listOf(
            IntentBundle(HatchyIntent.PAYWALL_BYPASS_ATTEMPT, listOf("unlock for free", "bypass", "crack", "skip paywall", "free pro", "mod apk", "patched", "refund trick", "hack")),
            IntentBundle(HatchyIntent.APP_NAVIGATION, listOf("where do i", "how do i", "can't find", "screen", "button", "navigate", "menu", "find", "planner", "calendar", "where can i", "where is", "how to", "add a", "select a")),
            IntentBundle(HatchyIntent.BREED_INFO, listOf("breed info", "about breed", "characteristics", "origin", "eggs per year", "egg color", "comb type", "plumage color", "weight", "standard", "about", "tell me about", "what is a")),
            IntentBundle(HatchyIntent.BREED_COMPARISON, listOf("compare", "versus", "vs", "which is better", "best layer", "best meat", "dual purpose", "better for")),
            IntentBundle(HatchyIntent.CROSSBREED_OUTCOME, listOf("cross", "crossbreed", "hybrid", "mix", "breed with", "offspring", "what happens if", "generation")),
            IntentBundle(HatchyIntent.BREEDING_GUIDANCE, listOf("how to breed", "starting breeding", "breeding program", "selection", "line breeding", "backcross", "inbreeding", "genetic stability", "how do i start breeding", "how to start a breeding", "breeding advice")),
            IntentBundle(HatchyIntent.INCUBATION_GUIDANCE, listOf("incubation length", "how long to hatch", "humidity for", "temperature for", "candling", "turning eggs", "lockdown", "hatch day", "how to incubate", "how do i start incubating", "incubation advice")),
            IntentBundle(HatchyIntent.INCUBATION_STATUS, listOf("closest to hatch", "due this week", "incubation status", "current hatch", "progress")),
            IntentBundle(HatchyIntent.NURSERY_GUIDANCE, listOf("chick care", "brooder temp", "feeding chicks", "nursery care", "first week")),
            IntentBundle(HatchyIntent.NURSERY_STATUS, listOf("chicks in nursery", "active batch", "nursery count", "flocklet status")),
            IntentBundle(HatchyIntent.FINANCE_HELP, listOf("how to record expense", "log sale", "finance help", "accounting")),
            IntentBundle(HatchyIntent.FINANCE_SUMMARY, listOf("spend", "profit", "loss", "money spent", "financial summary", "top cost")),
            IntentBundle(HatchyIntent.EQUIPMENT_HELP, listOf("setup device", "connect sensor", "equipment help", "using incubator")),
            IntentBundle(HatchyIntent.EQUIPMENT_STATUS, listOf("device status", "sensor data", "battery", "offline")),
            IntentBundle(HatchyIntent.GENERAL_POULTRY, listOf("poultry care", "behavior", "pecking", "feeding", "housing", "general care")),
            IntentBundle(HatchyIntent.POULTRY_HEALTH, listOf("sick", "disease", "mites", "not laying", "mortality", "health issue", "symptoms")),
            IntentBundle(HatchyIntent.USER_DATA_QUERY, listOf("my flock", "my birds", "my business", "my data", "count", "total")),
            IntentBundle(HatchyIntent.BILLING_SUBSCRIPTION, listOf("paywall", "subscription", "pro", "billing", "purchase", "upgrade", "tier", "cost", "price")),
            IntentBundle(HatchyIntent.TROUBLESHOOTING, listOf("problem", "issue", "not working", "fail", "late", "early", "stuck", "trouble", "why", "notification"))
        ),
        "nl" to listOf(
            IntentBundle(HatchyIntent.PAYWALL_BYPASS_ATTEMPT, listOf("gratis ontgrendelen", "omzeilen", "kraken", "betaalmuur overslaan", "gratis pro", "gepatcht", "hack")),
            IntentBundle(HatchyIntent.APP_NAVIGATION, listOf("waar kan ik", "hoe moet ik", "kan niet vinden", "scherm", "knop", "navigeren", "menu", "vinden", "planner", "kalender", "hoe voeg ik toe")),
            IntentBundle(HatchyIntent.BREED_INFO, listOf("rasinformatie", "over ras", "kenmerken", "herkomst", "eieren per jaar", "eierkleur", "kam type", "gewicht")),
            IntentBundle(HatchyIntent.CROSSBREED_OUTCOME, listOf("kruisen", "kruising", "hybride", "mix", "fokken met", "nakomelingen", "wat gebeurt er als")),
            IntentBundle(HatchyIntent.INCUBATION_GUIDANCE, listOf("broedduur", "hoe lang broeden", "vochtigheid", "temperatuur", "schouwen", "draaien", "lockdown")),
            IntentBundle(HatchyIntent.FINANCE_SUMMARY, listOf("uitgegeven", "winst", "verlies", "geld", "financieel overzicht")),
            IntentBundle(HatchyIntent.POULTRY_HEALTH, listOf("ziek", "ziekte", "mijten", "leggen niet", "sterfte", "gezondheid", "symptomen"))
            // (Full Dutch translations can be added later, focusing on core logic now)
        )
    )

    fun getBundlesFor(locale: String): List<IntentBundle> {
        val lang = locale.split("-").first().lowercase()
        return bundles[lang] ?: bundles["en"]!!
    }
}
