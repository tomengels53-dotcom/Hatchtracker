package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.DomainEventDao
import com.example.hatchtracker.data.mappers.toModel
import com.example.hatchtracker.model.DomainEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DomainEventRepository @Inject constructor(
    private val domainEventDao: DomainEventDao
) {
    suspend fun getEventsForAggregate(aggregateType: String, aggregateId: String): List<DomainEvent> = 
        withContext(Dispatchers.IO) {
            domainEventDao.getByAggregate(aggregateType, aggregateId).map { it.toModel() }
        }

    fun getEventsFlowForAggregate(aggregateType: String, aggregateId: String): kotlinx.coroutines.flow.Flow<List<DomainEvent>> =
        domainEventDao.getFlowByAggregate(aggregateType, aggregateId)
            .map { entities -> entities.map { it.toModel() } }

    suspend fun getRecentEvents(limit: Int): List<DomainEvent> = 
        withContext(Dispatchers.IO) {
            domainEventDao.getRecentEvents(limit).map { it.toModel() }
        }
}
