package com.example.hatchtracker.billing

import com.example.hatchtracker.data.models.SubscriptionTier
import com.example.hatchtracker.data.repository.DeviceRepository
import com.example.hatchtracker.model.DeviceType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EvaluateEquipmentAccessUseCaseTest {

    private val deviceRepository = mockk<DeviceRepository>()
    private val subscriptionStateManager = mockk<SubscriptionStateManager>()
    private lateinit var useCase: EvaluateEquipmentAccessUseCase

    @Before
    fun setup() {
        useCase = EvaluateEquipmentAccessUseCase(deviceRepository, subscriptionStateManager)
        
        // Default: Not admin/dev
        every { subscriptionStateManager.isAdmin } returns MutableStateFlow(false)
        every { subscriptionStateManager.isDeveloper } returns MutableStateFlow(false)
    }

    @Test
    fun `invoke should allow access if user is admin`() = runBlocking {
        every { subscriptionStateManager.isAdmin } returns MutableStateFlow(true)

        val result = useCase.invoke(DeviceType.SETTER)

        assertTrue(result.allowed)
        assertEquals(null, result.maxAllowed)
    }

    @Test
    fun `invoke should allow same-bucket edit`() = runBlocking {
        val result = useCase.invoke(
            targetType = DeviceType.SETTER,
            originalDeviceId = "dev1",
            originalType = DeviceType.SETTER
        )

        assertTrue(result.allowed)
    }

    @Test
    fun `invoke should block if bucket limit reached`() = runBlocking {
        val capabilities = SubscriptionCapabilities.getForTier(SubscriptionTier.FREE).copy(
            maxIncubationEquipment = 1,
            maxBroodingEquipment = 5,
            maxHousingEquipment = 5,
            maxCareEquipment = 5,
            maxMonitoringEquipment = 3
        )
        every { subscriptionStateManager.currentCapabilities } returns MutableStateFlow(capabilities)
        coEvery { deviceRepository.countActiveDevicesByBucket(EquipmentLimitBucket.INCUBATION_CORE) } returns flowOf(1)

        val result = useCase.invoke(DeviceType.SETTER)

        assertFalse(result.allowed)
        assertEquals(1, result.maxAllowed)
        assertEquals(1, result.currentCount)
        assertTrue(result.formattedMessage?.contains("allows up to 1") == true)
    }

    @Test
    fun `invoke should allow if bucket limit not reached`() = runBlocking {
        val capabilities = SubscriptionCapabilities.getForTier(SubscriptionTier.FREE).copy(
            maxIncubationEquipment = 2
        )
        every { subscriptionStateManager.currentCapabilities } returns MutableStateFlow(capabilities)
        coEvery { deviceRepository.countActiveDevicesByBucket(EquipmentLimitBucket.INCUBATION_CORE) } returns flowOf(1)

        val result = useCase.invoke(DeviceType.SETTER)

        assertTrue(result.allowed)
    }

    @Test
    fun `invoke should allow unlimited if maxAllowed is null`() = runBlocking {
        val capabilities = SubscriptionCapabilities.getForTier(SubscriptionTier.PRO)
        every { subscriptionStateManager.currentCapabilities } returns MutableStateFlow(capabilities)
        coEvery { deviceRepository.countActiveDevicesByBucket(EquipmentLimitBucket.INCUBATION_CORE) } returns flowOf(100)

        val result = useCase.invoke(DeviceType.SETTER)

        assertTrue(result.allowed)
        assertEquals(null, result.maxAllowed)
    }
}
