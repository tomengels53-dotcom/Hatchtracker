package com.example.hatchtracker.core.logging

import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

object SafeRun {
    fun <T> run(tag: String, name: String, block: () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun <T> runSuspend(tag: String, name: String, block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    fun isTransient(t: Throwable): Boolean {
        return hasCause(t) { cause ->
            cause is IOException ||
                cause is SocketTimeoutException ||
                cause is TimeoutException ||
                cause.javaClass.simpleName.contains("FirebaseNetworkException", ignoreCase = true)
        }
    }

    private fun hasCause(t: Throwable?, predicate: (Throwable) -> Boolean): Boolean {
        var current = t
        while (current != null) {
            if (predicate(current)) return true
            current = current.cause
        }
        return false
    }
}
