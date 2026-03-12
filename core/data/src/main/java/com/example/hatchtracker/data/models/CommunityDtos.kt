package com.example.hatchtracker.data.models

import com.google.firebase.firestore.IgnoreExtraProperties
import com.example.hatchtracker.domain.model.*

@IgnoreExtraProperties
data class PostDto(
    val id: String = "",
    val authorUserId: String = "",
    val authorSnapshot: CommunityAuthorSnapshot? = null,
    val kind: String = "TEXT",
    val bodyText: String = "",
    val media: List<CommunityMedia> = emptyList(),
    val linkedEntities: List<EntityPassportSnapshot> = emptyList(),
    val visibility: String = "PUBLIC",
    val moderationState: String = "CLEAN",
    val commentCount: Int = 0,
    val reactionCount: Int = 0,
    val shareCount: Int = 0,
    val reportCount: Int = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val lastActivityAt: Long = 0
)

@IgnoreExtraProperties
data class CommentDto(
    val id: String = "",
    val postId: String = "",
    val authorUserId: String = "",
    val authorSnapshot: CommunityAuthorSnapshot? = null,
    val bodyText: String = "",
    val moderationState: String = "CLEAN",
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

@IgnoreExtraProperties
data class ReactionDto(
    val userId: String = "",
    val type: String = "LIKE",
    val createdAt: Long = 0
)
