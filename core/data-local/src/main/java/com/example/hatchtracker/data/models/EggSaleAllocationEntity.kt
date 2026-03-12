package com.example.hatchtracker.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * FIFO allocation of eggs from a production log to a sale.
 * Retained permanently as an audit trail even after sale cancellation.
 * soldEggs in EggProductionEntity is the live cached sum of non-cancelled allocations.
 */
@Entity(
    tableName = "egg_sale_allocation",
    indices = [
        Index(value = ["saleId"]),
        Index(value = ["productionLogId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = EggSaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class EggSaleAllocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** FK → egg_sale.id (CASCADE on delete) */
    val saleId: Long,

    /** FK → egg_production.id (String UUID). No enforced FK; validator checks orphans. */
    val productionLogId: String,

    /** Eggs allocated from this production log to this sale. Must be > 0. */
    val allocatedCount: Int
)
