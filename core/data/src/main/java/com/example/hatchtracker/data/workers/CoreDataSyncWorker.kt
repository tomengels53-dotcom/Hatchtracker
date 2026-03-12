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
import com.example.hatchtracker.data.sync.CoreDataSyncCoordinator
import com.google.firebase.auth.FirebaseAuth
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class CoreDataSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val flockDao: FlockDao,
    private val birdDao: BirdDao,
    private val incubationDao: IncubationDao,
    private val syncCoordinator: CoreDataSyncCoordinator,
    private val auth: FirebaseAuth
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val user = auth.currentUser
        val userId = user?.uid
        if (userId == null) {
            Logger.d(LogTags.SYNC, "SyncWorker skipped: No user logged in")
            return@withContext Result.success()
        }

        try {
            Logger.i(LogTags.SYNC, "Detailed Sync check started for user $userId")

            // 1. Claim Ownership (Migration for legacy data)
            // Efficiently batch update NULL owners to current UID
            flockDao.claimOwnership(userId)
            birdDao.claimOwnership(userId)
            incubationDao.claimOwnership(userId)

            // 2. Trigger Push
            // Coordinator will pick up all dirty records (which includes the just-claimed ones if they were dirty)
            // Note: MIGRATION_4_5 sets pendingSync=1 by default, so they are dirty.
            syncCoordinator.triggerPush()

            Logger.i(LogTags.SYNC, "SyncWorker completed successfully")
            Result.success()
        } catch (e: Exception) {
            Logger.e(LogTags.SYNC, "SyncWorker failed", e)
            Result.retry()
        }
    }
}
