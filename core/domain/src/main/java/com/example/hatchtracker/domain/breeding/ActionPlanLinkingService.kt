package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.LinkRole
import com.example.hatchtracker.data.models.MergeMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backward-compatible wrapper for modules still referencing the legacy service name.
 */
@Singleton
class ActionPlanLinkingService @Inject constructor(
    private val delegate: ProgramLinkingService
) {
    suspend fun linkFlock(
        planId: String,
        flockCloudId: String,
        role: LinkRole,
        genIndex: Int? = null
    ): Result<Unit> = delegate.linkFlock(planId, flockCloudId, role, genIndex)

    suspend fun linkFlocklet(
        planId: String,
        flockletCloudId: String,
        role: LinkRole,
        genIndex: Int? = null
    ): Result<Unit> = delegate.linkFlocklet(planId, flockletCloudId, role, genIndex)

    suspend fun setMergeMode(planId: String, mode: MergeMode): Result<Unit> =
        delegate.setMergeMode(planId, mode)

    suspend fun setSelectedBirds(
        planId: String,
        genIndex: Int,
        birdCloudIds: List<String>
    ): Result<Unit> = delegate.setSelectedBirds(planId, genIndex, birdCloudIds)

    suspend fun computeBreederPool(planId: String, genIndex: Int): List<String> =
        delegate.computeBreederPool(planId, genIndex)
}
