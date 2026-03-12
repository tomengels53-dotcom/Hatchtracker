package com.example.hatchtracker.data.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import com.example.hatchtracker.data.BirdDao
import com.example.hatchtracker.data.FlockDao
import com.example.hatchtracker.data.IncubationDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class TombstoneCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val flockDao: FlockDao,
    private val birdDao: BirdDao,
    private val incubationDao: IncubationDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Logger.i(LogTags.SYNC, "Starting Tombstone Cleanup")
            
            // Cleanup threshold: 30 days ago
            val cutoff = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)

            val deletedFlocks = flockDao.deleteExpiredTombstones(cutoff)
            val deletedBirds = birdDao.deleteExpiredTombstones(cutoff)
            val deletedIncubations = incubationDao.deleteExpiredTombstones(cutoff)
            
            Logger.i(LogTags.SYNC, "Tombstone Cleanup Complete. Cleaned: Flocks=$deletedFlocks, Birds=$deletedBirds, Incubations=$deletedIncubations")
            Result.success()
        } catch (e: Exception) {
            Logger.e(LogTags.SYNC, "Tombstone Cleanup failed", e)
            Result.failure()
        }
    }
}
