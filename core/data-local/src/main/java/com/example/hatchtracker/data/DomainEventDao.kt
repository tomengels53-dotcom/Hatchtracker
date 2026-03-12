package com.example.hatchtracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.hatchtracker.data.models.DomainEventEntity

@Dao
interface DomainEventDao {

    @Insert
    suspend fun insert(event: DomainEventEntity): Long

    @Query("SELECT * FROM domain_event WHERE eventId = :eventId")
    suspend fun getByEventId(eventId: String): DomainEventEntity?

    /** Get events for a specific aggregate — uses new String aggregateId column. */
    @Query("SELECT * FROM domain_event WHERE aggregateType = :aggregateType AND aggregateId = :aggregateId ORDER BY createdAtEpochMillis DESC")
    suspend fun getByAggregate(aggregateType: String, aggregateId: String): List<DomainEventEntity>

    @Query("SELECT * FROM domain_event WHERE aggregateType = :aggregateType AND aggregateId = :aggregateId ORDER BY createdAtEpochMillis DESC")
    fun getFlowByAggregate(aggregateType: String, aggregateId: String): kotlinx.coroutines.flow.Flow<List<DomainEventEntity>>

    @Query("SELECT * FROM domain_event ORDER BY createdAtEpochMillis DESC LIMIT :limit")
    suspend fun getRecentEvents(limit: Int): List<DomainEventEntity>

    @Query("DELETE FROM domain_event WHERE createdAtEpochMillis < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long): Int
}
