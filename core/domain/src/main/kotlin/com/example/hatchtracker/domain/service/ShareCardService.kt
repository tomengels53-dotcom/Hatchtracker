package com.example.hatchtracker.domain.service

import com.example.hatchtracker.domain.model.ShareableCard
import com.example.hatchtracker.domain.model.CardStat
import com.example.hatchtracker.domain.model.CardType
import com.example.hatchtracker.model.Incubation
import com.example.hatchtracker.model.Bird
import com.example.hatchtracker.model.Flock

/**
 * Generates branded, social-ready share cards from real app data.
 */
class ShareCardService {

    fun generateIncubationCard(incubation: Incubation): ShareableCard {
        val hatchRate = if (incubation.eggsCount > 0) {
            (incubation.hatchedCount.toDouble() / incubation.eggsCount * 100).toInt()
        } else 0

        return ShareableCard(
            title = "Hatch Result",
            subtitle = "${incubation.species} Batch",
            stats = listOf(
                CardStat("Eggs Set", incubation.eggsCount.toString()),
                CardStat("Hatched", incubation.hatchedCount.toString(), isPrimary = true),
                CardStat("Hatch Rate", "$hatchRate%")
            ),
            imageUrl = null, // In real implementation, would resolve from equipment or first chick
            cardType = CardType.HATCH_RESULT
        )
    }

    fun generateBirdCard(bird: Bird): ShareableCard {
        return ShareableCard(
            title = bird.displayName,
            subtitle = bird.species.name,
            stats = listOf(
                CardStat("Breed", bird.breed, isPrimary = true),
                CardStat("Generation", "F${bird.generation}"),
                CardStat("Sex", bird.sex.name)
            ),
            imageUrl = bird.imagePath,
            cardType = CardType.BIRD_PASSPORT
        )
    }

    fun generateFlockCard(flock: Flock): ShareableCard {
        return ShareableCard(
            title = flock.name,
            subtitle = "${flock.species} Flock",
            stats = listOf(
                CardStat("Birds", flock.eggCount.toString(), isPrimary = true), // Simplified
                CardStat("Status", if (flock.active) "Active" else "Inactive")
            ),
            imageUrl = flock.imagePath,
            cardType = CardType.FLOCK_SUMMARY
        )
    }
}
