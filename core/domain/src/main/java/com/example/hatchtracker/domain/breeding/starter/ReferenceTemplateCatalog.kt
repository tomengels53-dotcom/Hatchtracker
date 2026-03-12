package com.example.hatchtracker.domain.breeding.starter

import com.example.hatchtracker.model.Species
import com.example.hatchtracker.model.genetics.GenotypeCall
import com.example.hatchtracker.model.genetics.Certainty

object ReferenceTemplateCatalog {

    val CHICKEN_TEMPLATES = listOf(
        ReferenceStarterTemplate(
            id = "starter_black_plumage",
            title = "Standard Black Stock",
            description = "High quality foundation stock for black-based plumage varieties.",
            species = Species.CHICKEN,
            fixedTraits = listOf("Extended Black", "Black Plumage"),
            genotypePriors = mapOf(
                "E" to GenotypeCall(locusId = "E", alleles = listOf("E", "E"), certainty = Certainty.CONFIRMED)
            ),
            rarity = "Common"
        ),
        ReferenceStarterTemplate(
            id = "starter_blue_green_eggs",
            title = "Colored Egg Foundation",
            description = "Stock carrying the Oocyan (blue egg) gene. Useful for Easter Egger programs.",
            species = Species.CHICKEN,
            fixedTraits = listOf("Blue Egg"),
            genotypePriors = mapOf(
                "O" to GenotypeCall(locusId = "O", alleles = listOf("O", "O"), certainty = Certainty.CONFIRMED)
            ),
            rarity = "Uncommon"
        ),
        ReferenceStarterTemplate(
            id = "starter_heavy_weight",
            title = "Heavy Utility Foundation",
            description = "Broad-bodied stock selected for size and frame.",
            species = Species.CHICKEN,
            fixedTraits = listOf("Heavy Weight"),
            genotypePriors = emptyMap(),
            rarity = "Common"
        )
    )

    fun getTemplatesForSpecies(species: Species): List<ReferenceStarterTemplate> {
        return when (species) {
            Species.CHICKEN -> CHICKEN_TEMPLATES
            else -> emptyList()
        }
    }
}

