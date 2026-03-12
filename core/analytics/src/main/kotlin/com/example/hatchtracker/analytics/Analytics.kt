package com.example.hatchtracker.analytics

interface Analytics {
    fun logEvent(name: String, params: Map<String, Any> = emptyMap())
}

class NoOpAnalytics : Analytics {
    override fun logEvent(name: String, params: Map<String, Any>) {
        // No-op
    }
}
