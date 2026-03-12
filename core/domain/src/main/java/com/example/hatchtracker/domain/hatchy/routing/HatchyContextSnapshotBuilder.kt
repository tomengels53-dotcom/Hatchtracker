package com.example.hatchtracker.domain.hatchy.routing

import com.example.hatchtracker.domain.hatchy.model.HatchyContext
import com.example.hatchtracker.data.models.SubscriptionTier
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds a lightweight structured context from current state and conversation history.
 */
@Singleton
class HatchyContextSnapshotBuilder @Inject constructor() {

    private var sessionHistory = mutableListOf<HatchyIntentResult>()

    fun build(
        appContext: HatchyContext,
        currentIntent: HatchyIntentResult
    ): HatchyContextSnapshot {
        // Add to session history
        sessionHistory.add(currentIntent)
        if (sessionHistory.size > 5) sessionHistory.removeAt(0)

        val recentBreeds = sessionHistory.flatMap { it.entities }
            .filter { it.type == EntityType.BREED }
            .map { it.value }
            .distinct()

        val recentTraits = sessionHistory.flatMap { it.entities }
            .filter { it.type == EntityType.TRAIT }
            .map { it.value }
            .distinct()

        val lastResult = if (sessionHistory.size >= 2) sessionHistory[sessionHistory.size - 2] else null

        val lastSpecies = sessionHistory.flatMap { it.entities }
            .find { it.type == EntityType.POULTRY_SPECIES }
            ?.value?.let { runCatching { PoultrySpecies.valueOf(it) }.getOrNull() }

        val breeds = sessionHistory.flatMap { it.entities }
            .filter { it.type == EntityType.BREED }
            .map { it.value }
        val lastBreedPair = if (breeds.size >= 2) Pair(breeds[breeds.size - 2], breeds.last()) else null

        val lastTopic = sessionHistory.flatMap { it.entities }
            .find { it.type == EntityType.POULTRY_TOPIC || it.type == EntityType.INCUBATION_TOPIC }
            ?.value

        return HatchyContextSnapshot(
            currentModule = appContext.currentModule.toString(),
            selectedSpecies = appContext.selectedSpecies,
            tier = appContext.tier,
            isAdminOrDeveloper = appContext.isAdminOrDeveloper,
            recentBreedsMentioned = recentBreeds,
            recentTraitsMentioned = recentTraits,
            lastResult = lastResult,
            hasUserDataContext = false,
            lastSpecies = lastSpecies,
            lastBreedPair = lastBreedPair,
            lastTopic = lastTopic
        )
    }

    fun clearSession() {
        sessionHistory.clear()
    }
}

data class HatchyContextSnapshot(
    val currentModule: String,
    val selectedSpecies: String?,
    val tier: SubscriptionTier,
    val isAdminOrDeveloper: Boolean,
    val recentBreedsMentioned: List<String>,
    val recentTraitsMentioned: List<String>,
    val lastResult: HatchyIntentResult?,
    val hasUserDataContext: Boolean,
    val lastSpecies: PoultrySpecies? = null,
    val lastBreedPair: Pair<String, String>? = null,
    val lastTopic: String? = null // Can be specialized later
) {
    val lastIntent: HatchyIntent? get() = lastResult?.intent
    val lastModule: String? get() = lastResult?.module
    val lastEntities: List<HatchyEntity> get() = lastResult?.entities ?: emptyList()
}
