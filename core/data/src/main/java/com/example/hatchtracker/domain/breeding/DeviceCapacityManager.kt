package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.Device
import com.example.hatchtracker.data.repository.IncubationRepository
import com.example.hatchtracker.model.DeviceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class DeviceCapacity(
    val device: Device,
    val totalCapacity: Int,
    val usedCapacity: Int,
    val remainingCapacity: Int
)

@Singleton
class DeviceCapacityManager @Inject constructor(
    private val incubationRepository: IncubationRepository
) {
    /**
     * Calculates capacity for a list of devices based on active incubations.
     */
    fun getCapacityForDevices(devicesFlow: Flow<List<Device>>): Flow<List<DeviceCapacity>> {
        return combine(devicesFlow, incubationRepository.allIncubations) { devices, incubations ->
            
            // Filter active incubations (not hatched, not failed/archived if that logic existed)
            val activeIncubations = incubations.filter { !it.hatchCompleted }
            
            devices.map { device ->
                // Calculate used slots for this device
                // incubations can be linked to incubator OR hatcher
                val used = when (device.type) {
                    DeviceType.SETTER, DeviceType.INCUBATOR -> {
                        activeIncubations
                            .filter { it.incubatorDeviceId == device.id }
                            .sumOf { it.eggsCount }
                    }
                    DeviceType.HATCHER -> {
                        activeIncubations
                            .filter { it.hatcherDeviceId == device.id }
                            .sumOf { it.eggsCount }
                    }
                    DeviceType.TURNER, DeviceType.HUMIDITY_CONTROLLER, DeviceType.THERMOSTAT,
                    DeviceType.BROOD_PLATE, DeviceType.HEAT_LAMP, DeviceType.HEAT_PANEL, DeviceType.BROODER,
                    DeviceType.COOP, DeviceType.NEST_BOX, DeviceType.RUN, DeviceType.COOP_AUTO,
                    DeviceType.FEEDER, DeviceType.WATERER, DeviceType.WASHER, DeviceType.SANITIZER,
                    DeviceType.CANDLER, DeviceType.SCALE, DeviceType.THERMOMETER, DeviceType.HYGROMETER, DeviceType.CAMERA -> 0
                }
                
                // Enforce non-negative remaining
                val remaining = (device.capacityEggs - used).coerceAtLeast(0)

                
                DeviceCapacity(
                    device = device,
                    totalCapacity = device.capacityEggs,
                    usedCapacity = used,
                    remainingCapacity = remaining
                )
            }
        }
    }
}

