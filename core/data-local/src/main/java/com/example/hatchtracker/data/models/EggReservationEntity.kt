package com.example.hatchtracker.data.models

import androidx.room.*

@Entity(
    tableName = "egg_reservation",
    foreignKeys = [
        ForeignKey(
            entity = IncubationEntity::class,
            parentColumns = ["id"],
            childColumns = ["incubationId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EggProductionEntity::class,
            parentColumns = ["id"],
            childColumns = ["productionLogId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("incubationId"),
        Index("productionLogId")
    ]
)
data class EggReservationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val incubationId: Long,
    val productionLogId: String, // String to match EggProductionEntity.id (UUID)
    
    val reservedCount: Int,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
) {
    init {
        require(reservedCount > 0) { "Reserved count must be greater than zero" }
    }
}
