package com.example.hatchtracker.domain.breeding.starter

import com.example.hatchtracker.data.models.TraitTarget
import com.example.hatchtracker.model.genetics.GenotypeCall

data class ReferenceStarterTemplate(
    val id: String,
    val title: String,
    val description: String,
    val species: com.example.hatchtracker.model.Species,
    val fixedTraits: List<String>,
    val genotypePriors: Map<String, GenotypeCall>,
    val estimatedPrice: Double? = null,
    val rarity: String = "Common"
)

