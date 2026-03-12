package com.example.hatchtracker.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Records a completed or cancelled egg sale.
 * Revenue and COGS are stored at sale time (snapshot accounting).
 * All monetary values are in integer cents for precision.
 */
@Entity(
    tableName = "egg_sale",
    indices = [
        Index(value = ["flockId", "saleDateEpochDay"]),
        Index(value = ["id", "cancelled"])
    ]
)
data class EggSaleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val flockId: String,

    /** Number of eggs sold. Must be > 0. */
    val quantity: Int,

    /** Date of sale as epoch day (LocalDate.toEpochDay()). */
    val saleDateEpochDay: Long,

    /** Price per egg in cents. >= 0. */
    val pricePerEggCents: Long,

    /** quantity * pricePerEggCents */
    val totalRevenueCents: Long,

    /** Derived cost per egg from 30-day flock cost window (BigDecimal rounded). */
    val derivedCostPerEggCents: Long,

    /** derivedCostPerEggCents * quantity */
    val totalCogsCents: Long,

    val notes: String? = null,

    /**
     * True if this sale has been cancelled.
     * Cancellation inserts reversal finance entries and decrements soldEggs.
     * Original allocations are retained as audit trail.
     */
    val cancelled: Boolean = false,

    val createdAtEpochMillis: Long = System.currentTimeMillis(),

    val syncId: String = java.util.UUID.randomUUID().toString(),
    val syncTime: Long? = null,

    val scopeType: String = "USER",
    val scopeId: String? = null
)
