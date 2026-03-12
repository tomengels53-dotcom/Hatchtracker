package com.example.hatchtracker.data.models

import androidx.room.*
import java.util.UUID

@Entity(
    tableName = "financial_entries",
    indices = [
        Index(value = ["ownerType", "ownerId"]),
        Index(value = ["ownerType", "ownerId", "date"])
    ]
)
data class FinancialEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val syncId: String = UUID.randomUUID().toString(),
    val ownerId: String,       // can be Bird ID, Flock ID, or ActionPlan ID
    val ownerType: String,     // "bird", "flock", "incubation", "action_plan"
    val type: String,          // "cost", "revenue"
    val category: String,      // e.g., "Feed", "Sale", "Medicine"
    val amount: Double,
    val amountNet: Double? = null,
    val amountVAT: Double? = null,
    val amountGross: Double? = null,
    val currency: String? = null,
    val vatEnabled: Boolean? = null,
    val vatRate: Double? = null,
    val date: Long,
    val notes: String = "",
    val quantity: Int = 1,
    val unit: String? = null,
    @ColumnInfo(defaultValue = "0")
    val isRecurring: Boolean = false,
    val recurrenceIntervalDays: Int? = null,
    val lastRecurrenceDate: Long? = null,
    val depreciationMonths: Int? = null,
    
    // Multi-user Support (Phase 5)
    @ColumnInfo(defaultValue = "'USER'")
    val scopeType: String = "USER",
    val scopeId: String? = null
)
