package com.example.hatchtracker.domain.support

import com.example.hatchtracker.data.models.SupportDiagnostics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SupportDiagnosticsFormatter {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun format(diagnostics: SupportDiagnostics): String {
        val builder = StringBuilder()
        builder.appendLine("HatchBase Diagnostics")
        builder.appendLine("Generated: ${formatDate(diagnostics.generatedAt)}")
        builder.appendLine("App: ${diagnostics.appVersionName} (${diagnostics.appVersionCode})")
        builder.appendLine("Device: ${diagnostics.deviceModel}")
        builder.appendLine("Android: ${diagnostics.androidVersion}")
        builder.appendLine("UserId: ${diagnostics.userId}")
        builder.appendLine("Subscription: ${diagnostics.subscriptionTier}")
        builder.appendLine()
        builder.appendLine("Worker Status")
        if (diagnostics.workerRuns.isEmpty()) {
            builder.appendLine("- none")
        } else {
            diagnostics.workerRuns.forEach { status ->
                val success = status.lastSuccessAt?.let { formatDate(it) } ?: "n/a"
                val failure = status.lastFailureAt?.let { formatDate(it) } ?: "n/a"
                val failureMsg = status.lastFailureMessage ?: "n/a"
                builder.appendLine("- ${status.workerName}: success=$success failure=$failure error=$failureMsg")
            }
        }
        builder.appendLine()
        builder.appendLine("Recent Logs")
        if (diagnostics.logLines.isEmpty()) {
            builder.appendLine("- none")
        } else {
            diagnostics.logLines.forEach { line ->
                builder.appendLine(line)
            }
        }
        return builder.toString()
    }

    private fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }
}


