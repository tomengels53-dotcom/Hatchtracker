package com.example.hatchtracker.feature.support

import android.content.Context
import android.os.Build
import com.example.hatchtracker.core.logging.Logger
import com.example.hatchtracker.core.logging.WorkerRunTracker
import com.example.hatchtracker.data.models.SupportDiagnostics
import com.example.hatchtracker.data.models.SubscriptionTier

object SupportDiagnosticsBuilder {
    fun build(
        context: Context,
        userId: String?,
        subscriptionTier: SubscriptionTier?
    ): SupportDiagnostics {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        @Suppress("DEPRECATION")
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }

        val deviceModel = listOf(Build.MANUFACTURER, Build.MODEL)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" ")

        return SupportDiagnostics(
            appVersionName = packageInfo.versionName ?: "",
            appVersionCode = versionCode,
            deviceModel = deviceModel,
            androidVersion = Build.VERSION.RELEASE ?: "",
            userId = userId ?: "anonymous",
            subscriptionTier = subscriptionTier?.name ?: "UNKNOWN",
            logLines = Logger.getRecentLogs(200),
            workerRuns = WorkerRunTracker.getStatuses(context)
        )
    }
}

