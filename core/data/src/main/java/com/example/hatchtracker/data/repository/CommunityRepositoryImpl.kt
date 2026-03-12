package com.example.hatchtracker.data.repository

import com.example.hatchtracker.core.logging.Logger
import com.example.hatchtracker.data.mappers.*
import com.example.hatchtracker.data.models.*
import com.example.hatchtracker.domain.model.*
import com.example.hatchtracker.domain.repository.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CommunityPostRepository, CommunityAdminRepository {

    private val POSTS_COLLECTION = "community_posts"
    private val COMMENTS_COLLECTION = "comments"
    private val REPORTS_COLLECTION = "community_reports"
    private val BLOCKS_COLLECTION = "community_user_blocks"

    override fun getFeed(limit: Int): Flow<List<CommunityPost>> = callbackFlow {
        val subscription = firestore.collection(POSTS_COLLECTION)
            .whereEqualTo("moderationState", "CLEAN")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val posts = snapshot?.documents?.mapNotNull { it.toObject(PostDto::class.java)?.toDomain() } ?: emptyList()
                trySend(posts)
            }
        awaitClose { subscription.remove() }
    }

    override fun getPost(postId: String): Flow<CommunityPost?> = callbackFlow {
        val subscription = firestore.collection(POSTS_COLLECTION)
            .document(postId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val post = snapshot?.toObject(PostDto::class.java)?.toDomain()
                trySend(post)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun createPost(post: CommunityPost): Result<Unit> {
        return try {
            firestore.collection(POSTS_COLLECTION)
                .document(post.id.ifEmpty { java.util.UUID.randomUUID().toString() })
                .set(post.toDto())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePost(post: CommunityPost): Result<Unit> {
        return try {
            firestore.collection(POSTS_COLLECTION)
                .document(post.id)
                .set(post.toDto())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            firestore.collection(POSTS_COLLECTION).document(postId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getComments(postId: String): Flow<List<CommunityComment>> = callbackFlow {
        val subscription = firestore.collection(POSTS_COLLECTION)
            .document(postId)
            .collection(COMMENTS_COLLECTION)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val comments = snapshot?.documents?.mapNotNull { it.toObject(CommentDto::class.java)?.toDomain() } ?: emptyList()
                trySend(comments)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun addComment(postId: String, comment: CommunityComment): Result<Unit> {
        return try {
            firestore.collection(POSTS_COLLECTION)
                .document(postId)
                .collection(COMMENTS_COLLECTION)
                .document(comment.id.ifEmpty { java.util.UUID.randomUUID().toString() })
                .set(comment.toDto())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteComment(postId: String, commentId: String): Result<Unit> {
        return try {
            firestore.collection(POSTS_COLLECTION)
                .document(postId)
                .collection(COMMENTS_COLLECTION)
                .document(commentId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleReaction(postId: String, userId: String, type: String): Result<Unit> {
        // In a real app, this would use a FieldValue.increment or a Cloud Function to avoid drift.
        // For foundation, we define the contract.
        return Result.success(Unit) 
    }

    override suspend fun reportPost(postId: String, reason: String, reporterId: String): Result<Unit> {
        return try {
            val report = hashMapOf(
                "postId" to postId,
                "reason" to reason,
                "reporterId" to reporterId,
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection(REPORTS_COLLECTION).add(report).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reportComment(postId: String, commentId: String, reason: String, reporterId: String): Result<Unit> {
        return try {
            val report = hashMapOf(
                "postId" to postId,
                "commentId" to commentId,
                "reason" to reason,
                "reporterId" to reporterId,
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection(REPORTS_COLLECTION).add(report).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun blockUser(userId: String, blockedUserId: String): Result<Unit> {
        return try {
            val block = hashMapOf(
                "userId" to userId,
                "blockedUserId" to blockedUserId,
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection(BLOCKS_COLLECTION).add(block).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
