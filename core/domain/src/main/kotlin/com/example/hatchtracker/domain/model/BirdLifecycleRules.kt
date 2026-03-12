package com.example.hatchtracker.domain.model

import com.example.hatchtracker.model.BirdLifecycleStage

/**
 * Rules governing transitions between lifecycle stages.
 */
object BirdLifecycleRules {

    private val transitions = mapOf(
        BirdLifecycleStage.EGG to listOf(BirdLifecycleStage.INCUBATING, BirdLifecycleStage.SOLD, BirdLifecycleStage.DECEASED),
        BirdLifecycleStage.INCUBATING to listOf(BirdLifecycleStage.FLOCKLET, BirdLifecycleStage.DECEASED),
        BirdLifecycleStage.FLOCKLET to listOf(BirdLifecycleStage.ADULT, BirdLifecycleStage.SOLD, BirdLifecycleStage.DECEASED),
        BirdLifecycleStage.ADULT to listOf(BirdLifecycleStage.SOLD, BirdLifecycleStage.DECEASED)
    )

    /**
     * Checks if a transition from one stage to another is valid.
     */
    fun isTransitionAllowed(from: BirdLifecycleStage, to: BirdLifecycleStage): Boolean {
        if (from == to) return true
        return transitions[from]?.contains(to) ?: false
    }

    /**
     * Returns the default next stage for a given stage, or null if terminal.
     */
    fun defaultNextStage(current: BirdLifecycleStage): BirdLifecycleStage? {
        return when (current) {
            BirdLifecycleStage.EGG -> BirdLifecycleStage.INCUBATING
            BirdLifecycleStage.INCUBATING -> BirdLifecycleStage.FLOCKLET
            BirdLifecycleStage.FLOCKLET -> BirdLifecycleStage.ADULT
            else -> null
        }
    }
}

