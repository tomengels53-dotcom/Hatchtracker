package com.example.hatchtracker.data

import com.example.hatchtracker.data.models.DomainEventEntity
import com.example.hatchtracker.data.models.SyncQueueEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Atomically logs a domain event and enqueues it for action-based synchronization.
 *
 * aggregateId is a String (UUID or syncId) — safe for UUID-keyed entities.
 * Must be called inside a db.withTransaction block for full B2 atomicity.
 */
@Singleton
class DomainEventLogger @Inject constructor(
    private val db: AppDatabase
) {
    companion object {
        const val HEALTH_RECORDED = "HEALTH_RECORDED"
        const val TREATMENT_APPLIED = "TREATMENT_APPLIED"
        const val VACCINATION_APPLIED = "VACCINATION_APPLIED"
        const val MAINTENANCE_LOGGED = "MAINTENANCE_LOGGED"
        const val BIRD_GRADUATED = "BIRD_GRADUATED"
        const val BIRD_MOVED = "BIRD_MOVED"
        const val HATCH_COMPLETED = "HATCH_COMPLETED"
        const val BIRD_REMOVED = "BIRD_REMOVED"
        const val BIRD_ADDED = "BIRD_ADDED"
    }

    private val dao = db.domainEventDao()
    private val syncQueueDao = db.syncQueueDao()

    suspend fun log(
        aggregateType: String,
        aggregateId: String,
        eventType: String,
        payloadJson: String? = null,
        schemaVersion: Int = 1,
        dedupeKey: String? = null
    ): String {
        val event = DomainEventEntity(
            aggregateType = aggregateType,
            aggregateId = aggregateId,
            eventType = eventType,
            payloadJson = payloadJson,
            schemaVersion = schemaVersion,
            dedupeKey = dedupeKey
        )

        // 1. Record the event
        dao.insert(event)

        // 2. Enqueue for action-based sync
        syncQueueDao.enqueue(SyncQueueEntity(eventId = event.eventId))

        return event.eventId
    }
}
