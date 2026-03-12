package com.example.hatchtracker.domain.breeding

/**
 * Strict rules for bird lifecycle transitions in HatchBase.
 */
enum class LifecycleStage { 
    EGG, 
    INCUBATION, 
    NURSERY, 
    FLOCK, 
    SOLD, 
    DECEASED 
}

data class LifecycleRule(
    val from: LifecycleStage,
    val to: LifecycleStage,
    val allowed: Boolean,
    val explanation: String
)

object HatchyLifecycleRules {

    val transitions = listOf(
        LifecycleRule(LifecycleStage.EGG, LifecycleStage.INCUBATION, true, "Eggs are collected from the flock and prepared for the incubator."),
        LifecycleRule(LifecycleStage.INCUBATION, LifecycleStage.NURSERY, true, "Successful hatches produce flocklets that move to the nursery (brooder)."),
        LifecycleRule(LifecycleStage.NURSERY, LifecycleStage.FLOCK, true, "Once feathered out and hardy, birds move from the nursery to the main flock."),
        
        // Terminal transitions
        LifecycleRule(LifecycleStage.FLOCK, LifecycleStage.SOLD, true, "Adult birds can be sold, which should be recorded in the Finance module."),
        LifecycleRule(LifecycleStage.NURSERY, LifecycleStage.SOLD, true, "Young birds (chicks/flocklets) can be sold directly from the nursery."),
        
        // Safety transitions
        LifecycleRule(LifecycleStage.INCUBATION, LifecycleStage.DECEASED, true, "Eggs that fail to develop or pip are marked deceased (outcome)."),
        LifecycleRule(LifecycleStage.NURSERY, LifecycleStage.DECEASED, true, "Losses in the brooder should be recorded for health statistics."),
        LifecycleRule(LifecycleStage.FLOCK, LifecycleStage.DECEASED, true, "Adult losses should be marked to maintain accurate flock counts.")
    )

    fun validate(from: LifecycleStage, to: LifecycleStage): LifecycleRule {
        return transitions.find { it.from == from && it.to == to }
            ?: LifecycleRule(from, to, false, "That transition isn't in the natural order of things. Usually, birds move Egg -> Incubation -> Nursery -> Flock.")
    }

    fun lifecycleSummary(): String {
        return "The natural path is Egg → Incubation → Nursery → Flock. Birds can also be Sold or marked Deceased at any stage given the right circumstances."
    }
}
