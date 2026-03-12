package com.example.hatchtracker.data.repository

import com.example.hatchtracker.model.Equipment
import com.example.hatchtracker.model.EquipmentMaintenanceLog
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Facade repository for [Equipment] that delegates to [DeviceRepository].
 * This provides a smooth transition to the new terminology while maintaining
 * 100% backward compatibility with the underlying "devices" Firestore collections.
 */
@Singleton
class EquipmentRepository @Inject constructor(
    private val deviceRepository: DeviceRepository
) : com.example.hatchtracker.domain.repo.EquipmentRepository {
    /**
     * Returns a Flow of all equipment owned by the current user.
     */
    override fun getUserEquipment(): Flow<List<Equipment>> = deviceRepository.getUserDevices()
    
    /**
     * Adds new equipment to the user's fleet.
     */
    suspend fun addEquipment(equipment: Equipment): Result<String> = deviceRepository.addDevice(equipment)
    
    /**
     * Updates existing equipment.
     */
    suspend fun updateEquipment(equipment: Equipment): Result<Unit> = deviceRepository.updateDevice(equipment)
    
    /**
     * Deletes equipment.
     */
    suspend fun deleteEquipment(equipmentId: String): Result<Unit> = deviceRepository.deleteDevice(equipmentId)

    /**
     * Adds a maintenance log for specific equipment.
     */
    suspend fun addMaintenanceLog(log: EquipmentMaintenanceLog): Result<String> = deviceRepository.addMaintenanceLog(log)

    /**
     * Fetches maintenance logs for equipment.
     */
    fun getMaintenanceLogs(equipmentId: String): Flow<List<EquipmentMaintenanceLog>> = deviceRepository.getMaintenanceLogsForDevice(equipmentId)

    /**
     * Deletes a maintenance log.
     */
    suspend fun deleteMaintenanceLog(equipmentId: String, logId: String): Result<Unit> = deviceRepository.deleteMaintenanceLog(equipmentId, logId)
}
