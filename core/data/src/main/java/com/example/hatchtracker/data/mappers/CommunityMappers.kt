package com.example.hatchtracker.data.mappers

import com.example.hatchtracker.data.models.*
import com.example.hatchtracker.domain.model.*

fun PostDto.toDomain(): CommunityPost {
    return CommunityPost(
        id = id,
        authorUserId = authorUserId,
        authorSnapshot = authorSnapshot ?: CommunityAuthorSnapshot(userId = authorUserId, username = "Unknown", displayName = "Unknown", avatarUrl = null, breederType = null, reputationScore = 0),
        kind = PostKind.valueOf(kind),
        bodyText = bodyText,
        media = media,
        linkedEntities = linkedEntities,
        visibility = Visibility.valueOf(visibility),
        moderationState = ModerationState.valueOf(moderationState),
        stats = PostStats(
            commentCount = commentCount,
            reactionCount = reactionCount,
            shareCount = shareCount,
            reportCount = reportCount
        ),
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastActivityAt = lastActivityAt
    )
}

fun CommunityPost.toDto(): PostDto {
    return PostDto(
        id = id,
        authorUserId = authorUserId,
        authorSnapshot = authorSnapshot,
        kind = kind.name,
        bodyText = bodyText,
        media = media,
        linkedEntities = linkedEntities,
        visibility = visibility.name,
        moderationState = moderationState.name,
        commentCount = stats.commentCount,
        reactionCount = stats.reactionCount,
        shareCount = stats.shareCount,
        reportCount = stats.reportCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastActivityAt = lastActivityAt
    )
}

fun CommentDto.toDomain(): CommunityComment {
    return CommunityComment(
        id = id,
        postId = postId,
        authorUserId = authorUserId,
        authorSnapshot = authorSnapshot ?: CommunityAuthorSnapshot(userId = authorUserId, username = "Unknown", displayName = "Unknown", avatarUrl = null, breederType = null, reputationScore = 0),
        bodyText = bodyText,
        moderationState = ModerationState.valueOf(moderationState),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun CommunityComment.toDto(): CommentDto {
    return CommentDto(
        id = id,
        postId = postId,
        authorUserId = authorUserId,
        authorSnapshot = authorSnapshot,
        bodyText = bodyText,
        moderationState = moderationState.name,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
