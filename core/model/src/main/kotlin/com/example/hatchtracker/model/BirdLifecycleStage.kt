package com.example.hatchtracker.model

/**
 * Explicit stages for the bird lifecycle.
 */
enum class BirdLifecycleStage {
    EGG,         // Raw eggs collected
    INCUBATING,  // Eggs currently in an incubator/hatcher
    FLOCKLET,    // Young chicks in the nursery
    ADULT,       // Mature birds in a flock
    SOLD,        // Terminal state: bird has been sold
    DECEASED     // Terminal state: bird has been lost
}
