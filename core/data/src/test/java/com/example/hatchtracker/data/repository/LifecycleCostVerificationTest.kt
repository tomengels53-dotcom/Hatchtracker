package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.models.Incubation
import com.example.hatchtracker.domain.pricing.unitcost.UnitCostResult
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LifecycleCostVerificationTest {

    private val financialRepository = mockk<FinancialRepository>()
    private val eggProductionDao = mockk<com.example.hatchtracker.data.EggProductionDao>()
    private val incubationRepository = mockk<IncubationRepository>()
    private val assetDao = mockk<com.example.hatchtracker.data.AssetDao>()

    private lateinit var unitCostProvider: UnitCostProviderImpl

    @Before
    fun setup() {
        unitCostProvider = UnitCostProviderImpl(
            financialRepository,
            eggProductionDao,
            incubationRepository,
            assetDao
        )
    }

    @Test
    fun `CostBasisDriftTest - Frozen basis remains stable regardless of financial repo changes`() = runBlocking {
        // Given a frozen incubation
        val frozenIncubation = Incubation(
            id = 1L,
            syncId = "frozen_1",
            species = "CHICKEN",
            startDate = "2024-01-01",
            expectedHatch = "2024-01-22",
            eggsCount = 10,
            hatchedCount = 5,
            isCostFrozen = true,
            costBasisCents = 1000L // $10.00
        )
        coEvery { incubationRepository.getIncubationBySyncId("frozen_1") } returns frozenIncubation

        // When requesting unit cost
        val result = unitCostProvider.getChickUnitCost("frozen_1") as UnitCostResult.Available

        // Then cost should match the frozen value ($10.00 / 5 = $2.00)
        assertEquals(2.0, result.unitCost, 0.001)
        assertEquals(1000L, result.totalCostCents)
        
        // Even if we mock financial repo to return different values, it shouldn't matter
        coEvery { financialRepository.getSumCents(any(), any(), any(), any(), any()) } returns 50000L
        
        val secondResult = unitCostProvider.getChickUnitCost("frozen_1") as UnitCostResult.Available
        assertEquals(2.0, secondResult.unitCost, 0.001) // Still 2.0
    }

    @Test
    fun `DepreciationScalingTest - Small assets allocate proportional cents`() = runBlocking {
        // Given an asset worth $36.50 and 12-month useful life
        // UnitCostProviderImpl uses 30-day months, so 21 days allocates $2.12
        val asset = com.example.hatchtracker.data.models.AssetEntity(
            purchasePrice = 36.50,
            residualValue = 0.0,
            usefulLifeMonths = 12
        )
        coEvery { assetDao.getActiveAssetByDeviceId("incubator_1") } returns asset
        
        val incubation = Incubation(
            id = 2L,
            syncId = "test_2",
            species = "CHICKEN",
            startDate = "2024-01-01",
            expectedHatch = "2024-01-22",
            eggsCount = 10,
            hatchedCount = 10,
            incubatorDeviceId = "incubator_1"
        )
        coEvery { incubationRepository.getIncubationBySyncId("test_2") } returns incubation
        
        // Mock other costs as 0
        coEvery { financialRepository.getSumCents(any(), any(), any(), any(), any()) } returns 0L
        coEvery { eggProductionDao.getSumTotalEggs(any(), any(), any(), any()) } returns 0 // (egg cost defaulting to 15c)

        val result = unitCostProvider.getChickUnitCost("test_2") as UnitCostResult.Available
        
        // Breakdown index 2 is Asset Depreciation in my implementation
        val depreciationLine = result.breakdown.find { it.label == "Asset Depreciation" }
        assertEquals(2.12, depreciationLine?.amount ?: 0.0, 0.001)
    }
}
