package com.example.hatchtracker.billing

import com.example.hatchtracker.data.repository.DeviceRepository
import com.example.hatchtracker.model.DeviceType
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Evaluates whether a user can add or edit a device based on their subscription limits.
 * Handles creation, intra-bucket edits, and cross-bucket edits.
 */
class EvaluateEquipmentAccessUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val subscriptionStateManager: SubscriptionStateManager
) {
    /**
     * @param targetType The [DeviceType] the user wants to set.
     * @param originalDeviceId The ID of the device if this is an edit operation.
     * @param originalType The original [DeviceType] if this is an edit operation.
     */
    suspend fun invoke(
        targetType: DeviceType,
        originalDeviceId: String? = null,
        originalType: DeviceType? = null
    ): EquipmentAccessResult {
        
        // 1. Check privileged roles (Admin/Dev) -> Bypass all limits
        val isAdmin = subscriptionStateManager.isAdmin.value
        val isDeveloper = subscriptionStateManager.isDeveloper.value
        
        if (isAdmin || isDeveloper) {
            return EquipmentAccessResult(
                allowed = true,
                bucket = targetType.bucket,
                currentCount = 0,
                maxAllowed = null,
                formattedMessage = null
            )
        }

        // 2. Handle same-bucket edits -> Bypass limit check
        if (originalDeviceId != null && originalType?.bucket == targetType.bucket) {
            return EquipmentAccessResult(
                allowed = true,
                bucket = targetType.bucket,
                currentCount = 0,
                maxAllowed = null, 
                formattedMessage = null
            )
        }

        // 3. Evaluate Target Bucket Limit
        val bucket = targetType.bucket
        val currentCount = deviceRepository.countActiveDevicesByBucket(bucket).first()
        val capabilities = subscriptionStateManager.currentCapabilities.value
        val tierName = capabilities.tier.name
        
        val maxAllowed = when (bucket) {
            EquipmentLimitBucket.INCUBATION_CORE -> capabilities.maxIncubationEquipment
            EquipmentLimitBucket.BROODING -> capabilities.maxBroodingEquipment
            EquipmentLimitBucket.HOUSING -> capabilities.maxHousingEquipment
            EquipmentLimitBucket.CARE -> capabilities.maxCareEquipment
            EquipmentLimitBucket.MONITORING -> capabilities.maxMonitoringEquipment
        }

        val allowed = maxAllowed == null || currentCount < maxAllowed

        val message = if (!allowed) {
            val plural = if (maxAllowed == 1) "" else "s"
            "Your $tierName plan allows up to $maxAllowed ${bucket.label.lowercase()} device$plural."
        } else null

        return EquipmentAccessResult(
            allowed = allowed,
            bucket = bucket,
            currentCount = currentCount,
            maxAllowed = maxAllowed,
            formattedMessage = message
        )
    }
}
