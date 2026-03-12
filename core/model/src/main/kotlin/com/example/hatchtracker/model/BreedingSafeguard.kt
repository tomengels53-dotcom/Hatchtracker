package com.example.hatchtracker.model

/**
 * Domain model for breeding safety and welfare alerts.
 */
sealed class BreedingSafeguard {
    object None : BreedingSafeguard()
    object WarningInbreeding : BreedingSafeguard()
    object BlockingLethal : BreedingSafeguard()
}
