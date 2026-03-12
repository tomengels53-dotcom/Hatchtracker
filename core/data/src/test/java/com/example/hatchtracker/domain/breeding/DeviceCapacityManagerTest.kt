package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.Device
import com.example.hatchtracker.data.models.DeviceType
import com.example.hatchtracker.data.models.Incubation
import com.example.hatchtracker.data.repository.IncubationRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceCapacityManagerTest {

    private val incubationRepository: IncubationRepository = mockk()
    private val capacityManager = DeviceCapacityManager(incubationRepository)

    @Test
    fun `Capacity logic correctly sums eggs for Incubators ignoring Hatchers`() = runTest {
        // Arrange
        val incubator = Device(id = "inc_1", type = DeviceType.INCUBATOR, capacityEggs = 100)
        val hatcher = Device(id = "hat_1", type = DeviceType.HATCHER, capacityEggs = 50)

        // Two active incubations
        val inc1 = Incubation(id = 1L, incubatorDeviceId = "inc_1", hatcherDeviceId = "hat_1", eggsCount = 20, hatchCompleted = false)
        val inc2 = Incubation(id = 2L, incubatorDeviceId = "inc_1", hatcherDeviceId = "other", eggsCount = 30, hatchCompleted = false)
        // One completed incubation (should be ignored)
        val inc3 = Incubation(id = 3L, incubatorDeviceId = "inc_1", hatcherDeviceId = "hat_1", eggsCount = 40, hatchCompleted = true)

        every { incubationRepository.allIncubations } returns flowOf(listOf(inc1, inc2, inc3))

        // Act
        val capacities = capacityManager.getCapacityForDevices(flowOf(listOf(incubator, hatcher))).first()

        // Assert
        val incCap = capacities.find { it.device.id == "inc_1" }!!
        val hatCap = capacities.find { it.device.id == "hat_1" }!!

        // Incubator: sees inc1 (20) + inc2 (30) = 50 used. inc3 is completed.
        assertEquals("Incubator used capacity invariant must match (active only)", 50, incCap.usedCapacity)
        assertEquals("Incubator remaining capacity invariant must match", 50, incCap.remainingCapacity)

        // Hatcher: sees inc1 (20). inc2 uses 'other', inc3 is completed.
        assertEquals("Hatcher used capacity invariant must match (active only)", 20, hatCap.usedCapacity)
        assertEquals("Hatcher remaining capacity invariant must match", 30, hatCap.remainingCapacity)
    }
}
