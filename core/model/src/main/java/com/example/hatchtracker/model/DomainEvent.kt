package com.example.hatchtracker.model

data class DomainEvent(
    val eventId: String,
    val aggregateType: String,
    val aggregateId: String,
    val eventType: String,
    val payloadJson: String?,
    val timestamp: Long
)
