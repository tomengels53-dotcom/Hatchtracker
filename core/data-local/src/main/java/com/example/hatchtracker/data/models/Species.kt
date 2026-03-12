package com.example.hatchtracker.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "species_catalog")
data class Species(
    @PrimaryKey
    val id: String,
    val name: String,
    val iconResId: Int? = null,
    val localName: String = "",
    val defaultIncubationDays: Int = 21,
    val hatchWindowDays: Int = 2,
    val incubationTempC: Double = 37.5,
    val incubationHumidityStart: Int = 45,
    val incubationHumidityLockdown: Int = 65,
    val turningUntilDay: Int = 18,
    val sourceLabel: String = "system",
    val speciesOrder: Int = 0,
    val isActive: Boolean = true,
    val syncId: String = id,
    val lastUpdated: Long = System.currentTimeMillis(),
    val dataSource: String = "local"
)
