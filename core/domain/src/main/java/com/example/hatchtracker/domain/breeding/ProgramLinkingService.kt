package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.*
import com.example.hatchtracker.domain.repo.FlockRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgramLinkingService @Inject constructor(
    private val planRepository: BreedingProgramRepository,
    private val flockRepository: FlockRepository,
    private val birdRepository: BirdRepository
) {

    suspend fun linkFlock(planId: String, flockCloudId: String, role: LinkRole, genIndex: Int? = null): Result<Unit> {
        val plan = planRepository.getPlan(planId).getOrNull() ?: return Result.failure(Exception("Plan not found"))
        
        // Species guard
        val flock = flockRepository.allActiveFlocks.first().find { it.cloudId == flockCloudId }
            ?: return Result.failure(Exception("Flock not found"))
            
        if (!flock.species.name.equals(plan.planSpecies.name, ignoreCase = true)) {
            return Result.failure(Exception("Flock species mismatch"))
        }

        val newLink = BreedingProgramAssetLink(
            type = AssetType.FLOCK,
            refId = flockCloudId,
            role = role,
            generationIndex = genIndex
        )

        val updatedAssets = plan.linkedAssets.toMutableList()
        updatedAssets.removeIf { it.role == role && it.generationIndex == genIndex }
        updatedAssets.add(newLink)

        return planRepository.updatePlan(plan.copy(linkedAssets = updatedAssets, updatedAt = System.currentTimeMillis()))
    }

    suspend fun linkFlocklet(planId: String, flockletCloudId: String, role: LinkRole, genIndex: Int? = null): Result<Unit> {
        val plan = planRepository.getPlan(planId).getOrNull() ?: return Result.failure(Exception("Plan not found"))
        
        val newLink = BreedingProgramAssetLink(
            type = AssetType.FLOCKLET,
            refId = flockletCloudId,
            role = role,
            generationIndex = genIndex
        )

        val updatedAssets = plan.linkedAssets.toMutableList()
        updatedAssets.removeIf { it.role == role && it.generationIndex == genIndex }
        updatedAssets.add(newLink)

        return planRepository.updatePlan(plan.copy(linkedAssets = updatedAssets, updatedAt = System.currentTimeMillis()))
    }

    suspend fun setMergeMode(planId: String, mode: MergeMode): Result<Unit> {
        val plan = planRepository.getPlan(planId).getOrNull() ?: return Result.failure(Exception("Plan not found"))
        return planRepository.updatePlan(plan.copy(mergeMode = mode, updatedAt = System.currentTimeMillis()))
    }

    suspend fun setSelectedBirds(planId: String, genIndex: Int, birdCloudIds: List<String>): Result<Unit> {
        val plan = planRepository.getPlan(planId).getOrNull() ?: return Result.failure(Exception("Plan not found"))
        
        val updatedAssets = plan.linkedAssets.toMutableList()
        // Remove old pool links for this gen
        updatedAssets.removeIf { it.role == LinkRole.BREEDER_POOL && it.generationIndex == genIndex }
        
        birdCloudIds.forEach { birdId ->
            updatedAssets.add(BreedingProgramAssetLink(
                type = AssetType.BIRD,
                refId = birdId,
                role = LinkRole.BREEDER_POOL,
                generationIndex = genIndex
            ))
        }

        return planRepository.updatePlan(plan.copy(linkedAssets = updatedAssets, updatedAt = System.currentTimeMillis()))
    }

    suspend fun computeBreederPool(planId: String, genIndex: Int): List<String> {
        val plan = planRepository.getPlan(planId).getOrNull() ?: return emptyList()
        
        return if (plan.mergeMode == MergeMode.MERGE) {
            // Pool = user-selected birds only
            plan.linkedAssets.filter { it.role == LinkRole.BREEDER_POOL && it.generationIndex == genIndex }
                .map { it.refId }
        } else {
            // KEEP_SEPARATE: Pool = all birds in both linked flocks (Sire/Dam sources)
            val sourceFlockIds = plan.linkedAssets.filter { 
                (it.role == LinkRole.SIRE_SOURCE || it.role == LinkRole.DAM_SOURCE) && it.generationIndex == genIndex && it.type == AssetType.FLOCK
            }.map { it.refId }
            
            val allBirds = birdRepository.allBirds.first()
            val birdsInFlocks = allBirds
                .filter { it.flockId?.toString() in sourceFlockIds }
                .map { it.cloudId }
            
            // Also include specifically selected birds if any
            val selectedBirds = plan.linkedAssets.filter { it.role == LinkRole.BREEDER_POOL && it.generationIndex == genIndex }
                .map { it.refId }
                
            (birdsInFlocks + selectedBirds).distinct()
        }
    }
}
