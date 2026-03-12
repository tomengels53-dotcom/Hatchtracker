package com.example.hatchtracker.data.models

data class BreedingProgramStep(
    val order: Int,
    val generation: Int, // F1=1, F2=2...
    val title: String,
    val instruction: String,
    val requiredParents: List<ParentSlot> = emptyList(),
    val expectedOutcomes: List<String> = emptyList(),
    val riskWarnings: List<String> = emptyList(),
    val selectionGuidance: String? = null,
    val retentionCriteria: String? = null
)
