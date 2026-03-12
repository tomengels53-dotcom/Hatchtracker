package com.example.hatchtracker.core.logging

import android.util.Log

object Logger {
    fun d(tag: String, msg: String) {
        LogBuffer.append("D", tag, msg)
        Log.d(tag, msg)
    }

    fun i(tag: String, msg: String) {
        LogBuffer.append("I", tag, msg)
        Log.i(tag, msg)
    }

    fun w(tag: String, msg: String, throwable: Throwable? = null) {
        LogBuffer.append("W", tag, msg, throwable)
        if (throwable != null) {
            Log.w(tag, msg, throwable)
        } else {
            Log.w(tag, msg)
        }
    }

    fun e(tag: String, msg: String, throwable: Throwable? = null) {
        LogBuffer.append("E", tag, msg, throwable)
        if (throwable != null) {
            Log.e(tag, msg, throwable)
        } else {
            Log.e(tag, msg)
        }
    }

    fun getRecentLogs(maxLines: Int = 200): List<String> {
        return LogBuffer.recentLines(maxLines)
    }
}
