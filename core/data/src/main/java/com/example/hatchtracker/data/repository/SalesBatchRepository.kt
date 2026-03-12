package com.example.hatchtracker.data.repository

import androidx.room.withTransaction
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import com.example.hatchtracker.data.AppDatabase
import com.example.hatchtracker.data.models.FinancialEntry
import com.example.hatchtracker.data.models.SalesBatch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SalesBatchRepository @Inject constructor(
    private val database: AppDatabase,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO)
    private val salesBatchDao = database.salesBatchDao()
    private val flockDao = database.flockDao()
    private val flockletDao = database.flockletDao()
    private val birdDao = database.birdDao()
    private val financialDao = database.financialDao()
    private val incubationDao = database.incubationDao()

    suspend fun recordSale(batch: SalesBatch, birdIds: List<Long>? = null): Result<Unit> {
        return try {
            database.withTransaction {
                // 1. Inventory Check & Reduction
                when (batch.itemType) {
                    "chick" -> {
                        val flocklet = flockletDao.getFlockletBySyncId(batch.ownerId)
                            ?: throw Exception("Flocklet not found")
                        
                        // Check if we are selling from Nursery Inventory (Flocklet)
                        if (batch.ownerType == "flocklet") {
                             if (flocklet.chickCount < batch.quantity) {
                                throw Exception("Insufficient chicks in nursery")
                            }
                            flockletDao.updateFlocklet(flocklet.copy(
                                chickCount = flocklet.chickCount - batch.quantity,
                                lastUpdated = System.currentTimeMillis()
                            ))
                        }
                    }
                    "egg" -> {
                        if (batch.ownerType == "incubation") {
                            // Selling an active Incubation (Clutch)
                            // ownerId here is the Incubation SyncId
                            val incubation = incubationDao.getIncubationEntityBySyncId(batch.ownerId)
                                ?: throw Exception("Incubation not found")
                            
                            // Mark as sold/completed
                            // We use a status update or soft delete? 
                            // Using a hack: set hatchCompleted = true, and maybe notes?
                            // Or better: Use a 'sold' flag if it existed.
                            // Since it doesn't, we'll mark it locally as deleted but with a note?
                            // Or perhaps we consider it "Hatch Completed" but with 0 hatched?
                            // Let's go with Soft Delete + Note for now as it removes it from active list.
                             incubationDao.updateIncubationEntity(incubation.copy(
                                deleted = true,
                                notes = (incubation.notes ?: "") + " [SOLD]",
                                localUpdatedAt = System.currentTimeMillis(),
                                pendingSync = true
                            ))
                        } else {
                            // Selling from Flock Inventory
                            val flock = flockDao.getFlockEntityBySyncId(batch.ownerId)
                                ?: throw Exception("Flock not found")
                            if (flock.eggCount < batch.quantity) {
                                throw Exception("Insufficient eggs in flock")
                            }
                            flockDao.updateFlockEntity(flock.copy(
                                eggCount = flock.eggCount - batch.quantity,
                                lastUpdated = System.currentTimeMillis()
                            ))
                        }
                    }
                    "adult" -> {
                        if (birdIds != null && birdIds.isNotEmpty()) {
                            // Specific Birds
                            if (birdIds.size != batch.quantity) {
                                throw Exception("Selected bird count does not match quantity")
                            }
                            birdIds.forEach { id ->
                                val bird = birdDao.getBirdEntityById(id)
                                    ?: throw Exception("Bird $id not found")
                                birdDao.updateBirdEntity(bird.copy(
                                    status = "sold",
                                    lastUpdated = System.currentTimeMillis()
                                ))
                            }
                        } else {
                            // Oldest Birds (Bulk)
                            val birdsToSell = salesBatchDao.getOldestActiveBirds(batch.ownerId, batch.quantity)
                            if (birdsToSell.size < batch.quantity) {
                                throw Exception("Insufficient adult birds in flock")
                            }
                            birdsToSell.forEach { bird ->
                                birdDao.updateBirdEntity(bird.copy(
                                    status = "sold",
                                    lastUpdated = System.currentTimeMillis()
                                ))
                            }
                        }
                    }
                }

                // 2. Insert Sales Batch
                salesBatchDao.insertSalesBatch(batch)

                // 3. Generate Financial Entry
                val financialCategory = when (batch.itemType) {
                    "egg" -> "sale_eggs"
                    "chick" -> "sale_chicks"
                    else -> "sale_adult"
                }

                val entry = FinancialEntry(
                    ownerType = batch.ownerType,
                    ownerId = batch.ownerId,
                    type = "revenue",
                    category = financialCategory,
                    amount = batch.totalPrice,
                    quantity = batch.quantity,
                    date = batch.saleDate,
                    notes = "Batch Sale: ${batch.itemType} x${batch.quantity}. ${batch.notes}",
                    syncId = "sale_${batch.syncId}" // Derived syncId to avoid duplicates
                )
                financialDao.insertEntry(entry)
            }

            // 4. Sync to Firestore (Background)
            syncToFirestore(batch)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun syncToFirestore(batch: SalesBatch) {
        val userId = auth.currentUser?.uid ?: return
        repositoryScope.launch {
            try {
                firestore.collection("users")
                    .document(userId)
                    .collection("salesBatches")
                    .document(batch.syncId)
                    .set(batch)
                    .await()
            } catch (e: Exception) {
                Logger.e(LogTags.FINANCE, "Failed to sync sale: ${batch.syncId}", e)
            }
        }
    }

    suspend fun getAverageSalePrice(itemType: String): Double? {
        return salesBatchDao.getAverageSalePrice(itemType)
    }

    suspend fun getAllBatches(): List<SalesBatch> {
        return salesBatchDao.getAllBatches()
    }
}
