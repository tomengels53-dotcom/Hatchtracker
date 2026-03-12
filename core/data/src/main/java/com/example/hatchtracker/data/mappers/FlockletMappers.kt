package com.example.hatchtracker.data.mappers

import com.example.hatchtracker.data.models.FlockletEntity
import com.example.hatchtracker.model.Flocklet

fun FlockletEntity.toModel(): Flocklet {
    return Flocklet(
        id = id,
        hatchId = hatchId,
        species = species,
        breeds = breeds,
        hatchDate = hatchDate,
        chickCount = chickCount,
        currentTemp = currentTemp,
        targetTemp = targetTemp,
        ageInDays = ageInDays,
        weightAvg = weightAvg,
        healthStatus = healthStatus,
        notes = notes,
        readyForFlock = readyForFlock,
        movedToFlockId = movedToFlockId,
        syncId = syncId,
        lifecycleStage = lifecycleStage,
        lastUpdated = lastUpdated,
        costBasisCents = costBasisCents,
        costBasisSourceRef = costBasisSourceRef,
        costBasisSchemaVersion = costBasisSchemaVersion
    )
}

fun Flocklet.toEntity(): FlockletEntity {
    return FlockletEntity(
        id = id,
        hatchId = hatchId,
        species = species,
        breeds = breeds,
        hatchDate = hatchDate,
        chickCount = chickCount,
        currentTemp = currentTemp,
        targetTemp = targetTemp,
        ageInDays = ageInDays,
        weightAvg = weightAvg,
        healthStatus = healthStatus,
        notes = notes,
        readyForFlock = readyForFlock,
        movedToFlockId = movedToFlockId,
        syncId = syncId,
        lifecycleStage = lifecycleStage,
        lastUpdated = lastUpdated,
        costBasisCents = costBasisCents,
        costBasisSourceRef = costBasisSourceRef,
        costBasisSchemaVersion = costBasisSchemaVersion
    )
}
