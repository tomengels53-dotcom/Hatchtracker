package com.example.hatchtracker.domain.breeding

data class BreedingProgram(
    val id: String,
    val sireId: String,
    val damId: String,
    val overallScore: Double,
    val summaryRationale: String,
    val pathway: List<ProgramGeneration>
)

data class ProgramGeneration(
    val generationNumber: Int,
    val milestoneGoal: String,
    val maleSource: String,
    val femaleSource: String,
    val sireId: String? = null,
    val damId: String? = null,
    val isVirtual: Boolean = false,
    val virtualSyncId: String? = null,
    val expectedTraitGains: List<String> = emptyList(),
    val selectionGuidance: String? = null,
    val retentionCriteria: String? = null,
    val stability: LineStabilitySnapshot? = null,
    val candidateKeepers: List<Map<String, com.example.hatchtracker.model.genetics.GenotypeCall>> = emptyList()
)
