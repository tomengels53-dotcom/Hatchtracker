package com.example.hatchtracker.domain.service

import com.example.hatchtracker.domain.model.ExpertiseProfile
import com.example.hatchtracker.domain.model.BadgeType

/**
 * Automatically routes community questions toward users with relevant expertise.
 */
class QuestionRoutingService(
    private val expertiseSignalService: ExpertiseSignalService
) {
    // Mock storage for cooldowns: userId -> list of notification timestamps
    private val routingHistory = mutableMapOf<String, MutableList<Long>>()

    fun detectTopics(content: String): List<QuestionTopic> {
        val topics = mutableListOf<QuestionTopic>()
        val lowerContent = content.lowercase()

        if (lowerContent.contains("incubat") || lowerContent.contains("hatch")) {
            topics.add(QuestionTopic.INCUBATION)
        }
        if (lowerContent.contains("gene") || lowerContent.contains("breed") || lowerContent.contains("trait")) {
            topics.add(QuestionTopic.GENETICS)
        }
        if (lowerContent.contains("feed") || lowerContent.contains("nutrition") || lowerContent.contains("eat")) {
            topics.add(QuestionTopic.NUTRITION)
        }
        if (lowerContent.contains("sick") || lowerContent.contains("health") || lowerContent.contains("disease")) {
            topics.add(QuestionTopic.HEALTH)
        }
        if (lowerContent.contains("duck") || lowerContent.contains("goose") || lowerContent.contains("waterfowl")) {
            topics.add(QuestionTopic.WATERFOWL)
        }

        return topics
    }

    fun findExpertsForTopics(
        topics: List<QuestionTopic>,
        expertProfiles: List<ExpertiseProfile>,
        blockedUserIds: List<String> = emptyList()
    ): List<String> {
        val targetUserIds = mutableSetOf<String>()

        topics.forEach { topic ->
            val requiredBadge = when (topic) {
                QuestionTopic.INCUBATION -> BadgeType.INCUBATION_EXPERT
                QuestionTopic.GENETICS -> BadgeType.GENETICS_SPECIALIST
                QuestionTopic.WATERFOWL -> BadgeType.WATERFOWL_SPECIALIST
                else -> null
            }

            expertProfiles.filter { profile ->
                // Route to experts with specific badges or relevant signals
                val hasBadge = requiredBadge?.let { b -> profile.badges.any { it.type == b } } ?: false
                val hasSignals = when (topic) {
                    QuestionTopic.INCUBATION -> profile.signals.breedingSuccessRate > 0.8
                    QuestionTopic.GENETICS -> profile.signals.consistentLineageDepth >= 3
                    else -> false
                }
                
                val isBlocked = blockedUserIds.contains(profile.userId)
                
                (hasBadge || hasSignals) && !isBlocked && isNotOnCooldown(profile.userId)
            }
            .sortedByDescending { it.signals.let { s -> expertiseSignalService.calculateScore(s) } }
            .forEach { targetUserIds.add(it.userId) }
        }

        return targetUserIds.take(5).toList() // Limit notifications to avoid spam
    }

    private fun isNotOnCooldown(userId: String): Boolean {
        val now = System.currentTimeMillis()
        val sixHours = 1000L * 60 * 60 * 6
        val windowStart = now - sixHours
        
        val history = routingHistory[userId] ?: return true
        history.removeIf { it < windowStart }
        return history.size < 3 // Max 3 notifications per 6 hours
    }

    fun recordRoutingNotification(userId: String) {
        val history = routingHistory.getOrPut(userId) { mutableListOf() }
        history.add(System.currentTimeMillis())
    }
}

enum class QuestionTopic {
    INCUBATION,
    GENETICS,
    NUTRITION,
    HEALTH,
    WATERFOWL
}
