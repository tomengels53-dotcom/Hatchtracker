package com.example.hatchtracker.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "breed_catalog",
    foreignKeys = [
        ForeignKey(
            entity = Species::class,
            parentColumns = ["id"],
            childColumns = ["speciesId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["speciesId"])]
)
data class Breed(
    @PrimaryKey
    val id: String,
    val speciesId: String,
    val name: String,
    val imageResId: Int? = null,
    val climateSuitability: String = "",
    val eggColor: String = "",
    val size: String = "",
    val weightClass: String = "",
    val eggProduction: String = "",
    val weightLbs: Double = 0.0,
    val weightKg: Double = 0.0,
    val heightInch: Double = 0.0,
    val heightCm: Double = 0.0,
    
    // Genetics Phase 2
    val breedType: String = "HERITAGE",
    val geneticTags: List<String> = emptyList(),
    val crossbreedingNotes: String? = null,

    val syncId: String = id,
    val lastUpdated: Long = System.currentTimeMillis(),
    val dataSource: String = "local"
)
