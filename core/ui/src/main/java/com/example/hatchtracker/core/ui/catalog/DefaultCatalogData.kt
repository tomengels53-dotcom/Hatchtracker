package com.example.hatchtracker.core.ui.catalog

import com.example.hatchtracker.core.ui.R

data class SpeciesOption(
    val id: String,
    val name: String,
    val imageResId: Int? = null
)

object DefaultCatalogData {
    val allSpecies: List<SpeciesOption> = listOf(
        SpeciesOption(id = "chicken", name = "Chicken", imageResId = R.drawable.species_chicken),
        SpeciesOption(id = "duck", name = "Duck", imageResId = R.drawable.species_duck),
        SpeciesOption(id = "goose", name = "Goose", imageResId = R.drawable.species_goose),
        SpeciesOption(id = "turkey", name = "Turkey", imageResId = R.drawable.species_turkey),
        SpeciesOption(id = "peafowl", name = "Peafowl", imageResId = R.drawable.species_peacock),
        SpeciesOption(id = "pheasant", name = "Pheasant", imageResId = R.drawable.species_pheasant),
        SpeciesOption(id = "quail", name = "Quail", imageResId = R.drawable.species_quail)
    )
}
