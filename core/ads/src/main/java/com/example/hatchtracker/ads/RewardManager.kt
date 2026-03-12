package com.example.hatchtracker.ads

import android.content.Context
import android.content.SharedPreferences
import android.os.StrictMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages temporary rewards granted to users (e.g. for watching ads).
 * Persists reward state locally.
 */
@Singleton
class RewardManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "hatchtracker_rewards"
        private const val KEY_AD_FREE_EXPIRY = "ad_free_expiry_timestamp"
    }

    private val _isAdFreeRewardActive = MutableStateFlow(false)
    val isAdFreeRewardActive: StateFlow<Boolean> = _isAdFreeRewardActive.asStateFlow()

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    init {
        checkRewardStatus()
    }

    /**
     * Checks if the ad-free reward is currently active.
     */
    fun checkRewardStatus() {
        val expiryTime = allowThreadDiskReads {
            prefs.getLong(KEY_AD_FREE_EXPIRY, 0L)
        }
        val isActive = System.currentTimeMillis() < expiryTime
        _isAdFreeRewardActive.value = isActive
    }

    /**
     * Grants the ad-free reward for the specified duration.
     */
    fun grantAdFreeReward(durationMillis: Long = TimeUnit.HOURS.toMillis(24)) {
        val newExpiry = System.currentTimeMillis() + durationMillis
        allowThreadDiskWrites {
            prefs.edit().putLong(KEY_AD_FREE_EXPIRY, newExpiry).apply()
        }
        _isAdFreeRewardActive.value = true
    }

    private inline fun <T> allowThreadDiskReads(block: () -> T): T {
        val oldPolicy = StrictMode.allowThreadDiskReads()
        return try {
            block()
        } finally {
            StrictMode.setThreadPolicy(oldPolicy)
        }
    }

    private inline fun <T> allowThreadDiskWrites(block: () -> T): T {
        val oldPolicy = StrictMode.allowThreadDiskWrites()
        return try {
            block()
        } finally {
            StrictMode.setThreadPolicy(oldPolicy)
        }
    }
}
