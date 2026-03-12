package com.example.hatchtracker.domain.service

import com.example.hatchtracker.domain.model.UserBlockRelation
import com.example.hatchtracker.domain.repository.UserBlockingRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockingService @Inject constructor(
    private val blockingRepository: UserBlockingRepository
) {
    suspend fun blockUser(blockerId: String, blockedId: String): Result<Unit> = runCatching {
        blockingRepository.addBlock(UserBlockRelation(blockerId, blockedId)).getOrThrow()
    }

    suspend fun unblockUser(blockerId: String, blockedId: String): Result<Unit> = runCatching {
        blockingRepository.removeBlock(blockerId, blockedId).getOrThrow()
    }

    suspend fun isBlocked(blockerId: String, blockedId: String): Boolean =
        blockingRepository.isBlocked(blockerId, blockedId)

    /**
     * Get list of user IDs blocked by this user.
     */
    suspend fun getBlockedUserIds(userId: String): List<String> =
        blockingRepository.getBlockedUserIds(userId)
}
