package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.models.Bird
import com.example.hatchtracker.data.models.Breed
import com.example.hatchtracker.data.models.Species

object DataRepository {
    val allSpecies: List<Species> = emptyList()
    val allBreeds: List<Breed> = emptyList()
    // Legacy compatibility list for older screens.
    val birdList: List<Bird> = emptyList()
}
