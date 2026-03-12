package com.example.hatchtracker.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Audit trail for domain mutations.
 * B2: aggregateId is now a String (UUID or syncId) for collision-safe, reversible identity.
 * Physical column name kept as "entityId" in current DB; renamed to "aggregateId" in migration 25→26.
 */
@Entity(
    tableName = "domain_event",
    indices = [
        Index("aggregateType"),
        Index("aggregateId"),
        Index("eventType"),
        Index("eventId", unique = true),
        Index("createdAtEpochMillis"),
        Index("createdAtEpochMillis", orders = [Index.Order.DESC], name = "index_domain_event_createdAt_desc")
    ]
)
data class DomainEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Stable UUID for sync and deduplication */
    @ColumnInfo(defaultValue = "''")
    val eventId: String = java.util.UUID.randomUUID().toString(),

    @ColumnInfo(name = "aggregateType")
    val aggregateType: String,

    /** String UUID or syncId — maps to physical column 'aggregateId' after migration 25→26 */
    @ColumnInfo(name = "aggregateId")
    val aggregateId: String,

    val eventType: String,

    val payloadJson: String? = null,

    val createdAtEpochMillis: Long = System.currentTimeMillis(),

    @ColumnInfo(defaultValue = "1")
    val schemaVersion: Int = 1,

    val dedupeKey: String? = null
)
