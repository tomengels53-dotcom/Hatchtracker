package com.example.hatchtracker.data.sync

import androidx.work.*
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import com.example.hatchtracker.data.BirdDao
import com.example.hatchtracker.data.FlockDao
import com.example.hatchtracker.data.IncubationDao
import com.example.hatchtracker.data.mappers.toEntity
import com.example.hatchtracker.data.models.Bird
import com.example.hatchtracker.data.models.Flock
import com.example.hatchtracker.data.models.Incubation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoreDataSyncCoordinator @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val flockDao: FlockDao,
    private val birdDao: BirdDao,
    private val incubationDao: IncubationDao,
    private val workManager: WorkManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var flockListener: ListenerRegistration? = null
    private var birdListener: ListenerRegistration? = null
    private var incubationListener: ListenerRegistration? = null
    private val isSyncing = AtomicBoolean(false)

    fun startObserving() {
        val userId = auth.currentUser?.uid ?: return
        
        // 1. Flocks
        flockListener?.remove()
        flockListener = firestore.collection("users").document(userId).collection("flocks")
            .whereEqualTo("ownerUserId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Logger.e(LogTags.SYNC, "Flock listen error", e)
                    return@addSnapshotListener
                }
                scope.launch {
                    snapshot?.documents?.forEach { doc ->
                        val remote = doc.toObject(Flock::class.java)
                        if (remote != null) processIncomingFlock(remote)
                    }
                }
            }

        // 2. Birds
        birdListener?.remove()
        birdListener = firestore.collection("users").document(userId).collection("birds")
            .whereEqualTo("ownerUserId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Logger.e(LogTags.SYNC, "Bird listen error", e)
                    return@addSnapshotListener
                }
                scope.launch {
                    snapshot?.documents?.forEach { doc ->
                        val remote = doc.toObject(Bird::class.java)
                        if (remote != null) processIncomingBird(remote)
                    }
                }
            }

        // 3. Incubations
        incubationListener?.remove()
        incubationListener = firestore.collection("users").document(userId).collection("incubations")
            .whereEqualTo("ownerUserId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Logger.e(LogTags.SYNC, "Incubation listen error", e)
                    return@addSnapshotListener
                }
                scope.launch {
                    snapshot?.documents?.forEach { doc ->
                        val remote = doc.toObject(Incubation::class.java)
                        if (remote != null) processIncomingIncubation(remote)
                    }
                }
            }
    }

    fun stopObserving() {
        flockListener?.remove()
        birdListener?.remove()
        incubationListener?.remove()
    }

    /**
     * Triggered by Repositories after a local write.
     */
    /**
     * Triggered by Repositories after a local write.
     * Schedules the SyncWorker to process the queue.
     */
    fun triggerPush() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
            
        workManager.enqueueUniqueWork(
            "PushSyncWork",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            syncRequest
        )
        Logger.i(
            LogTags.SYNC,
            "op=triggerPush status=enqueued name=PushSyncWork constraints=${syncRequest.workSpec.constraints}"
        )
    }

    // pushDirtyRecords is now deprecated in favor of SyncWorker
    // Removed to keep coordinator lightweight and focused on incoming listens

    private suspend fun processIncomingFlock(remote: Flock) {
        val local = flockDao.getFlockEntityByCloudId(remote.cloudId)
        if (shouldApplyUpdate(local?.pendingSync == true, local?.serverUpdatedAt, remote.serverUpdatedAt)) {
            flockDao.upsertByCloudId(remote.toEntity().copy(pendingSync = false))
        }
    }

    private suspend fun processIncomingBird(remote: Bird) {
        val local = birdDao.getBirdEntityByCloudId(remote.cloudId)
        if (shouldApplyUpdate(local?.pendingSync == true, local?.serverUpdatedAt, remote.serverUpdatedAt)) {
            birdDao.upsertByCloudId(remote.toEntity().copy(pendingSync = false))
        }
    }

    private suspend fun processIncomingIncubation(remote: Incubation) {
        val local = incubationDao.getIncubationEntityByCloudId(remote.cloudId)
        if (shouldApplyUpdate(local?.pendingSync == true, local?.serverUpdatedAt, remote.serverUpdatedAt)) {
            incubationDao.upsertByCloudId(remote.toEntity().copy(pendingSync = false))
        }
    }

    /**
     * Conflict Resolution Policy (Phase 15 Refinement):
     * 1. If Local is Dirty (pendingSync=true) -> PROTECT LOCAL.
     * 2. If timestamps match -> PREFER CLOUD (Apply Remote).
     * 3. If Remote is newer -> UPDATE.
     */
    private fun shouldApplyUpdate(localIsDirty: Boolean, localServerTs: Long?, remoteServerTs: Long?): Boolean {
        if (localIsDirty) return false // Protect dirty local changes
        
        val localTs = localServerTs ?: 0L
        val remoteTs = remoteServerTs ?: 0L
        
        return remoteTs >= localTs // Tie-breaker: Cloud wins
    }
}

// Temporary extension property to avoid changing Models globally yet, 
// strictly handled inside the upsert logic or manually map.
// Actually, upsertByCloudId in DAOs handles the merging of ID.
// We add a transient flag `updatedBySync` to models? No, models are data classes.
// We just pass `pendingSync = false` in the copy.
