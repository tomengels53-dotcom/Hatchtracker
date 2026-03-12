package com.example.hatchtracker.domain.hatchy.routing.services

import com.example.hatchtracker.domain.hatchy.routing.PoultrySpecies

/**
 * Structured, deterministic knowledge base for core poultry domains.
 * Provides typed facts for knowledge services.
 */
object PoultryCoreKnowledge {

    val IncubationPeriods = mapOf(
        PoultrySpecies.CHICKEN to IncubationRecord(21, 18, 99.5, 45.0, 65.0),
        PoultrySpecies.DUCK to IncubationRecord(28, 25, 99.5, 55.0, 75.0),
        PoultrySpecies.GOOSE to IncubationRecord(30, 27, 99.5, 55.0, 75.0),
        PoultrySpecies.TURKEY to IncubationRecord(28, 25, 99.5, 50.0, 70.0),
        PoultrySpecies.QUAIL to IncubationRecord(18, 15, 99.5, 45.0, 65.0),
        PoultrySpecies.GUINEA_FOWL to IncubationRecord(27, 24, 99.5, 45.0, 65.0),
        PoultrySpecies.PEAFOWL to IncubationRecord(28, 25, 99.5, 50.0, 70.0)
    )

    // Baseline temperatures in Fahrenheit
    val BrooderTempsF = mapOf(
        1 to 95.0,
        2 to 90.0,
        3 to 85.0,
        4 to 80.0,
        5 to 75.0,
        6 to 70.0
    )

    val GeneticsFacts = mapOf(
        "F1" to "First generation cross. Usually shows hybrid vigor and uniform traits.",
        "F2" to "Second generation cross. Higher genetic variation; traits from original parent breeds may reappear randomly.",
        "Backcross" to "Crossing a hybrid back to one of its parent breeds to stabilize a specific trait.",
        "Sex-Linked" to "Traits carried on sex chromosomes, allowing for chick sexing at hatch (e.g., Black Star/Red Star crosses)."
    )

    val HealthBasics = listOf(
        HealthFact(
            symptom = "Soft Shell Eggs",
            causes = listOf("Calcium deficiency", "Heat stress", "Vitamin D3 deficiency", "Young layers"),
            action = "Add oyster shell supplement and ensure access to vitamin-enriched water."
        ),
        HealthFact(
            symptom = "Egg Binding",
            causes = listOf("Large egg size", "Calcium deficiency", "Obesity"),
            action = "EMERGENCY. Provide warm bath (Epsom salts), lubrication, and keep in a quiet, dark place. Seek vet if no progress."
        ),
        HealthFact(
            symptom = "Pasty Butt",
            causes = listOf("Temperature fluctuations in brooder", "Stress", "Improper diet"),
            action = "Gently clean with warm water and cloth. Ensure brooder temp is stable."
        )
    )

    data class IncubationRecord(
        val totalDays: Int,
        val lockdownDay: Int,
        val idealTempF: Double,
        val genericHumidity: Double,
        val lockdownHumidity: Double
    )

    data class HealthFact(
        val symptom: String,
        val causes: List<String>,
        val action: String
    )
}

