package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.AppDatabase
import com.example.hatchtracker.data.EggProductionDao
import com.example.hatchtracker.data.EggReservationDao
import com.example.hatchtracker.data.models.EggProductionEntity
import com.example.hatchtracker.data.models.EggReservationEntity
import com.example.hatchtracker.data.sync.CoreDataSyncCoordinator
import com.example.hatchtracker.domain.pricing.UnitCostProvider
import com.example.hatchtracker.data.DomainEventLogger
import com.google.firebase.auth.FirebaseAuth
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

class EggReservationRepositoryTest {

    private val db = mockk<AppDatabase>(relaxed = true)
    private val dao = mockk<EggProductionDao>(relaxed = true)
    private val resDao = mockk<EggReservationDao>(relaxed = true)
    private val syncCoordinator = mockk<CoreDataSyncCoordinator>(relaxed = true)
    private val unitCostProvider = mockk<UnitCostProvider>(relaxed = true)
    private val domainEventLogger = mockk<DomainEventLogger>(relaxed = true)
    private val auth = mockk<FirebaseAuth>(relaxed = true)

    private lateinit var repository: EggProductionRepository

    @Before
    fun setup() {
        every { db.eggReservationDao() } returns resDao
        repository = EggProductionRepository(
            db, dao, syncCoordinator, unitCostProvider, domainEventLogger, auth
        )
    }

    @Test
    fun `reserveEggsForIncubation allocates eggs FIFO across logs`() {
        runBlocking {
            // Given
            val flockId = "flock1"
            val incubationId = 100L
            val logs = listOf(
                EggProductionEntity(id = "log1", flockId = flockId, dateEpochDay = 1, totalEggs = 2, crackedEggs = 0, setForIncubation = 0),
                EggProductionEntity(id = "log2", flockId = flockId, dateEpochDay = 2, totalEggs = 1, crackedEggs = 0, setForIncubation = 0),
                EggProductionEntity(id = "log3", flockId = flockId, dateEpochDay = 3, totalEggs = 5, crackedEggs = 0, setForIncubation = 0)
            )
            coEvery { dao.getAvailableLogsForFlock(flockId) } returns logs

            // When: Request 4 eggs
            repository.reserveEggsForIncubation(flockId, incubationId, 4)

            // Then:
            // Log 1: take 2
            coVerify { resDao.insertReservation(match { it.productionLogId == "log1" && it.reservedCount == 2 }) }
            coVerify { dao.incrementSetForIncubation("log1", 2, any()) }
            
            // Log 2: take 1
            coVerify { resDao.insertReservation(match { it.productionLogId == "log2" && it.reservedCount == 1 }) }
            coVerify { dao.incrementSetForIncubation("log2", 1, any()) }

            // Log 3: take 1
            coVerify { resDao.insertReservation(match { it.productionLogId == "log3" && it.reservedCount == 1 }) }
            coVerify { dao.incrementSetForIncubation("log3", 1, any()) }
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `reserveEggsForIncubation throws if not enough eggs available`() {
        runBlocking {
            // Given
            val flockId = "flock1"
            val logs = listOf(
                EggProductionEntity(id = "log1", flockId = flockId, dateEpochDay = 1, totalEggs = 2, crackedEggs = 0, setForIncubation = 0)
            )
            coEvery { dao.getAvailableLogsForFlock(flockId) } returns logs

            // When: Request 4 eggs (only 2 available)
            repository.reserveEggsForIncubation(flockId, 200L, 4)
        }
    }

    @Test
    fun `releaseEggsForIncubation restores production logs and deletes reservations`() {
        runBlocking {
            // Given
            val incubationId = 300L
            val reservations = listOf(
                EggReservationEntity(id = 1, incubationId = incubationId, productionLogId = "log1", reservedCount = 2),
                EggReservationEntity(id = 2, incubationId = incubationId, productionLogId = "log2", reservedCount = 1)
            )
            coEvery { resDao.getReservationsByIncubation(incubationId) } returns reservations

            // When
            repository.releaseEggsForIncubation(incubationId)

            // Then
            coVerify { dao.decrementSetForIncubation("log1", 2, any()) }
            coVerify { dao.decrementSetForIncubation("log2", 1, any()) }
            coVerify { resDao.deleteReservationsByIncubation(incubationId) }
        }
    }
}
