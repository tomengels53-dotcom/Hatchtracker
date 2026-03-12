package com.example.hatchtracker.core.navigation

import androidx.navigation.NavController

/**
 * Data class representing the result of a breed selection.
 * Pure Kotlin data class.
 */
data class BreedSelectionResult(
    val speciesId: String,
    val speciesName: String,
    val breedId: String,
    val breedName: String
)

/**
 * Shared contract for passing breed selection results between screens.
 * Uses individual keys in SavedStateHandle to avoid Parcelable dependency.
 */
object BreedSelectionContract {
    const val KEY_SPECIES_ID = "breed_res_species_id"
    const val KEY_SPECIES_NAME = "breed_res_species_name"
    const val KEY_BREED_ID = "breed_res_breed_id"
    const val KEY_BREED_NAME = "breed_res_breed_name"

    /**
     * Helper to set the selection result on the previous back stack entry.
     */
    fun setSelectionResult(navController: NavController, result: BreedSelectionResult) {
        navController.previousBackStackEntry?.savedStateHandle?.let { handle ->
            handle[KEY_SPECIES_ID] = result.speciesId
            handle[KEY_SPECIES_NAME] = result.speciesName
            handle[KEY_BREED_ID] = result.breedId
            handle[KEY_BREED_NAME] = result.breedName
        }
    }

    /**
     * Helper to consume the selection result from the current back stack entry.
     * Reads and removes the keys from SavedStateHandle.
     */
    fun consumeSelectionResult(navController: NavController): BreedSelectionResult? {
        val handle = navController.currentBackStackEntry?.savedStateHandle ?: return null

        if (!handle.contains(KEY_BREED_ID)) return null

        val result = BreedSelectionResult(
            speciesId = handle.get<String>(KEY_SPECIES_ID) ?: "",
            speciesName = handle.get<String>(KEY_SPECIES_NAME) ?: "",
            breedId = handle.get<String>(KEY_BREED_ID) ?: "",
            breedName = handle.get<String>(KEY_BREED_NAME) ?: ""
        )

        // Consume (remove) the data
        handle.remove<String>(KEY_SPECIES_ID)
        handle.remove<String>(KEY_SPECIES_NAME)
        handle.remove<String>(KEY_BREED_ID)
        handle.remove<String>(KEY_BREED_NAME)

        return result
    }
}
