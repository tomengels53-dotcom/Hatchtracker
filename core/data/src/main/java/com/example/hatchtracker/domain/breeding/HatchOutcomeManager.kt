@file:android.annotation.SuppressLint("NewApi")

package com.example.hatchtracker.domain.breeding

import com.example.hatchtracker.data.models.Bird
import com.example.hatchtracker.data.models.Incubation
import com.example.hatchtracker.data.models.Sex as LocalSex
import com.example.hatchtracker.data.BirdDao
import com.example.hatchtracker.data.IncubationDao
import com.example.hatchtracker.data.mappers.toEntity
import com.example.hatchtracker.data.mappers.toModel
import com.example.hatchtracker.data.repository.NurseryRepository
import com.example.hatchtracker.model.GeneticProfile
import com.example.hatchtracker.domain.breeding.BreedingPredictionService
import com.example.hatchtracker.model.Species
import androidx.room.withTransaction
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

class HatchOutcomeManager @Inject constructor(
    private val database: com.example.hatchtracker.data.AppDatabase, // Inject Database for transactions
    private val birdDao: BirdDao,
    private val incubationDao: IncubationDao,
    private val nurseryRepository: NurseryRepository,
    private val breedingPredictionService: BreedingPredictionService,
    private val unitCostProvider: com.example.hatchtracker.domain.pricing.UnitCostProvider,
    private val lifecycleCostService: com.example.hatchtracker.data.service.LifecycleCostService
) {

    suspend fun processHatch(
        incubation: Incubation,
        hatchedCount: Int,
        infertileCount: Int,
        failedCount: Int,
        hatchDate: LocalDate = LocalDate.now()
    ): Result<List<Bird>> {
        return try {
            // 1. Validation
            if (hatchedCount < 0 || infertileCount < 0 || failedCount < 0) {
                 return Result.failure(IllegalArgumentException("Counts cannot be negative"))
            }

            if ((hatchedCount + infertileCount + failedCount) > incubation.eggsCount) {
                 return Result.failure(IllegalArgumentException("Total outcomes (${hatchedCount + infertileCount + failedCount}) cannot exceed eggs set (${incubation.eggsCount})"))
            }

            database.withTransaction {
                // 2. Fetch Parents for genetics
                val mother = incubation.birdId?.let { birdDao.getBirdEntityById(it) }
                val father = incubation.fatherBirdId?.let { birdDao.getBirdEntityById(it) }
                val motherModel = mother?.toModel()
                val fatherModel = father?.toModel()

                // 3. Predict Genetics
                val predictedGeneticProfile = if (motherModel != null && fatherModel != null) {
                    val species = try {
                        Species.valueOf(incubation.species.uppercase())
                    } catch (e: Exception) {
                        Species.CHICKEN
                    }

                    val result = breedingPredictionService.predictBreeding(
                        species = species,
                        sireProfile = fatherModel.geneticProfile,
                        damProfile = motherModel.geneticProfile
                    )

                    GeneticProfile(
                        inferredTraits = result.phenotypeResult.probabilities.map { p ->
                            "${p.phenotypeId} (${(p.probability * 100).toInt()}%)"
                        },
                        confidenceLevel = if (result.phenotypeResult.probabilities.any { it.probability > 0.8 })
                            com.example.hatchtracker.data.models.ConfidenceLevel.HIGH.name
                            else com.example.hatchtracker.data.models.ConfidenceLevel.MEDIUM.name,
                        genotypeVersion = 2
                    )
                } else {
                    GeneticProfile()
                }

                // 4. Generate Birds
                val newBirds = mutableListOf<Bird>()
                val batchId = System.currentTimeMillis()

                for (i in 1..hatchedCount) {
                    val species = runCatching { Species.valueOf(incubation.species.trim().uppercase()) }
                        .getOrDefault(Species.UNKNOWN)
                    val bird = Bird(
                        syncId = UUID.randomUUID().toString(),
                        species = species,
                        breed = motherModel?.breed ?: incubation.hatchNotes ?: "Mixed", // Basic breed inheritance
                        hatchDate = hatchDate.toString(),
                        motherId = incubation.birdId,
                        fatherId = incubation.fatherBirdId,
                        incubationId = incubation.id,
                        hatchBatchId = batchId,
                        sex = LocalSex.UNKNOWN,
                        flockId = incubation.flockId,
                        geneticProfile = predictedGeneticProfile
                    )
                    newBirds.add(bird)
                }

                // 5. Persist Birds
                newBirds.forEach { birdDao.insertBirdEntity(it.toEntity()) }

                // 6. Calculate and freeze incubation cost basis
                val costResult = unitCostProvider.getChickUnitCost(incubation.syncId)
                val availableResult = costResult as? com.example.hatchtracker.domain.pricing.unitcost.UnitCostResult.Available
                val totalInvestmentCents = availableResult?.totalCostCents ?: 0L
                val assetAllocationCents = availableResult?.breakdown
                    ?.find { it.label == "Asset Depreciation" }
                    ?.amount?.let { (it * 100).toLong() } ?: 0L

                nurseryRepository.ensureFlockletForHatch(
                    incubation = incubation,
                    hatchedCount = hatchedCount,
                    hatchDate = hatchDate,
                    fallbackBreed = motherModel?.breed,
                    costBasisCents = totalInvestmentCents,
                    costBasisSourceRef = "incubation_${incubation.syncId}"
                )

                // 7. Update Incubation & Freeze Cost Basis
                val updatedIncubation = incubation.copy(
                    hatchedCount = hatchedCount,
                    infertileCount = infertileCount,
                    failedCount = failedCount,
                    hatchCompleted = true,
                    actualHatchDate = hatchDate.toString(),
                    lastUpdated = System.currentTimeMillis(),
                    // Lifecycle Costing
                    costBasisCents = totalInvestmentCents,
                    assetAllocationCents = assetAllocationCents,
                    isCostFrozen = true
                )
                incubationDao.updateIncubationEntity(updatedIncubation.toEntity())

                // 8. Record Snapshot Ledger (Phase 3 Guardrails)
                lifecycleCostService.recordFrozenBasis(
                    entityType = com.example.hatchtracker.core.domain.models.LedgerEntityType.INCUBATION,
                    entityId = incubation.syncId,
                    amountCents = totalInvestmentCents,
                    sourceType = if (hatchedCount > 0) com.example.hatchtracker.core.domain.models.LedgerSourceType.DIRECT_COST
                                 else com.example.hatchtracker.core.domain.models.LedgerSourceType.MORTALITY_ADJUSTMENT,
                    userId = incubation.ownerUserId ?: ""
                )

                // 9. Finance Chain: Record Hatch Profit (Asset Value) - Optional legacy support
                if (hatchedCount > 0) {
                    val unitValue = availableResult?.unitCost ?: 0.0
                    val totalValue = unitValue * hatchedCount
                    
                    val financialEntry = com.example.hatchtracker.data.models.FinancialEntry(
                        ownerId = incubation.syncId,
                        ownerType = "incubation",
                        type = "revenue",
                        category = "hatch_value",
                        amount = totalValue,
                        quantity = hatchedCount,
                        date = System.currentTimeMillis(),
                        notes = "Automated asset value for $hatchedCount hatched chicks",
                        syncId = "hatch_${incubation.syncId}" // Deterministic ID
                    )
                    database.financialDao().insertEntry(financialEntry)
                }

                Result.success(newBirds)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

