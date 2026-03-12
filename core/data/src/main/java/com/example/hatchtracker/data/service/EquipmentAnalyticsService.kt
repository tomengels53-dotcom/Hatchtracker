package com.example.hatchtracker.data.service

import com.example.hatchtracker.data.repository.DeviceRepository
import com.example.hatchtracker.model.EquipmentMaintenanceLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides read-only analytics for equipment.
 * Implementation moved to core:data to access repositories while adhering to dependency graph.
 */
@Singleton
class EquipmentAnalyticsService @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    /**
     * Calculates the maintenance frequency (logs per month) for a device.
     */
    fun getMaintenanceFrequency(deviceId: String): Flow<Double?> {
        return deviceRepository.getMaintenanceLogsForDevice(deviceId).map { logs ->
            if (logs.isEmpty()) return@map 0.0
            
            val sortedLogs = logs.sortedBy { it.date }
            val firstLogDate = sortedLogs.first().date
            val lastLogDate = System.currentTimeMillis()
            
            val durationMs = lastLogDate - firstLogDate
            val months = durationMs / (1000L * 60 * 60 * 24 * 30)
            
            if (months < 1) logs.size.toDouble() else logs.size.toDouble() / months
        }
    }

    /**
     * Calculates the total maintenance cost for a device.
     */
    fun getTotalMaintenanceCost(deviceId: String): Flow<Double> {
        return deviceRepository.getMaintenanceLogsForDevice(deviceId).map { logs ->
            logs.sumOf { it.cost ?: 0.0 }
        }
    }

    /**
     * Utilization rate is currently a placeholder until linked with accounting/incubation cycles.
     */
    fun getUtilizationRate(deviceId: String): Flow<Double?> {
        return deviceRepository.getMaintenanceLogsForDevice(deviceId).map { null }
    }
}
