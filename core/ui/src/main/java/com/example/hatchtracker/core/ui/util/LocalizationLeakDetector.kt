package com.example.hatchtracker.core.ui.util

import android.util.Log
import com.example.hatchtracker.core.ui.BuildConfig

/**
 * LocalizationLeakDetector: Debug-only utility to catch non-localized text usage.
 */
object LocalizationLeakDetector {
    private const val TAG = "LocalizationLeak"

    /**
     * Flags usage of UiText.Plain which should be avoided for user-facing text.
     */
    fun logLeakedPlain(value: String) {
        if (BuildConfig.DEBUG && value.isNotEmpty()) {
            // Check if it's likely a developer log vs user-facing text
            if (value.length > 3 && value.any { it.isLowerCase() }) {
                Log.w(TAG, "DETECTED UNLOCALIZED TEXT: \"$value\"")
            }
        }
    }
}
