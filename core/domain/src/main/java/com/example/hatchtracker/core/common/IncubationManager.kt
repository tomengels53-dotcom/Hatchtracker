@file:android.annotation.SuppressLint("NewApi")

package com.example.hatchtracker.core.common

import com.example.hatchtracker.model.IncubationLike
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

enum class IncubationPhase {
    EARLY_INCUBATION,
    MID_INCUBATION,
    LOCKDOWN,
    HATCH_WINDOW,
    OVERDUE,
    COMPLETE
}

data class IncubationStatus(
    val day: Int,
    val totalDays: Int,
    val phase: IncubationPhase,
    val progress: Float
)

data class IncubationTask(
    val type: IncubationTaskType,
    val description: String,
    val descriptionKey: String? = null,
    val descriptionArgs: List<Any> = emptyList(),
    val isCritical: Boolean = false
)

data class IncubationTargets(
    val tempMin: Double,
    val tempMax: Double,
    val humidityMin: Double,
    val humidityMax: Double,
    val tempUnit: String = "C"
)

enum class DayStatus {
    PAST,
    TODAY,
    FUTURE
}

data class TimelineDayState(
    val dayNumber: Int,
    val date: LocalDate,
    val status: DayStatus,
    val phase: IncubationPhase,
    val tasks: List<IncubationTask>
)

object IncubationManager {

    fun getConfig(incubation: IncubationLike): IncubationConfig {
        return SpeciesConfigs.getForSpecies(incubation.species)
    }

    private fun parseStartDate(dateStr: String): LocalDate? {
        return try {
            // Try ISO format first (YYYY-MM-DD) which we enforce in DB
            LocalDate.parse(dateStr)
        } catch (e: Exception) {
            // Fallback or return null
            null
        }
    }

    fun getStatus(incubation: IncubationLike): IncubationStatus {
        val config = getConfig(incubation)
        val startDate = parseStartDate(incubation.startDate) ?: return IncubationStatus(0, config.incubationDays, IncubationPhase.EARLY_INCUBATION, 0f)
        
        val today = LocalDate.now()
        // Day 1 is the day AFTER start date usually, or start date itself?
        // Standard convention: "Day 1" is the first full day completed, or the day setting. 
        // Let's assume Day 1 = Start Date.
        val daysElapsed = ChronoUnit.DAYS.between(startDate, today).toInt() + 1
        
        val phase = when {
            incubation.hatchCompleted -> IncubationPhase.COMPLETE
            daysElapsed > config.incubationDays + 2 -> IncubationPhase.OVERDUE
            daysElapsed >= config.hatchWindowStartDay -> IncubationPhase.HATCH_WINDOW
            daysElapsed >= config.lockdownDay -> IncubationPhase.LOCKDOWN
            daysElapsed > (config.incubationDays / 2) -> IncubationPhase.MID_INCUBATION
            else -> IncubationPhase.EARLY_INCUBATION
        }

        val progress = (daysElapsed.toFloat() / config.incubationDays.toFloat()).coerceIn(0f, 1f)

        return IncubationStatus(
            day = daysElapsed,
            totalDays = config.incubationDays,
            phase = phase,
            progress = progress
        )
    }

    fun getDailyTasks(incubation: IncubationLike): List<IncubationTask> {
        val config = getConfig(incubation)
        val status = getStatus(incubation)
        val day = status.day
        val tasks = mutableListOf<IncubationTask>()

        if (status.phase == IncubationPhase.COMPLETE) return emptyList()

        // Turning
        if (day <= config.turningUntilDay) {
            tasks.add(IncubationTask(IncubationTaskType.TURN, "Turn eggs (3-5 times daily)", descriptionKey = "task_turn_eggs", isCritical = true))
        } else if (day == config.turningUntilDay + 1) {
             tasks.add(IncubationTask(IncubationTaskType.LOCKDOWN, "STOP turning eggs (Final Turn)", descriptionKey = "task_stop_turning", isCritical = true))
        }

        // Cooling
        if (config.coolingStartDay != null && day >= config.coolingStartDay && day < config.lockdownDay) {
             tasks.add(IncubationTask(IncubationTaskType.COOL, "Cool eggs for ${config.coolingDurationMinutes} minutes", descriptionKey = "task_cool_eggs_format", descriptionArgs = listOf(config.coolingDurationMinutes ?: ""), isCritical = false))
        }

        // Misting
        if (config.mistingStartDay != null && day >= config.mistingStartDay && day < config.lockdownDay) {
             tasks.add(IncubationTask(IncubationTaskType.MIST, "Mist eggs with lukewarm water", descriptionKey = "task_mist_eggs", isCritical = false))
        }

        // Candling
        if (config.candleDays.contains(day)) {
            tasks.add(IncubationTask(IncubationTaskType.CANDLE, "Candle eggs today (Day $day check)", descriptionKey = "task_candle_eggs_format", descriptionArgs = listOf(day), isCritical = false))
        }

        // Lockdown
        if (day == config.lockdownDay) {
            tasks.add(IncubationTask(IncubationTaskType.LOCKDOWN, "Enter LOCKDOWN mode", descriptionKey = "task_lockdown_mode", isCritical = true))
            tasks.add(IncubationTask(IncubationTaskType.CHECK_PARAMS, "Increase humidity to ${config.humidityLockdown.min}-${config.humidityLockdown.max}%", descriptionKey = "task_humidity_increase_format", descriptionArgs = listOf(config.humidityLockdown.min.roundToInt(), config.humidityLockdown.max.roundToInt()), isCritical = true))
        }
        
        // Ventiliation
        if (config.ventilationIncreaseDay != null && day == config.ventilationIncreaseDay) {
             tasks.add(IncubationTask(IncubationTaskType.CHECK_PARAMS, "Increase ventilation", descriptionKey = "task_ventilation_increase", isCritical = false))
        }

        // General Check
        tasks.add(IncubationTask(IncubationTaskType.CHECK_PARAMS, "Check Temperature & Humidity", descriptionKey = "task_check_params", isCritical = false))

        return tasks
    }

    fun getTargets(incubation: IncubationLike): IncubationTargets {
        val config = getConfig(incubation)
        val status = getStatus(incubation)
        val day = status.day

        val temp = config.temperature
        val humidity = if (day >= config.lockdownDay) config.humidityLockdown else config.humidityDays1ToLockdown

        return IncubationTargets(
            tempMin = temp.min,
            tempMax = temp.max,
            humidityMin = humidity.min,
            humidityMax = humidity.max
        )
    }

    fun generateTimeline(incubation: IncubationLike): List<TimelineDayState> {
        val config = getConfig(incubation)
        val startDate = parseStartDate(incubation.startDate) ?: return emptyList()
        val today = LocalDate.now()
        
        val timeline = mutableListOf<TimelineDayState>()
        val totalDays = config.incubationDays + 3 // Show a few days past hatch date
        
        for (i in 1..totalDays) {
            val date = startDate.plusDays((i - 1).toLong())
            val status = when {
                date.isBefore(today) -> DayStatus.PAST
                date.isEqual(today) -> DayStatus.TODAY
                else -> DayStatus.FUTURE
            }
            
            // Calculate phase/tasks for this specific historical/future day
            val daysElapsed = i
            val phase = when {
                daysElapsed > config.incubationDays + 2 -> IncubationPhase.OVERDUE
                daysElapsed >= config.hatchWindowStartDay -> IncubationPhase.HATCH_WINDOW
                daysElapsed >= config.lockdownDay -> IncubationPhase.LOCKDOWN
                daysElapsed > (config.incubationDays / 2) -> IncubationPhase.MID_INCUBATION
                else -> IncubationPhase.EARLY_INCUBATION
            }
            
            // Tasks
             val tasks = mutableListOf<IncubationTask>()
             // Turning
            if (daysElapsed <= config.turningUntilDay) {
                tasks.add(IncubationTask(IncubationTaskType.TURN, "Turn eggs", descriptionKey = "task_turn_eggs_simple"))
            } else if (daysElapsed == config.turningUntilDay + 1) {
                 tasks.add(IncubationTask(IncubationTaskType.LOCKDOWN, "FINAL TURN today", descriptionKey = "task_final_turn", isCritical = true))
            }
            // Lockdown
            if (daysElapsed == config.lockdownDay) {
                tasks.add(IncubationTask(IncubationTaskType.LOCKDOWN, "Enter LOCKDOWN", descriptionKey = "task_enter_lockdown", isCritical = true))
            }
            
            timeline.add(TimelineDayState(i, date, status, phase, tasks))
        }
        return timeline
    }
}

