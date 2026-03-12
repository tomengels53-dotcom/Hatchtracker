package com.example.hatchtracker.domain.model

/**
 * Represents a post in the community feed.
 */
data class CommunityPost(
    val id: String = "",
    val authorUserId: String,
    val authorSnapshot: CommunityAuthorSnapshot,
    val kind: PostKind = PostKind.TEXT,
    val bodyText: String,
    val media: List<CommunityMedia> = emptyList(),
    val linkedEntities: List<EntityPassportSnapshot> = emptyList(),
    val visibility: Visibility = Visibility.PUBLIC,
    val moderationState: ModerationState = ModerationState.CLEAN,
    val stats: PostStats = PostStats(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastActivityAt: Long = System.currentTimeMillis()
)

enum class PostKind {
    TEXT, IMAGE, VIDEO, QUESTION, ANNOUNCEMENT
}

enum class Visibility {
    PUBLIC, FOLLOWERS, PRIVATE
}

enum class ModerationState {
    CLEAN, PENDING_REVIEW, FLAGGED, REMOVED, BLOCKED
}

data class PostStats(
    val commentCount: Int = 0,
    val reactionCount: Int = 0,
    val shareCount: Int = 0,
    val reportCount: Int = 0
)

data class CommunityMedia(
    val mediaId: String,
    val type: String, // IMAGE, VIDEO
    val url: String,
    val thumbnailUrl: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0
)

/**
 * Represents a comment on a community post.
 */
data class CommunityComment(
    val id: String = "",
    val postId: String,
    val authorUserId: String,
    val authorSnapshot: CommunityAuthorSnapshot,
    val bodyText: String,
    val moderationState: ModerationState = ModerationState.CLEAN,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Represents a reaction to a post.
 */
data class CommunityReaction(
    val userId: String,
    val type: String, // LIKE, LOVE, WOW, HELP, etc.
    val createdAt: Long = System.currentTimeMillis()
)
