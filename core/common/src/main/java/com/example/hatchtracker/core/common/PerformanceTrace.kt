package com.example.hatchtracker.core.common

import android.util.Log

/**
 * Debugging utilities to track Flow emissions and recompositions 
 * to ensure O(1) list scalablity.
 */
object PerformanceTrace {
    private const val TAG = "PerfTrace"
    var isEnabled = true

    fun logEmission(source: String, count: Int, processingMillis: Long) {
        if (!isEnabled) return
        Log.d(TAG, "[$source] Emitted $count items in ${processingMillis}ms")
    }

    fun markStart(): Long = System.currentTimeMillis()

    fun markEnd(source: String, startMillis: Long) {
        if (!isEnabled) return
        Log.d(TAG, "[$source] completed in ${System.currentTimeMillis() - startMillis}ms")
    }
}
