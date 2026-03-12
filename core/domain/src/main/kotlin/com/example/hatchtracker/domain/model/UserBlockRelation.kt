package com.example.hatchtracker.domain.model

/**
 * Represents a private block relationship between two users.
 * 
 * Policy:
 * - Blocked users' posts are hidden from the blocker's feed.
 * - Existing comments from blocked users are collapsed.
 * - Marketplace listings from blocked users are hidden.
 * - Question routing skips blocked experts.
 */
data class UserBlockRelation(
    val blockerUserId: String,
    val blockedUserId: String,
    val createdAt: Long = System.currentTimeMillis()
)
