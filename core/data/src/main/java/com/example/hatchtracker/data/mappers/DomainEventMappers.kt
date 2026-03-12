package com.example.hatchtracker.data.mappers

import com.example.hatchtracker.data.models.DomainEventEntity
import com.example.hatchtracker.model.DomainEvent

fun DomainEventEntity.toModel(): DomainEvent {
    return DomainEvent(
        eventId = eventId,
        aggregateType = aggregateType,
        aggregateId = aggregateId,
        eventType = eventType,
        payloadJson = payloadJson,
        timestamp = createdAtEpochMillis
    )
}
