package com.example.hatchtracker.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.hatchtracker.data.repository.FinancialRepository
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class FinancialRecurrenceWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val financialRepository: FinancialRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Logger.i(LogTags.FINANCE, "Starting recurring entries processing")
            financialRepository.processRecurringEntries()
            Result.success()
        } catch (e: Exception) {
            Logger.e(LogTags.FINANCE, "Failed to process recurring entries", e)
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<FinancialRecurrenceWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build())
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "financial_recurrence",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            Logger.i(
                LogTags.FINANCE,
                "op=schedule status=updated name=financial_recurrence constraints=${request.workSpec.constraints}"
            )
        }
    }
}
