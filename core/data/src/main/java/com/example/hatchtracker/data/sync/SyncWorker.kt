package com.example.hatchtracker.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import com.example.hatchtracker.data.*
import com.example.hatchtracker.model.SyncState
import com.example.hatchtracker.data.models.SyncQueueStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val db: AppDatabase,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val userId = auth.currentUser?.uid ?: return Result.failure()
        val queueDao = db.syncQueueDao()
        val eventDao = db.domainEventDao()

        // Fetch pending syncs
        val pendingSyncs = queueDao.getPendingSyncs(SyncQueueStatus.PENDING, 20)

        if (pendingSyncs.isEmpty()) return Result.success()

        val batch = firestore.batch()
        val successfulSyncs = mutableListOf<com.example.hatchtracker.data.models.SyncQueueEntity>()

        pendingSyncs.forEach { queueEntry ->
            try {
                // 1. Fetch the actual domain event
                val event = eventDao.getByEventId(queueEntry.eventId)
                    ?: throw IllegalStateException("SyncQueue entry refers to missing event: ${queueEntry.eventId}")
                
                // 2. Process the sync (pushing state of affected aggregate)
                if (queueSyncEntryToBatch(userId, event, batch)) {
                    successfulSyncs.add(queueEntry)
                } else {
                    queueDao.removeByEventId(queueEntry.eventId)
                }
                
            } catch (e: Exception) {
                Logger.e(LogTags.SYNC, "Sync failed for event ${queueEntry.eventId}", e)
                
                // Exponential backoff
                val nextRetry = System.currentTimeMillis() + 
                    TimeUnit.MINUTES.toMillis(Math.pow(2.0, queueEntry.attemptCount.toDouble()).toLong())
                
                queueDao.update(
                    queueEntry.copy(
                        attemptCount = queueEntry.attemptCount + 1,
                        lastAttemptAt = System.currentTimeMillis(),
                        nextRetryAt = nextRetry,
                        errorMessage = e.message
                    )
                )
            }
        }

        if (successfulSyncs.isNotEmpty()) {
            try {
                batch.commit().await()
                val now = System.currentTimeMillis()
                successfulSyncs.forEach { entry ->
                    val event = eventDao.getByEventId(entry.eventId)
                    if (event != null) {
                        markEntitySynced(event.aggregateType, event.aggregateId, now)
                    }
                    queueDao.removeByEventId(entry.eventId)
                }
            } catch (e: Exception) {
                Logger.e(LogTags.SYNC, "Batch commit failed", e)
                val now = System.currentTimeMillis()
                successfulSyncs.forEach { entry ->
                    val nextRetry = now + TimeUnit.MINUTES.toMillis(Math.pow(2.0, entry.attemptCount.toDouble()).toLong())
                    queueDao.update(
                        entry.copy(
                            attemptCount = entry.attemptCount + 1,
                            lastAttemptAt = now,
                            nextRetryAt = nextRetry,
                            errorMessage = "Batch failed: " + e.message
                        )
                    )
                }
            }
        }

        return if (queueDao.getPendingSyncs(SyncQueueStatus.PENDING, 1).isNotEmpty()) {
            Result.retry()
        } else {
            Result.success()
        }
    }

    private suspend fun queueSyncEntryToBatch(
        userId: String, 
        event: com.example.hatchtracker.data.models.DomainEventEntity,
        batch: com.google.firebase.firestore.WriteBatch
    ): Boolean {
        val userRef = firestore.collection("users").document(userId)
        
        // Map aggregateType to Firestore collection name
        val collectionName = when (event.aggregateType) {
            "BIRD" -> "birds"
            "FLOCK" -> "flocks"
            "INCUBATION" -> "incubations"
            "PRODUCTION_LOG" -> "eggProduction"
            "BREED_LINE" -> "breedLines"
            "EGG_SALE" -> "eggSales"
            else -> {
                // Some events might not need state syncing (e.g. metadata events)
                Logger.w(LogTags.SYNC, "No state sync mapped for aggregateType: ${event.aggregateType}")
                return false
            }
        }

        val docRef = userRef.collection(collectionName).document(event.aggregateId)
        if (event.eventType.endsWith("_DELETED") || event.eventType.endsWith("_SOFT_DELETED")) {
            batch.delete(docRef)
        } else {
            // UPSERT: Fetch current local state of the aggregate and set to Firestore
            val entity = getEntity(event.aggregateType, event.aggregateId) ?: return false
            batch.set(docRef, entity)
        }
        return true
    }

    private suspend fun getEntity(type: String, id: String): Any? {
        return when (type) {
            "BIRD" -> db.birdDao().getBirdByCloudId(id)
            "FLOCK" -> db.flockDao().getFlockByCloudId(id)
            "INCUBATION" -> db.incubationDao().getIncubationByCloudId(id)
            "PRODUCTION_LOG" -> db.eggProductionDao().getByCloudId(id)
            "BREED_LINE" -> db.eggProductionDao().getBreedLineByCloudId(id)
            "EGG_SALE" -> db.eggSaleDao().getSaleBySyncId(id)
            else -> null
        }
    }

    private suspend fun markEntitySynced(type: String, id: String, serverTime: Long) {
        when (type) {
            "BIRD" -> db.birdDao().confirmSync(id, serverTime)
            "FLOCK" -> db.flockDao().confirmSync(id, serverTime)
            "INCUBATION" -> db.incubationDao().confirmSync(id, serverTime)
            "PRODUCTION_LOG" -> db.eggProductionDao().confirmSync(id, serverTime)
            "BREED_LINE" -> db.eggProductionDao().confirmBreedLineSync(id, serverTime)
            "EGG_SALE" -> db.eggSaleDao().markSynced(id, serverTime)
        }
    }
}

