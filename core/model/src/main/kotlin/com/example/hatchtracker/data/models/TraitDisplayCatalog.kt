package com.example.hatchtracker.data.models

enum class TraitCategory {
    MENDELIAN,     // Engine-driven (B, O, etc.)
    QUANTITATIVE,  // Range-driven (Temperament, Size)
    PHYSICAL       // UI-only (e.g. specific note-like physical features)
}

data class GeneticTrait(
    val id: String,
    val label: String,
    val description: String,
    val category: TraitCategory,
    val options: List<TraitOption>
)

data class TraitOption(
    val id: String,
    val label: String,
    val colorHex: String? = null,
    val iconRes: Int? = null
)

object TraitDisplayCatalog {
    val traits = listOf(
        GeneticTrait(
            id = "egg_color",
            label = "Egg Color",
            description = "The shell color of eggs laid by hens of this line.",
            category = TraitCategory.MENDELIAN,
            options = listOf(
                TraitOption("white", "White", "#FFFFFF"),
                TraitOption("cream", "Cream/Tinted", "#F5F5DC"),
                TraitOption("brown", "Brown", "#A52A2A"),
                TraitOption("dark_brown", "Dark Brown (Chocolate)", "#3E2723"),
                TraitOption("blue", "Blue", "#87CEEB"),
                TraitOption("green", "Green", "#98FB98"),
                TraitOption("olive", "Olive", "#808000")
            )
        ),
        GeneticTrait(
            id = "size",
            label = "Bird Size",
            description = "The typical physical size and weight class.",
            category = TraitCategory.QUANTITATIVE,
            options = listOf(
                TraitOption("bantam", "Bantam (Small)"),
                TraitOption("standard", "Standard"),
                TraitOption("large_fowl", "Large Fowl"),
                TraitOption("giant", "Giant")
            )
        ),
        GeneticTrait(
            id = "temperament",
            label = "Temperament",
            description = "General behavioral disposition.",
            category = TraitCategory.QUANTITATIVE,
            options = listOf(
                TraitOption("docile", "Docile/Friendly"),
                TraitOption("active", "Active/Flighty"),
                TraitOption("aggressive", "Aggressive"),
                TraitOption("broody", "Broody (Good Mothers)")
            )
        ),
        GeneticTrait(
            id = "comb",
            label = "Comb Type",
            description = "Shape of the comb, relevant for climate tolerance.",
            category = TraitCategory.MENDELIAN,
            options = listOf(
                TraitOption("single", "Single"),
                TraitOption("pea", "Pea"),
                TraitOption("rose", "Rose"),
                TraitOption("walnut", "Walnut"),
                TraitOption("cushion", "Cushion")
            )
        ),
        GeneticTrait(
            id = "skin_color",
            label = "Skin/Shank Color",
            description = "Color of the skin and legs.",
            category = TraitCategory.MENDELIAN,
            options = listOf(
                TraitOption("yellow", "Yellow"),
                TraitOption("white", "White"),
                TraitOption("black", "Black (Fibromelanistic)"),
                TraitOption("slate", "Slate/Blue")
            )
        )
    )

    fun getTrait(id: String): GeneticTrait? = traits.find { it.id == id }
}
