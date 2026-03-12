package com.example.hatchtracker.domain.repo

import com.example.hatchtracker.model.FinancialStats
import com.example.hatchtracker.model.EnrichedFinancialStats
import kotlinx.coroutines.flow.Flow

interface FinancialRepository {
    fun getAggregatedStats(ownerType: String? = null, ownerId: String? = null, startDate: Long = 0, endDate: Long = Long.MAX_VALUE): Flow<FinancialStats>
    fun getEnrichedStats(ownerType: String, ownerId: String): Flow<EnrichedFinancialStats>
}
