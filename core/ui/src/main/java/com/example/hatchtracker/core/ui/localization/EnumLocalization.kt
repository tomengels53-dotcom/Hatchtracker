package com.example.hatchtracker.core.ui.localization

import com.example.hatchtracker.core.ui.R
import com.example.hatchtracker.model.Species
import com.example.hatchtracker.model.Sex
import com.example.hatchtracker.model.BreedingGoalType

/**
 * EnumLocalization: Centralized mapping for enum display names.
 * Ensures we don't leak .name.replace("_", " ") into the UI.
 */

fun Species.displayNameRes(): Int = when (this) {
    Species.CHICKEN -> R.string.species_chicken
    Species.DUCK -> R.string.species_duck
    Species.TURKEY -> R.string.species_turkey
    Species.QUAIL -> R.string.species_quail
    Species.GOOSE -> R.string.species_goose
    Species.PEAFOWL -> R.string.species_peafowl
    Species.PHEASANT -> R.string.species_pheasant
    Species.UNKNOWN -> R.string.species_unknown
}

fun Sex.displayNameRes(): Int = when (this) {
    Sex.MALE -> R.string.sex_male
    Sex.FEMALE -> R.string.sex_female
    Sex.UNKNOWN -> R.string.sex_unknown
}
fun BreedingGoalType.displayNameRes(): Int = when (this) {
    BreedingGoalType.SIZE -> com.example.hatchtracker.core.common.R.string.breeding_goal_size
    BreedingGoalType.EGG_COLOR -> com.example.hatchtracker.core.common.R.string.breeding_goal_egg_color
    BreedingGoalType.TEMPERAMENT -> com.example.hatchtracker.core.common.R.string.breeding_goal_temperament
    BreedingGoalType.GENETIC_DIVERSITY -> com.example.hatchtracker.core.common.R.string.breeding_goal_genetic_diversity
    BreedingGoalType.BREED_STABILIZATION -> com.example.hatchtracker.core.common.R.string.breeding_goal_breed_stabilization
    BreedingGoalType.SEX_LINKED_SORTING -> com.example.hatchtracker.core.common.R.string.breeding_goal_sex_linked_sorting
    BreedingGoalType.EXOTIC_FEATURES -> com.example.hatchtracker.core.common.R.string.breeding_goal_exotic_features
}
