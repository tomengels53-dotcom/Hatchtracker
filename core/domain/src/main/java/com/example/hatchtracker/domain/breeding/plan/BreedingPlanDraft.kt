package com.example.hatchtracker.domain.breeding.plan

import com.example.hatchtracker.data.models.BreedingProgramStep
import com.example.hatchtracker.data.models.BreedingTarget
import com.example.hatchtracker.model.Species

data class BreedingPlanDraft(
    val id: String,
    val planType: ProgramMode,
    val species: Species,
    val goal: BreedingTarget,
    val summaryRationale: String,
    val steps: List<BreedingProgramStep>
)

