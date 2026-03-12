package com.example.hatchtracker.data.mappers

import com.example.hatchtracker.data.models.FlockEntity
import com.example.hatchtracker.model.Flock
import com.example.hatchtracker.model.Species

fun FlockEntity.toModel(): Flock {
    return Flock(
        localId = id,
        syncId = syncId,
        species = try { Species.valueOf(species.uppercase()) } catch (e: Exception) { Species.UNKNOWN },
        breeds = breeds,
        name = name,
        purpose = purpose,
        active = active,
        createdAt = createdAt,
        notes = notes,
        eggCount = eggCount,
        lastUpdated = lastUpdated,
        imagePath = imagePath,
        defaultGeneticProfile = defaultGeneticProfile,
        ownerUserId = ownerUserId,
        cloudId = cloudId,
        serverUpdatedAt = serverUpdatedAt,
        localUpdatedAt = localUpdatedAt,
        deleted = deleted,
        syncState = syncState
    )
}

fun Flock.toEntity(): FlockEntity {
    return FlockEntity(
        id = localId,
        syncId = syncId,
        species = species.name,
        breeds = breeds,
        name = name,
        purpose = purpose,
        active = active,
        createdAt = createdAt,
        notes = notes,
        eggCount = eggCount,
        lastUpdated = lastUpdated,
        imagePath = imagePath,
        defaultGeneticProfile = defaultGeneticProfile,
        ownerUserId = ownerUserId,
        cloudId = cloudId,
        serverUpdatedAt = serverUpdatedAt,
        localUpdatedAt = localUpdatedAt,
        deleted = deleted,
        syncState = syncState
    )
}
