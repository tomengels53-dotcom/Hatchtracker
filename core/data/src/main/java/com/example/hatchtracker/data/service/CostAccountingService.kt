package com.example.hatchtracker.data.service

import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import com.example.hatchtracker.core.domain.models.AssetAllocationEvent
import com.example.hatchtracker.core.domain.models.CostBasisLedgerEntry
import com.example.hatchtracker.core.domain.models.*
import com.example.hatchtracker.data.models.*
import com.example.hatchtracker.data.repository.AssetAllocationRepository
import com.example.hatchtracker.data.repository.AssetRepository
import com.example.hatchtracker.data.repository.CostBasisRepository
import com.example.hatchtracker.data.repository.IncubationRepository
import com.example.hatchtracker.data.repository.NurseryRepository
import com.example.hatchtracker.data.repository.FlockRepository
import com.example.hatchtracker.data.repository.BirdRepository
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CostAccountingService @Inject constructor(
    private val assetRepo: AssetRepository,
    private val allocationRepo: AssetAllocationRepository,
    private val ledgerRepo: CostBasisRepository,
    private val incubationRepo: IncubationRepository,
    private val nurseryRepo: NurseryRepository,
    private val flockRepo: FlockRepository,
    private val birdRepo: BirdRepository
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("UTC"))
    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM").withZone(ZoneId.of("UTC"))

    suspend fun onIncubationHatchCompleted(incubationId: Long) {
        val incubation = incubationRepo.getIncubationById(incubationId) ?: return
        val flocklet = nurseryRepo.getFlockletByHatchId(incubationId)

        val devicesToAllocate = listOfNotNull(
            incubation.incubatorDeviceId.takeIf { it.isNotBlank() }?.let { it to LedgerSourceType.INCUBATOR_DEPRECIATION },
            incubation.hatcherDeviceId.takeIf { it.isNotBlank() }?.let { it to LedgerSourceType.HATCHER_DEPRECIATION }
        )

        for ((deviceId, ledgerSource) in devicesToAllocate) {
            val asset = assetRepo.getActiveAssetByDeviceId(deviceId)
            if (asset != null && asset.depreciationMethod == DepreciationMethod.CYCLE_BASED) {
                val depPerCycle = DepreciationEngine.calculateCycleDepreciation(asset)
                if (depPerCycle <= 0) continue
                
                val periodKey = "cycle_${asset.cyclesAllocatedCount + 1}"
                val allocationId = "${asset.assetId}_INCUBATION_${incubationId}_${periodKey}"
                
                val existing = allocationRepo.getAllocation(allocationId)
                if (existing == null) {
                    val alloc = AssetAllocationEvent(
                        allocationId = allocationId,
                        assetId = asset.assetId,
                        scopeType = AssetScopeType.INCUBATION,
                        scopeId = incubationId.toString(),
                        periodKey = periodKey,
                        amount = depPerCycle,
                        createdAt = System.currentTimeMillis()
                    )
                    allocationRepo.addAllocation(alloc)
                    
                    val updatedAsset = asset.copy(
                        cyclesAllocatedCount = asset.cyclesAllocatedCount + 1,
                        lastAllocatedAtEpochMs = System.currentTimeMillis()
                    )
                    assetRepo.updateAsset(updatedAsset)

                    if (flocklet != null) {
                        val ledgerEntry = CostBasisLedgerEntry(
                            entryId = UUID.randomUUID().toString(),
                            entityType = LedgerEntityType.FLOCKLET,
                            entityId = flocklet.id.toString(),
                            sourceType = ledgerSource,
                            amount = depPerCycle,
                            createdAt = System.currentTimeMillis()
                        )
                        ledgerRepo.addLedgerEntry(ledgerEntry)
                    }
                }
            }
        }
    }

    suspend fun performDailyDepreciation() {
        // Find active TIME_BASED assets for Brooders
        val assets = assetRepo.getActiveAssets()
            .filter { it.depreciationMethod == DepreciationMethod.TIME_BASED && it.category == AssetCategory.BROODER }

        if (assets.isEmpty()) return

        val activeFlocklets = nurseryRepo.activeFlocklets.firstOrNull() ?: emptyList()
        val numFlocklets = activeFlocklets.size
        if (numFlocklets == 0) return

        val now = Instant.now()
        val todayKey = dateFormatter.format(now)

        for (asset in assets) {
            val montlyDep = DepreciationEngine.calculateMonthlyDepreciation(asset)
            if (montlyDep <= 0) continue
            val dailyDep = montlyDep / 30.0
            
            val allocationId = "${asset.assetId}_NURSERY_ALL_${todayKey}"
            val existing = allocationRepo.getAllocation(allocationId)
            
            if (existing == null) {
                val alloc = AssetAllocationEvent(
                    allocationId = allocationId,
                    assetId = asset.assetId,
                    scopeType = AssetScopeType.FLOCKLET,
                    scopeId = "ALL_ACTIVE",
                    periodKey = todayKey,
                    amount = dailyDep,
                    createdAt = System.currentTimeMillis()
                )
                allocationRepo.addAllocation(alloc)
                
                val updatedAsset = asset.copy(lastAllocatedAtEpochMs = now.toEpochMilli())
                assetRepo.updateAsset(updatedAsset)

                val costPerFlocklet = dailyDep / numFlocklets.toDouble()
                val entries = activeFlocklets.map { f ->
                    CostBasisLedgerEntry(
                        entryId = UUID.randomUUID().toString(),
                        entityType = LedgerEntityType.FLOCKLET,
                        entityId = f.id.toString(),
                        sourceType = LedgerSourceType.BROODER_DEPRECIATION,
                        amount = costPerFlocklet,
                        createdAt = System.currentTimeMillis()
                    )
                }
                ledgerRepo.addLedgerEntries(entries)
            }
        }
    }

    suspend fun performMonthlyDepreciation() {
        val assets = assetRepo.getActiveAssets()
            .filter { it.depreciationMethod == DepreciationMethod.TIME_BASED && it.category == AssetCategory.COOP }

        if (assets.isEmpty()) return

        val activeFlocks = flockRepo.activeFlocks.firstOrNull() ?: emptyList()
        val numFlocks = activeFlocks.size
        if (numFlocks == 0) return

        val now = Instant.now()
        val monthKey = monthFormatter.format(now)

        for (asset in assets) {
            val monthlyDep = DepreciationEngine.calculateMonthlyDepreciation(asset)
            if (monthlyDep <= 0) continue
            
            val allocationId = "${asset.assetId}_FLOCK_ALL_${monthKey}"
            val existing = allocationRepo.getAllocation(allocationId)
            
            if (existing == null) {
                val alloc = AssetAllocationEvent(
                    allocationId = allocationId,
                    assetId = asset.assetId,
                    scopeType = AssetScopeType.FLOCK,
                    scopeId = "ALL_ACTIVE",
                    periodKey = monthKey,
                    amount = monthlyDep,
                    createdAt = System.currentTimeMillis()
                )
                allocationRepo.addAllocation(alloc)
                
                val updatedAsset = asset.copy(lastAllocatedAtEpochMs = now.toEpochMilli())
                assetRepo.updateAsset(updatedAsset)

                // Flocks aren't direct ledger entities for individual tracking, Cost is spread to Birds eventually.
                // For simplicity, we distribute evenly across all Birds in Active Flocks.
                val birdsInActiveFlocks = birdRepo.getAllBirdsSync().filter { b -> activeFlocks.any { f -> f.id == b.flockId } }
                if (birdsInActiveFlocks.isNotEmpty()) {
                    val costPerBird = monthlyDep / birdsInActiveFlocks.size.toDouble()
                    val entries = birdsInActiveFlocks.map { b ->
                        CostBasisLedgerEntry(
                            entryId = UUID.randomUUID().toString(),
                            entityType = LedgerEntityType.BIRD,
                            entityId = b.id.toString(),
                            sourceType = LedgerSourceType.COOP_DEPRECIATION,
                            amount = costPerBird,
                            createdAt = System.currentTimeMillis()
                        )
                    }
                    ledgerRepo.addLedgerEntries(entries)
                }
            }
        }
    }

    suspend fun onFlockletDeath(flockletId: Long, count: Int) {
        val flocklet = nurseryRepo.getFlockletById(flockletId) ?: return
        val currentCount = flocklet.chickCount
        if (currentCount <= 0 || count <= 0) return

        val currentPool = ledgerRepo.getCostBasisTotal(LedgerEntityType.FLOCKLET, flockletId.toString())
        val costPerChick = currentPool / currentCount.toDouble()
        val writeOffAmount = costPerChick * count

        val entry = CostBasisLedgerEntry(
            entryId = UUID.randomUUID().toString(),
            entityType = LedgerEntityType.FLOCKLET,
            entityId = flockletId.toString(),
            sourceType = LedgerSourceType.MORTALITY_ADJUSTMENT,
            amount = -writeOffAmount,
            createdAt = System.currentTimeMillis()
        )
        ledgerRepo.addLedgerEntry(entry)
    }

    suspend fun graduateFlocklet(flockletId: Long, resultingBirdIds: List<Long>) {
        if (resultingBirdIds.isEmpty()) return
        
        val currentPool = ledgerRepo.getCostBasisTotal(LedgerEntityType.FLOCKLET, flockletId.toString())
        if (currentPool <= 0.0) return

        val negativeEntry = CostBasisLedgerEntry(
            entryId = UUID.randomUUID().toString(),
            entityType = LedgerEntityType.FLOCKLET,
            entityId = flockletId.toString(),
            sourceType = LedgerSourceType.GRADUATION_TRANSFER,
            amount = -currentPool,
            createdAt = System.currentTimeMillis()
        )
        ledgerRepo.addLedgerEntry(negativeEntry)

        val costPerBird = currentPool / resultingBirdIds.size.toDouble()
        val entries = resultingBirdIds.map { bId ->
            CostBasisLedgerEntry(
                entryId = UUID.randomUUID().toString(),
                entityType = LedgerEntityType.BIRD,
                entityId = bId.toString(),
                sourceType = LedgerSourceType.GRADUATION_TRANSFER,
                amount = costPerBird,
                createdAt = System.currentTimeMillis()
            )
        }
        ledgerRepo.addLedgerEntries(entries)
    }

    suspend fun lockSalesCogs(sourceType: String, sourceId: String, quantity: Int): Double {
        val ledgerType = when (sourceType.lowercase()) {
            "flocklet", "chick" -> LedgerEntityType.FLOCKLET
            "adult", "bird" -> LedgerEntityType.BIRD
            else -> return 0.0
        }

        val pool = ledgerRepo.getCostBasisTotal(ledgerType, sourceId)
        if (pool <= 0.0) return 0.0

        var costToRemove = pool
        
        if (ledgerType == LedgerEntityType.FLOCKLET) {
            val flocklet = nurseryRepo.getFlockletById(sourceId.toLongOrNull() ?: 0L)
            if (flocklet != null && flocklet.chickCount > 0) {
                val proportion = (quantity.toDouble() / flocklet.chickCount.toDouble()).coerceAtMost(1.0)
                costToRemove = pool * proportion
                
                // Adjust flocklet chick count as per the plan
                val newCount = flocklet.chickCount - quantity
                val toUpdate = flocklet.copy(
                    chickCount = newCount.coerceAtLeast(0),
                    movedToFlockId = if (newCount <= 0) -1 else flocklet.movedToFlockId
                )
                nurseryRepo.updateFlocklet(toUpdate)
            }
        }

        if (costToRemove > 0) {
            val entry = CostBasisLedgerEntry(
                entryId = UUID.randomUUID().toString(),
                entityType = ledgerType,
                entityId = sourceId,
                sourceType = LedgerSourceType.SALE_COGS,
                amount = -costToRemove,
                createdAt = System.currentTimeMillis()
            )
            ledgerRepo.addLedgerEntry(entry)
        }
        
        return costToRemove
    }

    suspend fun retireAsset(assetId: String, retirementValue: Double) {
        val asset = assetRepo.getAsset(assetId) ?: return
        
        val updated = asset.copy(
            status = AssetStatus.RETIRED,
            retiredDateEpochMs = System.currentTimeMillis(),
            retirementValue = retirementValue
        )
        assetRepo.updateAsset(updated)
    }
}
