package com.example.hatchtracker.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import com.example.hatchtracker.data.service.CostAccountingService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class CostAccountingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val costAccountingService: CostAccountingService
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Logger.i(LogTags.FINANCE, "Starting Cost Accounting Depreciation allocations")
            // Daily check
            costAccountingService.performDailyDepreciation()
            // Monthly check (the service uses yyyy-MM allocation keys, so calling it daily is idempotent)
            costAccountingService.performMonthlyDepreciation()
            
            Result.success()
        } catch (e: Exception) {
            Logger.e(LogTags.FINANCE, "Failed to perform asset depreciation", e)
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            // Schedule to run ideally once a day. Time doesn't matter since services are idempotent.
            val request = PeriodicWorkRequestBuilder<CostAccountingWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build())
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "cost_accounting",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            Logger.i(
                LogTags.FINANCE,
                "op=schedule status=updated name=cost_accounting constraints=${request.workSpec.constraints}"
            )
        }
    }
}
