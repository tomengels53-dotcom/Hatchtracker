package com.example.hatchtracker.data.remote.mappers

import com.example.hatchtracker.data.models.Device
import com.example.hatchtracker.data.models.DeviceFeatures
import com.example.hatchtracker.data.models.DeviceType
import com.example.hatchtracker.data.remote.models.DeviceDocument
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceDocumentMapperTest {

    @Test
    fun `DeviceDocument maps to Device correctly focusing on legacy shape`() {
        // Arrange
        val documentId = "doc123"
        val expectedCapacity = 42
        val expectedType = DeviceType.HATCHER
        
        val doc = DeviceDocument(
            id = documentId,
            userId = "user_abc",
            type = expectedType,
            modelId = "model_x",
            displayName = "My Legacy Unit",
            capacityEggs = expectedCapacity,
            features = DeviceFeatures(
                autoTurn = true,
                autoHumidity = false,
                autoTemperature = true,
                requiresManualLockdown = false,
                supportsExternalHatcher = true
            ),
            createdAt = 1000L,
            isActive = true
        )

        // Act
        val model = doc.toModel()

        // Assert
        assertEquals("ID must map exactly (Firestore DocumentId)", documentId, model.id)
        assertEquals("CapacityEggs invariant must map exactly", expectedCapacity, model.capacityEggs)
        assertEquals("Type invariant must map exactly", expectedType, model.type)
        assertEquals("autoTurn invariant must map exactly", true, model.features.autoTurn)
        assertEquals("autoTemperature invariant must map exactly", true, model.features.autoTemperature)
    }

    @Test
    fun `Device maps to DeviceDocument correctly`() {
        // Arrange
        val model = Device(
            id = "dev_1",
            userId = "user_1",
            type = DeviceType.INCUBATOR,
            modelId = "mod_1",
            displayName = "New Device",
            capacityEggs = 24,
            features = DeviceFeatures(autoTurn = false),
            createdAt = 5000L,
            isActive = false
        )

        // Act
        val doc = model.toDocument()

        // Assert
        assertEquals("dev_1", doc.id)
        assertEquals("user_1", doc.userId)
        assertEquals(DeviceType.INCUBATOR, doc.type)
        assertEquals("mod_1", doc.modelId)
        assertEquals("New Device", doc.displayName)
        assertEquals(24, doc.capacityEggs)
        assertEquals(false, doc.features.autoTurn)
        assertEquals(5000L, doc.createdAt)
        assertEquals(false, doc.isActive)
    }
}
