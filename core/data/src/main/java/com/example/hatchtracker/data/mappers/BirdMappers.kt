package com.example.hatchtracker.data.mappers

import com.example.hatchtracker.data.models.BirdEntity
import com.example.hatchtracker.model.Bird
import com.example.hatchtracker.model.Species

fun BirdEntity.toModel(): Bird {
    return Bird(
        localId = id,
        syncId = syncId,
        flockId = flockId,
        species = try { Species.valueOf(species.uppercase()) } catch (e: Exception) { Species.UNKNOWN },
        breed = breed,
        breedId = breedId,
        sex = sex,
        hatchDate = hatchDate,
        generation = generation,
        motherId = motherId,
        fatherId = fatherId,
        incubationId = incubationId,
        hatchBatchId = hatchBatchId,
        color = color,
        notes = notes,
        status = status,
        lifecycleStage = lifecycleStage,
        lastUpdated = lastUpdated,
        imagePath = imagePath,
        geneticProfile = geneticProfile,
        ringNumber = ringNumber,
        ownerUserId = ownerUserId,
        cloudId = cloudId,
        serverUpdatedAt = serverUpdatedAt,
        localUpdatedAt = localUpdatedAt,
        deleted = deleted,
        syncState = syncState,
        costBasisCents = costBasisCents,
        costBasisSourceRef = costBasisSourceRef
    )
}

fun Bird.toEntity(): BirdEntity {
    return BirdEntity(
        id = localId,
        syncId = syncId,
        flockId = flockId,
        species = species.name,
        breed = breed,
        breedId = breedId,
        sex = sex,
        hatchDate = hatchDate,
        generation = generation,
        motherId = motherId,
        fatherId = fatherId,
        incubationId = incubationId,
        hatchBatchId = hatchBatchId,
        color = color,
        notes = notes,
        status = status,
        lifecycleStage = lifecycleStage,
        lastUpdated = lastUpdated,
        imagePath = imagePath,
        geneticProfile = geneticProfile,
        ringNumber = ringNumber,
        ownerUserId = ownerUserId,
        cloudId = cloudId,
        serverUpdatedAt = serverUpdatedAt,
        localUpdatedAt = localUpdatedAt,
        deleted = deleted,
        syncState = syncState,
        costBasisCents = costBasisCents,
        costBasisSourceRef = costBasisSourceRef
    )
}
