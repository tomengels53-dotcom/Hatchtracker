package com.example.hatchtracker.data.service

import com.example.hatchtracker.core.domain.models.*
import com.example.hatchtracker.data.models.*
import com.example.hatchtracker.model.Flocklet
import com.example.hatchtracker.model.BirdLifecycleStage
import com.example.hatchtracker.data.repository.*
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class CostAccountingServiceTest {

    private val assetRepo: AssetRepository = mockk(relaxed = true)
    private val allocationRepo: AssetAllocationRepository = mockk(relaxed = true)
    private val ledgerRepo: CostBasisRepository = mockk(relaxed = true)
    private val incubationRepo: IncubationRepository = mockk(relaxed = true)
    private val nurseryRepo: NurseryRepository = mockk(relaxed = true)
    private val flockRepo: FlockRepository = mockk(relaxed = true)
    private val birdRepo: BirdRepository = mockk(relaxed = true)

    private val service = CostAccountingService(
        assetRepo, allocationRepo, ledgerRepo, incubationRepo, nurseryRepo, flockRepo, birdRepo
    )

    @Test
    fun `Cycle depreciation executes correctly on Hatch Completed`() = runTest {
        // Arrange
        val incubationId = 100L
        val incubatorDeviceId = "dev_incubator"
        
        val incubation = Incubation(
            id = incubationId,
            incubatorDeviceId = incubatorDeviceId,
            hatcherDeviceId = "",
            hatchCompleted = true
        )
        val flocklet = Flocklet(id = 200L, hatchId = incubationId, chickCount = 10, species = "Chicken", hatchDate = 1000L)
        
        val incubatorAsset = Asset(
            assetId = "asset_1",
            name = "Incubator",
            linkedDeviceId = incubatorDeviceId,
            category = AssetCategory.INCUBATOR,
            purchaseDateEpochMs = 0L,
            purchasePrice = 1000.0,
            residualValue = 100.0, // Depreciable amount = 900
            depreciationMethod = DepreciationMethod.CYCLE_BASED,
            usefulLifeMonths = null,
            expectedCycles = 10,  // 90 per cycle
            cyclesAllocatedCount = 0,
            lastAllocatedAtEpochMs = 0L,
            retiredDateEpochMs = null,
            retirementValue = null,
            status = AssetStatus.ACTIVE
        )

        coEvery { incubationRepo.getIncubationById(incubationId) } returns incubation
        coEvery { nurseryRepo.getFlockletByHatchId(incubationId) } returns flocklet
        coEvery { assetRepo.getActiveAssetByDeviceId(incubatorDeviceId) } returns incubatorAsset
        coEvery { allocationRepo.getAllocation(any()) } returns null

        val allocationSlot = slot<AssetAllocationEvent>()
        val ledgerSlot = slot<CostBasisLedgerEntry>()
        
        coEvery { allocationRepo.addAllocation(capture(allocationSlot)) } just Runs
        coEvery { ledgerRepo.addLedgerEntry(capture(ledgerSlot)) } just Runs

        // Act
        service.onIncubationHatchCompleted(incubationId)

        // Assert
        assertTrue(allocationSlot.isCaptured)
        assertTrue(ledgerSlot.isCaptured)

        // Depreciation amount should be (1000 - 100) / 10 = 90.0
        assertEquals("Depreciation calculated amount invariant must be strictly 90.0", 90.0, allocationSlot.captured.amount, 0.001)
        assertEquals("Asset scope must be INCUBATION", AssetScopeType.INCUBATION, allocationSlot.captured.scopeType)
        
        assertEquals("Ledger entry amount must match allocation", 90.0, ledgerSlot.captured.amount, 0.001)
        assertEquals("Ledger entity must be FLOCKLET", LedgerEntityType.FLOCKLET, ledgerSlot.captured.entityType)
        assertEquals("Ledger source must be INCUBATOR_DEPRECIATION", LedgerSourceType.INCUBATOR_DEPRECIATION, ledgerSlot.captured.sourceType)
    }

    @Test
    fun `Time based brooder daily depreciation executes correctly`() = runTest {
        // Arrange
        val brooderAsset = Asset(
            assetId = "asset_2",
            name = "Brooder",
            linkedDeviceId = "dev_brooder",
            category = AssetCategory.BROODER,
            purchaseDateEpochMs = 0L,
            purchasePrice = 1200.0, // 100 per month
            residualValue = 0.0,
            depreciationMethod = DepreciationMethod.TIME_BASED,
            usefulLifeMonths = 12,
            expectedCycles = null,
            cyclesAllocatedCount = 0,
            lastAllocatedAtEpochMs = 0L,
            retiredDateEpochMs = null,
            retirementValue = null,
            status = AssetStatus.ACTIVE
        )

        val flocklets = listOf(
            Flocklet(id = 1L, chickCount = 10, species = "Chicken", hatchDate = 0L),
            Flocklet(id = 2L, chickCount = 10, species = "Chicken", hatchDate = 0L)
        )

        coEvery { assetRepo.getActiveAssets() } returns listOf(brooderAsset)
        every { nurseryRepo.activeFlocklets } returns flowOf(flocklets)
        coEvery { allocationRepo.getAllocation(any()) } returns null

        val allocationSlot = slot<AssetAllocationEvent>()
        val ledgerSlot = slot<List<CostBasisLedgerEntry>>()

        coEvery { allocationRepo.addAllocation(capture(allocationSlot)) } just Runs
        coEvery { ledgerRepo.addLedgerEntries(capture(ledgerSlot)) } just Runs

        // Act
        service.performDailyDepreciation()

        // Assert
        assertTrue(allocationSlot.isCaptured)
        assertTrue(ledgerSlot.isCaptured)

        // 1200 / 12 = 100 per month. 100 / 30 = 3.333 per day total
        val expectedDailyTotal = 1200.0 / 12.0 / 30.0
        assertEquals("Daily depreciation total invariant", expectedDailyTotal, allocationSlot.captured.amount, 0.001)

        val entries = ledgerSlot.captured
        assertEquals("Must spread across exactly 2 active flocklets", 2, entries.size)
        
        val expectedPerFlocklet = expectedDailyTotal / 2.0
        assertEquals("Must be roughly 1.666", expectedPerFlocklet, entries[0].amount, 0.001)
        assertEquals(LedgerSourceType.BROODER_DEPRECIATION, entries[0].sourceType)
    }
}
