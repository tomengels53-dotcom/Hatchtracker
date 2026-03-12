package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.catalog.DeviceCatalog
import com.example.hatchtracker.model.CatalogDevice
import com.example.hatchtracker.model.DeviceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceCatalogRepository @Inject constructor() {

    /**
     * Returns all supported incubator (Setter) models.
     */
    fun getIncubatorModels(): Flow<List<CatalogDevice>> = getModelsByType(DeviceType.SETTER)

    /**
     * Returns all supported hatcher models.
     */
    fun getHatcherModels(): Flow<List<CatalogDevice>> = getModelsByType(DeviceType.HATCHER)

    /**
     * Returns all catalog models.
     */
    fun getAllModels(): Flow<List<CatalogDevice>> = flow {
        emit(DeviceCatalog.getAllModels())
    }

    /**
     * Returns catalog models based on requested device type.
     */
    fun getModelsByType(type: DeviceType): Flow<List<CatalogDevice>> = flow {
        val models = DeviceCatalog.OFFICIAL_MODELS.filter { it.deviceType == type }
        emit(models)
    }

    /**
     * Finds a specific model definition by ID.
     */
    suspend fun getModelById(modelId: String): CatalogDevice? {
        return DeviceCatalog.getModel(modelId)
    }
}

