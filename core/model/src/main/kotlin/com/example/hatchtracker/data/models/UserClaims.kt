package com.example.hatchtracker.data.models

/**
 * Model representing the custom claims parsed from Firebase ID Token.
 */
data class UserClaims(
    val isAdmin: Boolean = false,
    val isDeveloper: Boolean = false,
    val isCommunityAdmin: Boolean = false
)
