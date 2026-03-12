package com.example.hatchtracker.core.common

import android.content.Context
import android.content.SharedPreferences
import android.os.StrictMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        withAllowedDiskReads {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun hasSeenWelcome(): Boolean {
        return withAllowedDiskReads {
            prefs.getBoolean(KEY_HAS_SEEN_WELCOME, false)
        }
    }

    fun setSeenWelcome() {
        withAllowedDiskWrites {
            prefs.edit().putBoolean(KEY_HAS_SEEN_WELCOME, true).apply()
        }
    }

    fun hasCompletedOnboarding(): Boolean {
        return withAllowedDiskReads {
            prefs.getBoolean(KEY_HAS_COMPLETED_ONBOARDING, false)
        }
    }

    fun setCompletedOnboarding() {
        withAllowedDiskWrites {
            prefs.edit().putBoolean(KEY_HAS_COMPLETED_ONBOARDING, true).apply()
        }
    }

    private inline fun <T> withAllowedDiskReads(block: () -> T): T {
        val policy = StrictMode.allowThreadDiskReads()
        return try {
            block()
        } finally {
            StrictMode.setThreadPolicy(policy)
        }
    }

    private inline fun <T> withAllowedDiskWrites(block: () -> T): T {
        val policy = StrictMode.allowThreadDiskWrites()
        return try {
            block()
        } finally {
            StrictMode.setThreadPolicy(policy)
        }
    }

    companion object {
        private const val PREFS_NAME = "hatchtracker_onboarding"
        private const val KEY_HAS_SEEN_WELCOME = "has_seen_welcome"
        private const val KEY_HAS_COMPLETED_ONBOARDING = "has_completed_onboarding"
    }
}
