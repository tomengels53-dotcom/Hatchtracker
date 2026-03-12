package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.*
import com.example.hatchtracker.domain.repo.FlockRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlockGraduationService @Inject constructor(
    private val planRepository: BreedingProgramRepository,
    private val flockRepository: FlockRepository,
    private val flockletReadRepository: FlockletReadRepository,
    private val authProvider: AuthProvider
) : FlockletGraduationListener {

    override suspend fun onFlockletGraduated(flockletLocalId: Long, targetFlockLocalId: Long) {
        val userId = authProvider.currentUserId ?: return
        val flocklet = flockletReadRepository.getFlockletById(flockletLocalId) ?: return
        val targetFlock = flockRepository.getFlockById(targetFlockLocalId) ?: return
        
        val flockletCloudId = flocklet.cloudId
        val targetFlockCloudId = targetFlock.cloudId
        
        // Find all plans referencing this flocklet
        val allPlans = planRepository.observePlans(userId).first()
        
        allPlans.filter { plan ->
            plan.linkedAssets.any { it.type == AssetType.FLOCKLET && it.refId == flockletCloudId }
        }.forEach { plan ->
            val updatedAssets = plan.linkedAssets.map { link ->
                if (link.type == AssetType.FLOCKLET && link.refId == flockletCloudId) {
                    link.copy(
                        type = AssetType.FLOCK,
                        refId = targetFlockCloudId,
                        status = LinkStatus.ACTIVE
                    )
                } else {
                    link
                }
            }
            
            val auditLog = plan.auditLog + BreedingProgramAuditEntry(
                message = "Flocklet graduated ($flockletCloudId) -> updated to FLOCK link ($targetFlockCloudId)."
            )
            
            planRepository.updatePlan(
                plan.copy(
                    linkedAssets = updatedAssets,
                    auditLog = auditLog,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
