package com.example.hatchtracker.domain.hatchy.knowledge

/**
 * Deterministic poultry husbandry and incubation facts.
 */
object HatchyPoultryKnowledge {

    val incubationDaysBySpecies = mapOf(
        "Chicken" to 21,
        "Duck (Domestic)" to 28,
        "Muscovy Duck" to 35,
        "Quail (Coturnix)" to 17,
        "Turkey" to 28,
        "Goose" to 30,
        "Guinea Fowl" to 26,
        "Pheasant" to 24
    )

    val incubationBasics = """
        🌡️ Temperature: Stay within 99.5°F (Forced air) or 101.5°F (Still air).
        💧 Humidity: 45-50% for incubation; increase to 65-70% for lockdown.
        🔄 Turning: 3-5 times a day until lockdown.
        🛑 Lockdown: Stop turning 3 days before hatch; keep incubator closed!
    """.trimIndent()

    val brooderGuide = """
        Week 1: 95°F (35°C)
        Week 2: 90°F (32°C)
        Week 3: 85°F (29°C)
        Week 4: 80°F (26°C)
        Week 5: 75°F (24°C)
        Week 6: Room temp or 70°F (21°C)
    """.trimIndent()

    val chickBehavior = """
        ❄️ Huddling under heat: Too cold.
        🔥 Huddled in corners/panting: Too hot.
        🐣 Spread evenly and peeping: Just right.
    """.trimIndent()

    val MEDICAL_DISCLAIMER = "I'm a breeder, not a vet, friend. I can't diagnose illness or prescribe medicine. If your bird is lookin' peaky, off its feed, or has trouble breathin', you should consult a qualified avian veterinarian immediately. Clean water and isolation are the only general tips I can give."

    fun getIncubationLength(species: String): String {
        val days = incubationDaysBySpecies[species] ?: return "Most small fowl take between 17 and 30 days. Tell me the specific bird and I'll give you the count."
        return "$species eggs typically hatch in $days days."
    }
}
