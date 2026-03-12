package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.models.UserBlockRelationDto
import com.example.hatchtracker.domain.model.UserBlockRelation
import com.example.hatchtracker.domain.repository.UserBlockingRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserBlockingRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserBlockingRepository {
    private val blocksCollection = firestore.collection("user_block_relations")

    override suspend fun addBlock(relation: UserBlockRelation): Result<Unit> = runCatching {
        val id = "${relation.blockerUserId}_${relation.blockedUserId}"
        blocksCollection.document(id).set(relation.toDto()).await()
    }

    override suspend fun removeBlock(blockerId: String, blockedUserId: String): Result<Unit> = runCatching {
        val id = "${blockerId}_${blockedUserId}"
        blocksCollection.document(id).delete().await()
    }

    override suspend fun isBlocked(blockerId: String, blockedUserId: String): Boolean {
        val id = "${blockerId}_${blockedUserId}"
        val doc = blocksCollection.document(id).get().await()
        return doc.exists()
    }

    override suspend fun getBlockedUserIds(blockerId: String): List<String> {
        return blocksCollection
            .whereEqualTo("blockerUserId", blockerId)
            .get()
            .await()
            .mapNotNull { it.toObject(UserBlockRelationDto::class.java).blockedUserId.takeIf(String::isNotBlank) }
    }
}

private fun UserBlockRelation.toDto() = UserBlockRelationDto(
    blockerUserId = blockerUserId,
    blockedUserId = blockedUserId,
    createdAt = createdAt
)
