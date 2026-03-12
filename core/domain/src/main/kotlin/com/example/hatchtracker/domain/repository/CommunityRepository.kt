package com.example.hatchtracker.domain.repository

import com.example.hatchtracker.domain.model.CommunityPost
import com.example.hatchtracker.domain.model.CommunityComment
import com.example.hatchtracker.domain.model.CommunityReaction
import kotlinx.coroutines.flow.Flow

/**
 * Repository for social feed and post lifecycle operations.
 */
interface CommunityPostRepository {
    fun getFeed(limit: Int = 20): Flow<List<CommunityPost>>
    fun getPost(postId: String): Flow<CommunityPost?>
    suspend fun createPost(post: CommunityPost): Result<Unit>
    suspend fun updatePost(post: CommunityPost): Result<Unit>
    suspend fun deletePost(postId: String): Result<Unit>
    
    // Comments
    fun getComments(postId: String): Flow<List<CommunityComment>>
    suspend fun addComment(postId: String, comment: CommunityComment): Result<Unit>
    suspend fun deleteComment(postId: String, commentId: String): Result<Unit>
    
    // Reactions
    suspend fun toggleReaction(postId: String, userId: String, type: String): Result<Unit>
}

/**
 * Repository for moderation and reporting content.
 */
interface CommunityAdminRepository {
    suspend fun reportPost(postId: String, reason: String, reporterId: String): Result<Unit>
    suspend fun reportComment(postId: String, commentId: String, reason: String, reporterId: String): Result<Unit>
    suspend fun blockUser(userId: String, blockedUserId: String): Result<Unit>
}
