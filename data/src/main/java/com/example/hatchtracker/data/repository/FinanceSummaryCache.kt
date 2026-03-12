package com.example.hatchtracker.data.repository

import com.example.hatchtracker.data.models.FinancialSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple short-TTL cache for basic financial totals to avoid loading spinners 
 * and jank when repeatedly opening Bottom Sheets.
 */
@Singleton
class FinanceSummaryCache @Inject constructor() {
    
    private val cache = MutableStateFlow<Map<String, FinancialSummary>>(emptyMap())

    val state: StateFlow<Map<String, FinancialSummary>> = cache.asStateFlow()

    fun getSummary(entityId: String): FinancialSummary? {
        return cache.value[entityId]
    }

    fun updateCache(entityId: String, summary: FinancialSummary) {
        cache.update { current ->
            val mutable = current.toMutableMap()
            mutable[entityId] = summary
            mutable
        }
    }

    fun clearCache() {
        cache.value = emptyMap()
    }
}
