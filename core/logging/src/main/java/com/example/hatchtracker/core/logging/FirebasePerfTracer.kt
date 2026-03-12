package com.example.hatchtracker.core.logging

import android.util.Log
import java.util.concurrent.atomic.AtomicInteger

/**
 * Debug-only tracer to ensure we do not exceed the
 * God-Mode threshold of 5 Firestore reads per navigation.
 */
object FirebasePerfTracer {
    private val readCounter = AtomicInteger(0)
    private val writeCounter = AtomicInteger(0)

    fun resetNavigationCounters() {
        val reads = readCounter.getAndSet(0)
        val writes = writeCounter.getAndSet(0)
        Log.i("FirebasePerfTracer", "Navigated. Previous route consumed $reads reads and $writes writes.")
        if (reads > 5) {
            Log.e("FirebasePerfTracer", "VIOLATION: Exceeded 5 reads per navigation! ($reads reads)")
        }
    }

    fun recordRead(count: Int = 1) {
        val current = readCounter.addAndGet(count)
        Log.d("FirebasePerfTracer", "Firestore Read +$count (Total: $current)")
    }

    fun recordWrite(count: Int = 1) {
        val current = writeCounter.addAndGet(count)
        Log.d("FirebasePerfTracer", "Firestore Write +$count (Total: $current)")
    }
}
