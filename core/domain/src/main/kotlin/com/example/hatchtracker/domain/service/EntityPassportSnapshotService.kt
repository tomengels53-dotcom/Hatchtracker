package com.example.hatchtracker.domain.service

import com.example.hatchtracker.domain.model.*
import com.example.hatchtracker.model.Bird
import com.example.hatchtracker.model.Flock

/**
 * Service to generate safe snapshots from domain entities.
 */
class EntityPassportSnapshotService {

    fun createBirdSnapshot(bird: Bird, context: ShareContext): EntityPassportSnapshot {
        val metadata = mutableMapOf<String, String>()
        metadata["Species"] = bird.species.name
        metadata["Breed"] = bird.breed
        metadata["Sex"] = bird.sex.name
        
        if (context == ShareContext.MARKETPLACE || context == ShareContext.SOCIAL_SHOWCASE) {
            metadata["Color"] = bird.color ?: "Standard"
        }
        
        if (context == ShareContext.MARKETPLACE) {
            metadata["Generation"] = "F${bird.generation}"
            metadata["Health Status"] = bird.status
        }

        return EntityPassportSnapshot(
            entityType = "BIRD",
            entityId = bird.syncId,
            title = bird.displayName,
            subtitle = "${bird.breed} ${bird.species.name}",
            heroImageUrl = bird.imagePath,
            displayMetadata = metadata
        )
    }

    fun createFlockSnapshot(flock: Flock, context: ShareContext): EntityPassportSnapshot {
        val metadata = mutableMapOf<String, String>()
        metadata["Species"] = flock.species.name
        metadata["Purpose"] = flock.purpose
        metadata["Bird Count"] = flock.breeds.size.toString()
        
        return EntityPassportSnapshot(
            entityType = "FLOCK",
            entityId = flock.syncId,
            title = flock.name,
            subtitle = "${flock.species.name} Flock",
            heroImageUrl = flock.imagePath,
            displayMetadata = metadata
        )
    }
}
