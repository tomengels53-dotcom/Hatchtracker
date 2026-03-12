package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.models.Device
import com.example.hatchtracker.data.remote.mappers.toDocument
import com.example.hatchtracker.data.remote.mappers.toModel
import com.example.hatchtracker.data.remote.models.DeviceDocument
import com.example.hatchtracker.data.remote.models.MaintenanceLogDocument
import com.example.hatchtracker.model.EquipmentMaintenanceLog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val domainEventLogger: com.example.hatchtracker.data.DomainEventLogger
) {
    private val USERS_COLLECTION = "users"
    private val DEVICES_COLLECTION = "devices"

    /**
     * Returns a Flow of all devices owned by the current user.
     */
    fun getUserDevices(): Flow<List<Device>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val collectionRef = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(DEVICES_COLLECTION)

        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    trySend(emptyList())
                } else {
                    close(error)
                }
                return@addSnapshotListener
            }

            if (snapshot != null) {
                // Parsing large snapshots can block the UI thread.
                // Move off-thread using the ProducerScope's launch.
                launch(Dispatchers.IO) {
                    val devices = snapshot.toObjects(DeviceDocument::class.java)
                        .map { it.toModel() }
                    trySend(devices)
                }
            } else {
                trySend(emptyList())
            }
        }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO).distinctUntilChanged()

    /**
     * Adds a new device to the user's fleet.
     * Ensures the userId is strictly enforced.
     */
    suspend fun addDevice(device: Device): Result<String> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        
        return try {
            val deviceRef = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(DEVICES_COLLECTION)
                .document() // Auto-ID

            val newDevice = device.copy(
                id = deviceRef.id,
                userId = userId,
                createdAt = System.currentTimeMillis(),
                isActive = true
            )

            deviceRef.set(newDevice.toDocument()).await()
            Result.success(deviceRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates an existing device.
     */
    suspend fun updateDevice(device: Device): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(DEVICES_COLLECTION)
                .document(device.id)
                .set(device.toDocument(), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a device.
     */
    suspend fun deleteDevice(deviceId: String): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(DEVICES_COLLECTION)
                .document(deviceId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Returns a Flow representing the count of active devices within a specific subscription bucket.
     */
    fun countActiveDevicesByBucket(bucket: com.example.hatchtracker.billing.EquipmentLimitBucket): Flow<Int> {
        return getUserDevices().map { devices ->
            devices.count { it.isActive && it.type.bucket == bucket }
        }.distinctUntilChanged()
    }

    /**
     * Adds a maintenance log for a specific device.
     */
    suspend fun addMaintenanceLog(log: EquipmentMaintenanceLog): Result<String> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        
        return withContext(Dispatchers.IO) {
            try {
                val logRef = firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(DEVICES_COLLECTION)
                    .document(log.equipmentId)
                    .collection("maintenance_logs")
                    .document()

                val finalLog = log.copy(id = logRef.id)
                logRef.set(finalLog.toDocument()).await()

                // Fail-safe maintenance bridge: log domain event for chronology
                try {
                    domainEventLogger.log(
                        aggregateType = "DEVICE",
                        aggregateId = log.equipmentId,
                        eventType = com.example.hatchtracker.data.DomainEventLogger.MAINTENANCE_LOGGED,
                        payloadJson = """{"type": "${log.type}", "description": "${log.description}", "cost": ${log.cost ?: 0.0}}"""
                    )
                } catch (e: Exception) {
                    // Fail silently for history to preserve primary state
                }

                Result.success(logRef.id)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Fetches maintenance logs for a device. Lazy-loaded on demand.
     */
    fun getMaintenanceLogsForDevice(deviceId: String): Flow<List<EquipmentMaintenanceLog>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val collectionRef = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(DEVICES_COLLECTION)
            .document(deviceId)
            .collection("maintenance_logs")

        val listener = collectionRef.orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        // Rules can deny maintenance logs for some users/devices.
                        // Keep UI alive by emitting an empty list instead of crashing collectors.
                        trySend(emptyList())
                    } else {
                        close(error)
                    }
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    launch(Dispatchers.IO) {
                        val logs = snapshot.toObjects(MaintenanceLogDocument::class.java)
                            .map { it.toModel() }
                        trySend(logs)
                    }
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO).distinctUntilChanged()

    /**
     * Deletes a maintenance log.
     */
    suspend fun deleteMaintenanceLog(deviceId: String, logId: String): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        
        return withContext(Dispatchers.IO) {
            try {
                firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(DEVICES_COLLECTION)
                    .document(deviceId)
                    .collection("maintenance_logs")
                    .document(logId)
                    .delete()
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

