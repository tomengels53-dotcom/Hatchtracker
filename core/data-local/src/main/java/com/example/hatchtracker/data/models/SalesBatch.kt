package com.example.hatchtracker.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "sales_batches")
data class SalesBatch(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ownerType: String, // "flock" or "flocklet"
    val ownerId: String,   // syncId of the owner

    val itemType: String,  // "egg", "chick", "adult"
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double,

    val buyerType: String, // "market", "breeder", "private"
    val buyerName: String? = null,

    val saleDate: Long,    // Timestamp
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    
    val syncId: String = UUID.randomUUID().toString(),
    val lastUpdated: Long = System.currentTimeMillis()
)

