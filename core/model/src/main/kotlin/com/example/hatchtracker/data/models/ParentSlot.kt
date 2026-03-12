package com.example.hatchtracker.data.models

import com.example.hatchtracker.data.models.TargetSex
import com.example.hatchtracker.data.models.TraitTarget
import com.example.hatchtracker.model.genetics.GenotypeCall

enum class ParentRole {
    SIRE, DAM
}

data class ParentSlot(
    val role: ParentRole,
    val source: String, // e.g. "BIRD:123", "BREED:Silkie", "SCENARIO:123:GEN:1"
    val displayName: String,
    val sex: TargetSex = if (role == ParentRole.SIRE) TargetSex.MALE else TargetSex.FEMALE,
    val birdId: String? = null,
    val requiredTraits: List<TraitTarget> = emptyList(),
    val preferredTraits: List<TraitTarget> = emptyList(),
    val excludedTraits: List<TraitTarget> = emptyList(),
    val requiredGenotypes: Map<String, GenotypeCall> = emptyMap(),
    val notes: List<String> = emptyList()
)
