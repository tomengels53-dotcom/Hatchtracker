package com.example.hatchtracker.core.logging

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogBuffer {
    private const val MAX_LINES = 200
    private val lock = Any()
    private val lines: ArrayDeque<String> = ArrayDeque(MAX_LINES)
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun append(level: String, tag: String, msg: String, throwable: Throwable? = null) {
        synchronized(lock) {
            val timestamp = formatter.format(Date())
            val fullMsg = if (throwable != null) {
                "$timestamp $level/$tag: $msg | ${throwable::class.java.simpleName}: ${throwable.message}"
            } else {
                "$timestamp $level/$tag: $msg"
            }
            if (lines.size >= MAX_LINES) {
                lines.removeFirst()
            }
            lines.addLast(fullMsg)
        }
    }

    fun recentLines(maxLines: Int = MAX_LINES): List<String> {
        synchronized(lock) {
            if (lines.isEmpty()) return emptyList()
            val takeCount = maxLines.coerceAtMost(lines.size)
            return lines.toList().takeLast(takeCount)
        }
    }
}
