package com.example.hatchtracker.model

object BirdLifecycleRules {
    fun isTransitionAllowed(from: BirdLifecycleStage, to: BirdLifecycleStage): Boolean {
        if (from == to) return true
        if (to == BirdLifecycleStage.SOLD || to == BirdLifecycleStage.DECEASED) return true
        return when (from) {
            BirdLifecycleStage.EGG -> to == BirdLifecycleStage.INCUBATING
            BirdLifecycleStage.INCUBATING -> to == BirdLifecycleStage.FLOCKLET || to == BirdLifecycleStage.ADULT
            BirdLifecycleStage.FLOCKLET -> to == BirdLifecycleStage.ADULT
            BirdLifecycleStage.ADULT -> false
            BirdLifecycleStage.SOLD, BirdLifecycleStage.DECEASED -> false
        }
    }
}
