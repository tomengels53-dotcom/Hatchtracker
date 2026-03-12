package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.FinancialDao
import com.example.hatchtracker.data.BirdEntityDao
import com.example.hatchtracker.data.IncubationEntityDao
import com.example.hatchtracker.data.mappers.toEntity
import com.example.hatchtracker.data.mappers.toModel
import com.example.hatchtracker.data.models.FinancialEntry
import com.example.hatchtracker.data.models.FinancialSummary
import com.example.hatchtracker.model.FinancialStats
import com.example.hatchtracker.model.EnrichedFinancialStats
import com.example.hatchtracker.model.FinancialTrustLevel
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinancialRepository @Inject constructor(
    private val financialDao: FinancialDao,
    private val birdDao: BirdEntityDao,
    private val incubationDao: IncubationEntityDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val domainEventLogger: com.example.hatchtracker.data.DomainEventLogger
) : com.example.hatchtracker.domain.repo.FinancialRepository {
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    suspend fun addEntry(entry: FinancialEntry) {
        if (entry.category.startsWith("SALE_")) {
            throw IllegalStateException("Sale entries must use SalesBatch flow to ensure inventory consistency.")
        }
        financialDao.insertEntry(entry)
        recalculateLocalSummary(entry.ownerId, entry.ownerType)
        
        domainEventLogger.log(
            aggregateType = "FINANCE",
            aggregateId = entry.syncId,
            eventType = "FINANCE_ENTRY_ADDED",
            payloadJson = """{"ownerId": "${entry.ownerId}", "ownerType": "${entry.ownerType}", "amount": ${entry.amount}}"""
        )

        val userId = auth.currentUser?.uid ?: return
        repositoryScope.launch {
            try {
                firestore.collection("users")
                    .document(userId)
                    .collection("financialEntries")
                    .document(entry.syncId)
                    .set(entry)
                    .await()
            } catch (e: Exception) {
                Logger.e(LogTags.FINANCE, "Failed to sync entry: ${entry.syncId}", e)
            }
        }
    }

    fun getEntriesForOwner(ownerId: String, ownerType: String): Flow<List<FinancialEntry>> {
        return financialDao.getEntriesForOwnerFlow(ownerId, ownerType)
    }

    suspend fun deleteEntry(entry: FinancialEntry) {
        financialDao.deleteEntry(entry)
        recalculateLocalSummary(entry.ownerId, entry.ownerType)

        domainEventLogger.log(
            aggregateType = "FINANCE",
            aggregateId = entry.syncId,
            eventType = "FINANCE_ENTRY_DELETED",
            payloadJson = """{"ownerId": "${entry.ownerId}", "syncId": "${entry.syncId}"}"""
        )

        val userId = auth.currentUser?.uid ?: return
        repositoryScope.launch {
            try {
                firestore.collection("users")
                    .document(userId)
                    .collection("financialEntries")
                    .document(entry.syncId)
                    .delete()
                    .await()
            } catch (e: Exception) {
                Logger.e(LogTags.FINANCE, "Failed to delete entry: ${entry.syncId}", e)
            }
        }
    }

    fun getEntriesForTypeInDateRange(ownerType: String, startDate: Long, endDate: Long): Flow<List<FinancialEntry>> {
        return financialDao.getEntriesForTypeInDateRange(ownerType, startDate, endDate)
    }

    fun getAllEntriesInDateRange(startDate: Long, endDate: Long): Flow<List<FinancialEntry>> {
        return financialDao.getAllEntriesInDateRange(startDate, endDate)
    }

    suspend fun getIncubationCosts(): List<FinancialEntry> {
        return financialDao.getCostsForOwnerType("incubation")
    }

    suspend fun getSumCostsForOwnerInRange(ownerId: String, ownerType: String, startDate: Long, endDate: Long): Double {
        return financialDao.getSumCents(
            ownerType = ownerType,
            ownerId = ownerId,
            startDate = startDate,
            endDate = endDate,
            type = "cost"
        ).first().toDouble() / 100.0
    }

    fun observeSumCents(ownerType: String?, ownerId: String?, startDate: Long, endDate: Long, type: String): Flow<Long> {
        return financialDao.getSumCents(ownerType, ownerId, startDate, endDate, type)
    }

    suspend fun getSumCents(ownerType: String, ownerId: String, startDate: Long, endDate: Long, type: String): Long {
        return financialDao.getSumCents(ownerType, ownerId, startDate, endDate, type).first()
    }

    fun observeFinancialSummary(ownerId: String, ownerType: String): Flow<FinancialSummary?> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            // If not logged in, just emit null or close
            close()
            return@callbackFlow
        }

        val summaryId = "${ownerType}_${ownerId}"
        val registration = firestore.collection("users")
            .document(userId)
            .collection("financialSummaries")
            .document(summaryId)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    launch(Dispatchers.IO) {
                        val summary = snapshot.toObject(FinancialSummary::class.java)
                        if (summary != null) {
                            financialDao.insertOrUpdateSummary(summary.toEntity())
                        }
                    }
                }
            }
        
        // Connect to local DB flow to emit values
        val localJob = repositoryScope.launch {
            financialDao.getSummaryForOwnerFlow(ownerId, ownerType).collect {
                trySend(it?.toModel())
            }
        }

        awaitClose {
            registration.remove()
            localJob.cancel()
        }
    }.flowOn(Dispatchers.IO).distinctUntilChanged()

    fun getFilteredEntries(
        ownerType: String?, 
        ownerId: String?, 
        startDate: Long, 
        endDate: Long
    ): Flow<List<FinancialEntry>> {
        return financialDao.getFilteredEntries(ownerType, ownerId, startDate, endDate)
    }

    override fun getAggregatedStats(
        ownerType: String?, 
        ownerId: String?, 
        startDate: Long, 
        endDate: Long
        ): Flow<FinancialStats> {
        return getFilteredEntries(ownerType, ownerId, startDate, endDate).map { entries ->
            val costGross = entries.filter { it.type == "cost" }.sumOf { it.amountGross ?: it.amount }
            val costNet = entries.filter { it.type == "cost" }.sumOf { it.amountNet ?: it.amount }
            val revenueGross = entries.filter { it.type == "revenue" }.sumOf { it.amountGross ?: it.amount }
            
            val profit = revenueGross - costGross
            val roi = if (costNet > 0.0) (profit / costNet) * 100.0 else 0.0
            
            FinancialStats(
                totalCost = costGross,
                totalRevenue = revenueGross,
                netProfit = profit,
                avgROI = roi,
                entryCount = entries.size
            )
        }
    }

    override fun getEnrichedStats(ownerType: String, ownerId: String): Flow<EnrichedFinancialStats> {
        return getAggregatedStats(ownerType, ownerId, 0, Long.MAX_VALUE).map { baseStats ->
            val entries = financialDao.getEntriesForOwnerSync(ownerId, ownerType)
            val hasCosts = entries.any { it.type == "cost" }
            val hasRevenue = entries.any { it.type == "revenue" }

            val trustLevel = when {
                hasCosts && hasRevenue -> FinancialTrustLevel.HIGH
                hasCosts -> FinancialTrustLevel.ESTIMATED
                else -> FinancialTrustLevel.INSUFFICIENT
            }

            var costPerBird: Double? = null
            var profitPerBatch: Double? = null

            if (ownerType == "flock") {
                val flockId = ownerId.toLongOrNull()
                if (flockId != null) {
                    val birdCount = birdDao.getBirdEntityCountForFlockSync(flockId)
                    if (birdCount > 0) {
                        costPerBird = baseStats.totalCost / birdCount
                    }
                }
            } else if (ownerType == "incubation") {
                profitPerBatch = baseStats.netProfit
            }

            EnrichedFinancialStats(
                baseStats = baseStats,
                trustLevel = trustLevel,
                costPerBird = costPerBird,
                profitPerBatch = profitPerBatch
            )
        }.flowOn(Dispatchers.IO)
    }

    suspend fun performFinancialAudit() {
        val rawEntries = financialDao.getAllEntriesSync()
        val calculatedTotals = rawEntries.groupBy { it.ownerType + "_" + it.ownerId }
            .mapValues { (_, entries) ->
                val revenue = entries.filter { it.type == "revenue" }.sumOf { it.amountGross ?: it.amount }
                val costs = entries.filter { it.type == "cost" }.sumOf { it.amountGross ?: it.amount }
                revenue - costs
            }

        val cachedSummaries = financialDao.getAllSummariesSync()

        cachedSummaries.forEach { summary ->
            val expectedProfit = calculatedTotals[summary.summaryId] ?: 0.0
            if (Math.abs(summary.profit - expectedProfit) > 0.01) {
                financialDao.insertOrUpdateSummary(summary.copy(profit = expectedProfit))
            }
        }
    }

    suspend fun processRecurringEntries() {
        val now = System.currentTimeMillis()
        val recurringTemplates = financialDao.getRecurringEntries()

        recurringTemplates.forEach { template ->
            val intervalMs = (template.recurrenceIntervalDays ?: 1) * 24 * 60 * 60 * 1000L
            var lastDate = template.lastRecurrenceDate ?: template.date
            
            var nextDate = lastDate + intervalMs
            var updatedLastDate = template.lastRecurrenceDate

            while (nextDate <= now) {
                val child = template.copy(
                    id = 0,
                    syncId = java.util.UUID.randomUUID().toString(),
                    date = nextDate,
                    isRecurring = false,
                    lastRecurrenceDate = null
                )
                financialDao.insertEntry(child)
                recalculateLocalSummary(child.ownerId, child.ownerType)
                
                updatedLastDate = nextDate
                nextDate += intervalMs
            }

            if (updatedLastDate != null && updatedLastDate != template.lastRecurrenceDate) {
                financialDao.insertEntry(template.copy(lastRecurrenceDate = updatedLastDate))
            }
        }
    }

    suspend fun recordSale(ownerId: String, ownerType: String, amount: Double, note: String) {
        val entry = FinancialEntry(
            ownerId = ownerId,
            ownerType = ownerType,
            amount = amount,
            amountGross = amount,
            amountNet = amount,
            amountVAT = 0.0,
            vatEnabled = false,
            type = "revenue",
            category = "Sale",
            notes = note,
            date = System.currentTimeMillis()
        )
        financialDao.insertEntry(entry)
        recalculateLocalSummary(entry.ownerId, entry.ownerType)
    }

    private suspend fun recalculateLocalSummary(ownerId: String, ownerType: String) {
        try {
            // Optimize with direct DAO sums (Phase 4)
            val costGrossCents = financialDao.getSumCents(ownerType, ownerId, 0, Long.MAX_VALUE, "cost").first()
            val revenueGrossCents = financialDao.getSumCents(ownerType, ownerId, 0, Long.MAX_VALUE, "revenue").first()
            
            val costGross = costGrossCents.toDouble() / 100.0
            val revenueGross = revenueGrossCents.toDouble() / 100.0
            val profit = revenueGross - costGross
            
            val summaryId = "${ownerType}_${ownerId}"
            val existingSummary = financialDao.getSummaryById(summaryId)
            
            val updatedSummary = if (existingSummary != null) {
                existingSummary.copy(
                    totalCosts = costGross,
                    totalRevenue = revenueGross,
                    profit = profit,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                FinancialSummary(
                    summaryId = summaryId,
                    ownerId = ownerId,
                    ownerType = ownerType,
                    totalCosts = costGross,
                    totalRevenue = revenueGross,
                    profit = profit,
                    updatedAt = System.currentTimeMillis()
                ).toEntity()
            }

            financialDao.insertOrUpdateSummary(updatedSummary)
        } catch (e: Exception) {
            Logger.e(LogTags.FINANCE, "Failed to recalculate local summary", e)
        }
    }

    suspend fun getEntryBySyncId(syncId: String): FinancialEntry? {
        return financialDao.getEntryBySyncId(syncId)
    }
}
