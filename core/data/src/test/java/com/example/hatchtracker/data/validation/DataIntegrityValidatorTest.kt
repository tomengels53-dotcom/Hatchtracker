package com.example.hatchtracker.data.validation

import com.example.hatchtracker.data.AppDatabase
import com.example.hatchtracker.data.EggProductionDao
import com.example.hatchtracker.data.EggReservationDao
import com.example.hatchtracker.data.IncubationDao
import com.example.hatchtracker.data.DomainEventDao
import com.example.hatchtracker.data.models.EggProductionEntity
import com.example.hatchtracker.data.models.IncubationEntity
import com.example.hatchtracker.data.ReservationSummary
import com.example.hatchtracker.data.IncubationSummary
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DataIntegrityValidatorTest {

    private lateinit var db: AppDatabase
    private lateinit var eggProductionDao: EggProductionDao
    private lateinit var eggReservationDao: EggReservationDao
    private lateinit var incubationDao: IncubationDao
    private lateinit var domainEventDao: DomainEventDao
    private lateinit var validator: DataIntegrityValidator

    @Before
    fun setup() {
        db = mockk(relaxed = true)
        eggProductionDao = mockk(relaxed = true)
        eggReservationDao = mockk(relaxed = true)
        incubationDao = mockk(relaxed = true)
        domainEventDao = mockk(relaxed = true)

        coEvery { db.eggProductionDao() } returns eggProductionDao
        coEvery { db.eggReservationDao() } returns eggReservationDao
        coEvery { db.incubationDao() } returns incubationDao
        coEvery { db.domainEventDao() } returns domainEventDao

        validator = DataIntegrityValidator(db)
    }

    @Test
    fun `validateEggReservesAndLogs returns issue when counts mismatch`() = runBlocking {
        // Given
        val logId = "log_1"
        val logs = listOf(
            EggProductionEntity(
                id = logId, 
                flockId = "f1", 
                totalEggs = 10, 
                setForIncubation = 5,
                dateEpochDay = 1000L
            )
        )
        // Aggregation says 3 reserved, but log says 5
        val summaries = listOf(ReservationSummary(logId, 3))

        coEvery { eggProductionDao.getAllLogsSync() } returns logs
        coEvery { eggReservationDao.summarizeAllReservationsByLog() } returns summaries

        // When
        val issues = validator.validateEverything()

        // Then
        val accountingIssues = issues.filter { it.category == "EGG_ACCOUNTING" }
        assertEquals(1, accountingIssues.size)
        assertEquals(DataIntegrityValidator.Severity.CRITICAL, accountingIssues[0].severity)
        assertTrue(accountingIssues[0].message.contains("mismatch"))
    }

    @Test
    fun `validateIncubationsAndReserves returns issue when counts mismatch`() = runBlocking {
        // Given
        val incId = 101L
        val incubations = listOf(
            IncubationEntity(
                id = incId, 
                species = "CHICKEN", 
                eggsCount = 10, 
                startDate = "2024-01-01",
                expectedHatch = "2024-01-22"
            )
        )
        // Aggregation says 12, but incubation says 10
        val summaries = listOf(IncubationSummary(incId, 12))

        coEvery { eggProductionDao.getAllLogsSync() } returns emptyList()
        coEvery { incubationDao.getAllIncubationEntitys() } returns incubations
        coEvery { eggReservationDao.summarizeAllReservationsByLog() } returns emptyList()
        coEvery { eggReservationDao.summarizeAllReservationsByIncubation() } returns summaries
        coEvery { domainEventDao.getRecentEvents(any()) } returns emptyList()

        // When
        val issues = validator.validateEverything()

        // Then
        val incubationIssues = issues.filter { it.category == "INCUBATION_ACCOUNTING" }
        assertEquals(1, incubationIssues.size)
        assertEquals(DataIntegrityValidator.Severity.MAJOR, incubationIssues[0].severity)
    }

    @Test
    fun `validateEverything returns no issues when data is consistent`() = runBlocking {
        // Given
        val logId = "log_1"
        val incId = 1L
        
        coEvery { eggProductionDao.getAllLogsSync() } returns listOf(
            EggProductionEntity(
                id = logId, 
                flockId = "f1", 
                totalEggs = 10, 
                setForIncubation = 5,
                dateEpochDay = 1000L
            )
        )
        coEvery { incubationDao.getAllIncubationEntitys() } returns listOf(
            IncubationEntity(
                id = incId, 
                species = "CHICKEN", 
                eggsCount = 5, 
                startDate = "2024-01-01",
                expectedHatch = "2024-01-22"
            )
        )
        coEvery { eggReservationDao.summarizeAllReservationsByLog() } returns listOf(ReservationSummary(logId, 5))
        coEvery { eggReservationDao.summarizeAllReservationsByIncubation() } returns listOf(IncubationSummary(incId, 5))
        coEvery { domainEventDao.getRecentEvents(any()) } returns emptyList()

        // When
        val issues = validator.validateEverything()

        // Then
        assertTrue(issues.isEmpty())
    }
}
