package com.example.hatchtracker.data.models

import androidx.room.*

@Entity(
    tableName = "trait_promotions",
    foreignKeys = [
        ForeignKey(
            entity = BirdEntity::class,
            parentColumns = ["id"],
            childColumns = ["birdId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("birdId")]
)
data class TraitPromotion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val birdId: Long,
    val traitName: String,
    val oldStatus: String, // e.g., "INFERRED"
    val newStatus: String, // e.g., "FIXED"
    val promotedBy: String, // Admin User ID
    val timestamp: Long = System.currentTimeMillis(),
    val reason: String? = null
)


