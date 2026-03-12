package com.example.hatchtracker.data.mappers

import com.example.hatchtracker.data.models.EggProductionEntity
import com.example.hatchtracker.model.EggProduction

fun EggProductionEntity.toModel(): EggProduction {
    return EggProduction(
        id = id,
        flockId = flockId,
        lineId = lineId,
        dateEpochDay = dateEpochDay,
        totalEggs = totalEggs,
        crackedEggs = crackedEggs,
        setForIncubation = setForIncubation,
        soldEggs = soldEggs,
        notes = notes,
        cloudId = cloudId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deleted = deleted,
        syncState = syncState
    )
}

fun EggProduction.toEntity(): EggProductionEntity {
    return EggProductionEntity(
        id = id,
        flockId = flockId,
        lineId = lineId,
        dateEpochDay = dateEpochDay,
        totalEggs = totalEggs,
        crackedEggs = crackedEggs,
        setForIncubation = setForIncubation,
        soldEggs = soldEggs,
        notes = notes,
        cloudId = cloudId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deleted = deleted,
        syncState = syncState
    )
}
