package com.example.hatchtracker.core.logging

import android.content.Context
import com.example.hatchtracker.data.models.WorkerRunStatus

object WorkerRunTracker {
    private const val PREFS_NAME = "worker_run_status"
    private const val KEY_WORKER_NAMES = "worker_names"

    fun recordSuccess(context: Context, workerName: String) {
        updateStatus(context, workerName, success = System.currentTimeMillis(), failure = null, failureMessage = null)
    }

    fun recordFailure(context: Context, workerName: String, error: Throwable?) {
        val message = error?.message ?: error?.javaClass?.simpleName ?: "Unknown error"
        updateStatus(context, workerName, success = null, failure = System.currentTimeMillis(), failureMessage = message)
    }

    fun getStatuses(context: Context): List<WorkerRunStatus> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val names = prefs.getStringSet(KEY_WORKER_NAMES, emptySet()) ?: emptySet()
        return names.map { name ->
            WorkerRunStatus(
                workerName = name,
                lastSuccessAt = prefs.getLong(keyFor(name, "success"), -1L).takeIf { it > 0 },
                lastFailureAt = prefs.getLong(keyFor(name, "failure"), -1L).takeIf { it > 0 },
                lastFailureMessage = prefs.getString(keyFor(name, "failure_message"), null)
            )
        }.sortedBy { it.workerName }
    }

    private fun updateStatus(
        context: Context,
        workerName: String,
        success: Long?,
        failure: Long?,
        failureMessage: String?
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val names = prefs.getStringSet(KEY_WORKER_NAMES, emptySet())?.toMutableSet() ?: mutableSetOf()
        names.add(workerName)
        val editor = prefs.edit()
        editor.putStringSet(KEY_WORKER_NAMES, names)
        success?.let { editor.putLong(keyFor(workerName, "success"), it) }
        failure?.let { editor.putLong(keyFor(workerName, "failure"), it) }
        if (failureMessage != null) {
            editor.putString(keyFor(workerName, "failure_message"), failureMessage)
        }
        editor.apply()
    }

    private fun keyFor(workerName: String, suffix: String): String = "worker_${workerName}_$suffix"
}
