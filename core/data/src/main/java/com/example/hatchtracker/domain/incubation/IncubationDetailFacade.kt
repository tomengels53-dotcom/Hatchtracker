package com.example.hatchtracker.domain.incubation

import com.example.hatchtracker.common.format.LocaleFormatService
import com.example.hatchtracker.core.common.BreedingAnalyzer
import com.example.hatchtracker.core.common.FinancialInsights
import com.example.hatchtracker.data.repository.*
import com.example.hatchtracker.data.service.BirdLifecycleService
import com.example.hatchtracker.domain.breeding.DeviceCapacity
import com.example.hatchtracker.domain.breeding.DeviceCapacityManager
import com.example.hatchtracker.domain.breeding.IncubationUtils
import com.example.hatchtracker.domain.subscription.AppCapabilities
import com.example.hatchtracker.data.models.FinancialStats
import com.example.hatchtracker.model.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Facade for Incubation Detail orchestration.
 * Centralizes device assignment, financial analytics, and stage management for active incubations.
 */
@Singleton
class IncubationDetailFacade @Inject constructor(
    private val incubationRepository: IncubationRepository,
    private val deviceRepository: DeviceRepository,
    private val deviceCapacityManager: DeviceCapacityManager,
    private val financialRepository: FinancialRepository,
    private val userRepository: UserRepository,
    private val lifecycleService: BirdLifecycleService,
    private val breedingAnalyzer: BreedingAnalyzer,
    val localeFormatService: LocaleFormatService
) {
    /**
     * User's preferred currency code.
     */
    val currencyCode: Flow<String> = userRepository.userProfile.map { it?.currencyCode ?: "USD" }

    /**
     * User's preferred date format.
     */
    val dateFormat: Flow<String> = userRepository.userProfile.map { it?.dateFormat ?: "DD-MM-YYYY" }

    /**
     * User's preferred time format (12h/24h).
     */
    val timeFormat: Flow<String> = userRepository.userProfile.map { it?.timeFormat ?: "24h" }

    /**
     * Current subscription capabilities.
     */
    val capabilities = MutableStateFlow(AppCapabilities())

    /**
     * Returns a one-shot snapshot of an incubation.
     */
    suspend fun getIncubation(id: Long): Incubation? = incubationRepository.getIncubationById(id)

    /**
     * Returns a flow of aggregated financial stats for an incubation.
     */
    fun observeAggregatedStats(incubationId: Long): Flow<FinancialStats?> {
        return financialRepository.getAggregatedStats(
            ownerType = "incubation",
            ownerId = incubationId.toString(),
            startDate = 0L,
            endDate = System.currentTimeMillis()
        )
    }

    /**
     * Deep analytics bridge for calculating financial insights and breeder scores.
     */
    fun calculateFinancialInsights(incubation: Incubation, stats: FinancialStats): FinancialInsights {
        // In a real app, we might pass offspring history here
        val breederScore = breedingAnalyzer.calculateBreederScore(listOf(incubation), emptyList())
        return breedingAnalyzer.calculateFinancialInsights(incubation, stats.totalCost, breederScore)
    }

    /**
     * Engine bridge for generating Hatchy advice based on incubation progress.
     */
    fun getHatchyAdvice(incubation: Incubation): String {
        val daysRemaining = IncubationUtils.calculateDaysUntilHatch(incubation.expectedHatch)
        val progress = IncubationUtils.calculateIncubationProgress(incubation.startDate, incubation.expectedHatch)
        
        return when {
            daysRemaining <= 3 -> "hatchy_advice_lockdown"
            daysRemaining <= 7 -> "hatchy_advice_approaching"
            progress > 0.5 -> "hatchy_advice_mid_incubation"
            else -> "hatchy_advice_early"
        }
    }

    /**
     * Returns all user hatchers with their current remaining capacity.
     */
    fun getUserHatchers(): Flow<List<DeviceCapacity>> {
        return deviceCapacityManager.getCapacityForDevices(deviceRepository.getUserDevices())
    }

    /**
     * Returns all user devices for simple name/ID resolution.
     */
    fun getAllUserDevices(): Flow<List<Device>> = deviceRepository.getUserDevices()

    /**
     * Persists updates to an incubation record.
     */
    suspend fun updateIncubation(incubation: Incubation) {
        incubationRepository.update(incubation)
    }

    /**
     * Removes an incubation from the system.
     */
    suspend fun deleteIncubation(incubation: Incubation) {
        incubationRepository.deleteIncubation(incubation)
    }

    /**
     * Marks incubation-scoped egg sales.
     */
    suspend fun markSold(incubation: Incubation, quantity: Int, price: Double, date: Long, buyerName: String, notes: String) {
        lifecycleService.markSold(
            sourceType = BirdLifecycleStage.INCUBATING,
            sourceId = incubation.id,
            syncId = incubation.syncId,
            quantity = quantity,
            price = price,
            date = date,
            buyerName = buyerName.ifBlank { null },
            notes = notes
        )
    }
}
