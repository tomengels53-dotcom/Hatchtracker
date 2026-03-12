package com.example.hatchtracker.notifications.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.hatchtracker.notifications.scheduling.HatchyReminderScheduler
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import com.example.hatchtracker.data.repository.IncubationRepository
import com.example.hatchtracker.data.repository.NurseryRepository
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

class HatchyReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface HatchyReminderWorkerEntryPoint {
        fun incubationRepository(): IncubationRepository
        fun nurseryRepository(): NurseryRepository
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPoints.get(context.applicationContext, HatchyReminderWorkerEntryPoint::class.java)
            val incubationRepository = entryPoint.incubationRepository()
            val nurseryRepository = entryPoint.nurseryRepository()

            val activeIncubations = incubationRepository.getAllIncubations()
            val activeFlocklets = nurseryRepository.activeFlocklets.first()

            val scheduler = HatchyReminderScheduler(context)
            scheduler.checkAndScheduleReminders(activeIncubations, activeFlocklets)

            Result.success()
        } catch (e: Exception) {
            Logger.e(LogTags.NOTIFICATIONS, "op=HatchyReminderWorker status=failed", e)
            Result.retry()
        }
    }
}
