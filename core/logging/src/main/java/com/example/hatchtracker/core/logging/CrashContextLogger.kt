package com.example.hatchtracker.core.logging

import java.util.concurrent.ConcurrentHashMap

/**
 * Utility to capture and provide application context for crash reporting and diagnostics.
 * This helper stores volatile state like current route, user ID, and recent events.
 */
object CrashContextLogger {
    private val contextMap = ConcurrentHashMap<String, String>()
    private val MAX_STRING_LENGTH = 100

    fun setCurrentRoute(route: String?) {
        update("route", route)
    }

    fun setUser(uid: String?) {
        update("user_uid", uid)
    }

    fun setSubscriptionTier(tier: String?) {
        update("subscription_tier", tier)
    }

    fun setLastScreenEvent(event: String) {
        update("last_event", event)
    }

    fun update(key: String, value: String?) {
        if (value == null) {
            contextMap.remove(key)
        } else {
            contextMap[key] = value.take(MAX_STRING_LENGTH)
        }
    }

    fun snapshot(): Map<String, String> {
        return contextMap.toMap()
    }

    fun getFormattedSnapshot(): String {
        return contextMap.entries.joinToString(", ") { "${it.key}=${it.value}" }
    }

    fun log(tag: String = "CrashContext") {
        try {
            val ctx = getFormattedSnapshot()
            if (ctx.isNotEmpty()) {
                Logger.i(tag, "op=snapshot context={$ctx}")
            }
        } catch (e: Exception) {
            // Never throw from logging utility
        }
    }
}
