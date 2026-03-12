package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.models.UserSafetyStateDto
import com.example.hatchtracker.domain.model.UserSafetyState
import com.example.hatchtracker.domain.repository.UserSafetyStateRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSafetyRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserSafetyStateRepository {
    private val safetyCollection = firestore.collection("user_safety_states")

    /**
     * Fetch user safety state with lazy evaluation of expiries.
     */
    override suspend fun getSafetyState(userId: String): UserSafetyState {
        val doc = safetyCollection.document(userId).get().await()
        val domain = doc.toObject(UserSafetyStateDto::class.java)?.toDomain() 
            ?: UserSafetyState(userId = userId)
        
        return domain.evaluateExpiries()
    }

    override suspend fun updateSafetyState(state: UserSafetyState): Result<Unit> = runCatching {
        val dto = state.toDto()
        safetyCollection.document(state.userId).set(dto).await()
    }

    /**
     * Helper to check if a user is restricted from a specific action.
     */
    suspend fun isRestricted(userId: String, action: RestrictionType): Boolean {
        val state = getSafetyState(userId)
        val now = System.currentTimeMillis()
        return when (action) {
            RestrictionType.POSTING -> state.postingRestrictedUntil?.let { it > now } ?: false
            RestrictionType.COMMENTING -> state.commentingRestrictedUntil?.let { it > now } ?: false
            RestrictionType.MARKETPLACE -> state.marketplaceRestrictedUntil?.let { it > now } ?: false
            RestrictionType.SUSPENSION -> state.isSuspended || (state.suspensionUntil?.let { it > now } ?: false)
        }
    }
}

enum class RestrictionType {
    POSTING, COMMENTING, MARKETPLACE, SUSPENSION
}

private fun UserSafetyState.evaluateExpiries(): UserSafetyState {
    val now = System.currentTimeMillis()
    val suspensionExpiry = suspensionUntil
    return copy(
        isSuspended = isSuspended && (suspensionExpiry == null || suspensionExpiry > now),
        suspensionUntil = suspensionExpiry?.takeIf { it > now },
        postingRestrictedUntil = postingRestrictedUntil?.takeIf { it > now },
        commentingRestrictedUntil = commentingRestrictedUntil?.takeIf { it > now },
        marketplaceRestrictedUntil = marketplaceRestrictedUntil?.takeIf { it > now }
    )
}

fun UserSafetyState.toDto() = UserSafetyStateDto(
    userId = userId,
    strikeCount = strikeCount,
    isSuspended = isSuspended,
    suspensionUntil = suspensionUntil,
    postingRestrictedUntil = postingRestrictedUntil,
    commentingRestrictedUntil = commentingRestrictedUntil,
    marketplaceRestrictedUntil = marketplaceRestrictedUntil,
    lastModeratedAt = lastModeratedAt,
    moderatorNotes = moderatorNotes
)

fun UserSafetyStateDto.toDomain() = UserSafetyState(
    userId = userId,
    strikeCount = strikeCount,
    isSuspended = isSuspended,
    suspensionUntil = suspensionUntil,
    postingRestrictedUntil = postingRestrictedUntil,
    commentingRestrictedUntil = commentingRestrictedUntil,
    marketplaceRestrictedUntil = marketplaceRestrictedUntil,
    lastModeratedAt = lastModeratedAt,
    moderatorNotes = moderatorNotes
)
