package com.example.hatchtracker.domain.hatchy.routing

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks conversation state to handle follow-up questions.
 * Stores the last intent, module, and entities.
 */
@Singleton
class HatchyContextMemory @Inject constructor() {
    private var lastIntent: HatchyIntent? = null
    private var lastModule: String? = null
    private var lastEntities: List<HatchyEntity> = emptyList()

    fun update(result: HatchyIntentResult) {
        lastIntent = result.intent
        lastModule = result.module ?: lastModule
        if (result.entities.isNotEmpty()) {
            lastEntities = result.entities
        }
    }

    fun getSnapshot(): MemorySnapshot {
        return MemorySnapshot(
            lastIntent = lastIntent,
            lastModule = lastModule,
            lastEntities = lastEntities
        )
    }

    fun clear() {
        lastIntent = null
        lastModule = null
        lastEntities = emptyList()
    }
}

data class MemorySnapshot(
    val lastIntent: HatchyIntent?,
    val lastModule: String?,
    val lastEntities: List<HatchyEntity>
)
