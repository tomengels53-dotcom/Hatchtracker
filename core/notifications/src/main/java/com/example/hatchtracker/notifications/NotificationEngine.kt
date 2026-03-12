package com.example.hatchtracker.notifications

import android.content.Context
import com.example.hatchtracker.data.models.IncubationLike
import com.example.hatchtracker.data.InboxNotificationDao
import com.example.hatchtracker.data.NotificationHistoryDao
import com.example.hatchtracker.data.models.InboxNotification
import com.example.hatchtracker.data.models.NotificationHistory
import com.example.hatchtracker.core.common.IncubationManager
import com.example.hatchtracker.troubleshooting.IncubationTroubleshooter
import com.example.hatchtracker.troubleshooting.RiskFactor
import com.example.hatchtracker.troubleshooting.Diagnosis
import com.example.hatchtracker.notifications.push.PushEnvelope
import com.example.hatchtracker.core.logging.LogTags
import com.example.hatchtracker.core.logging.Logger
import com.example.hatchtracker.core.notifications.R

class NotificationEngine(
    private val historyDao: NotificationHistoryDao,
    private val inboxDao: InboxNotificationDao,
    private val prefs: NotificationPreferences,
    private val context: Context
) {
    private val troubleshooter = IncubationTroubleshooter()

    suspend fun handleRemoteEvent(envelope: PushEnvelope): NotificationEvent? {
        if (!prefs.isNotificationsEnabled) return null

        if (historyDao.isEventProcessed(envelope.eventId)) {
            Logger.i(LogTags.NOTIFICATIONS, "op=handleRemoteEvent status=skipped reason=dedupe eventId=${envelope.eventId}")
            return null
        }

        val type = envelope.type
        val severity = envelope.severityHint ?: NotificationSeverity.INFO
        val title = envelope.title ?: context.getString(R.string.notification_generic_title)
        val message = envelope.body ?: ""

        val event = NotificationEvent(
            incubationId = envelope.entityId ?: 0,
            ruleId = "remote_${type.name.lowercase()}",
            type = type,
            severity = severity,
            title = title,
            message = message,
            eventId = envelope.eventId,
            deeplink = envelope.deeplink
        )

        val inQuietHours = prefs.isInQuietHours()
        if (inQuietHours && severity != NotificationSeverity.CRITICAL) {
            Logger.i(LogTags.NOTIFICATIONS, "op=handleRemoteEvent status=silent reason=quiet_hours eventId=${envelope.eventId}")
            persistToInbox(event)
            recordNotificationSent(event.incubationId, event.ruleId, event.eventId)
            return null
        }

        persistToInbox(event)
        recordNotificationSent(event.incubationId, event.ruleId, event.eventId)

        return event
    }

    suspend fun checkScheduledNotifications(incubation: IncubationLike): List<NotificationEvent> {
        val events = mutableListOf<NotificationEvent>()
        if (!prefs.isNotificationsEnabled || !prefs.isIncubationRemindersEnabled) return events

        val config = IncubationManager.getConfig(incubation)
        val status = IncubationManager.getStatus(incubation)
        val day = status.day

        if (day == config.turningUntilDay + 1) {
            val ruleId = "stop_turning"
            if (shouldTriggerBasic(incubation.id, ruleId)) {
                createAndAddEvent(
                    events,
                    incubation.id,
                    ruleId,
                    NotificationType.SCHEDULED,
                    NotificationSeverity.WARNING,
                    context.getString(R.string.notification_stop_turning_title),
                    context.getString(R.string.notification_stop_turning_message)
                )
            }
        }

        if (day == config.lockdownDay) {
            val ruleId = "lockdown_start"
            if (shouldTriggerBasic(incubation.id, ruleId)) {
                createAndAddEvent(
                    events,
                    incubation.id,
                    ruleId,
                    NotificationType.SCHEDULED,
                    NotificationSeverity.CRITICAL,
                    context.getString(R.string.notification_lockdown_title),
                    context.getString(R.string.notification_lockdown_message, config.humidityLockdown.optimal.toInt())
                )
            }
        }

        if (day == config.hatchWindowStartDay) {
            val ruleId = "hatch_window"
            if (shouldTriggerBasic(incubation.id, ruleId)) {
                createAndAddEvent(
                    events,
                    incubation.id,
                    ruleId,
                    NotificationType.SCHEDULED,
                    NotificationSeverity.INFO,
                    context.getString(R.string.notification_hatch_window_title),
                    context.getString(R.string.notification_hatch_window_message)
                )
            }
        }

        if (config.coolingStartDay != null && day == config.coolingStartDay) {
            val ruleId = "cooling_start"
            if (shouldTriggerBasic(incubation.id, ruleId)) {
                createAndAddEvent(
                    events,
                    incubation.id,
                    ruleId,
                    NotificationType.SCHEDULED,
                    NotificationSeverity.INFO,
                    context.getString(R.string.notification_cooling_start_title),
                    context.getString(R.string.notification_cooling_start_message, config.speciesName)
                )
            }
        }

        return events
    }

    suspend fun checkEnvironmentalNotifications(
        incubation: IncubationLike,
        currentTemp: Double,
        currentHumidity: Double
    ): List<NotificationEvent> {
        val events = mutableListOf<NotificationEvent>()
        if (!prefs.isNotificationsEnabled || !prefs.isIncubationRemindersEnabled) return events

        val targets = IncubationManager.getTargets(incubation)

        if (currentTemp < targets.tempMin || currentTemp > targets.tempMax) {
            val ruleId = "temp_deviation"
            val baseSeverity = NotificationSeverity.WARNING
            val basicMsg = if (currentTemp < targets.tempMin) {
                context.getString(R.string.notification_temp_low_message, currentTemp)
            } else {
                context.getString(R.string.notification_temp_high_message, currentTemp)
            }

            val riskFactor = if (currentTemp < targets.tempMin) RiskFactor.TEMP_LOW else RiskFactor.TEMP_HIGH
            val diagnosis = getAiDiagnosis(incubation, currentTemp, null, riskFactor)

            processEnvironmentalTrigger(
                events,
                incubation.id,
                ruleId,
                baseSeverity,
                context.getString(R.string.notification_temp_alert_title),
                basicMsg,
                diagnosis
            )
        }

        if (currentHumidity < targets.humidityMin || currentHumidity > targets.humidityMax) {
            val ruleId = "humidity_deviation"
            val baseSeverity = NotificationSeverity.WARNING
            val basicMsg = if (currentHumidity < targets.humidityMin) {
                context.getString(R.string.notification_humidity_low_message, currentHumidity.toInt())
            } else {
                context.getString(R.string.notification_humidity_high_message, currentHumidity.toInt())
            }

            val riskFactor = if (currentHumidity < targets.humidityMin) RiskFactor.HUMIDITY_LOW else RiskFactor.HUMIDITY_HIGH
            val diagnosis = getAiDiagnosis(incubation, null, currentHumidity, riskFactor)

            processEnvironmentalTrigger(
                events,
                incubation.id,
                ruleId,
                baseSeverity,
                context.getString(R.string.notification_humidity_alert_title),
                basicMsg,
                diagnosis
            )
        }

        return events
    }

    private fun getAiDiagnosis(incubation: IncubationLike, temp: Double?, humidity: Double?, targetRisk: RiskFactor): Diagnosis? {
        if (!prefs.isProUser) return null

        val diagnoses = troubleshooter.analyze(incubation, temp, humidity)
        return diagnoses.find { it.riskFactor == targetRisk }
    }

    private suspend fun processEnvironmentalTrigger(
        events: MutableList<NotificationEvent>,
        incubationId: Long,
        ruleId: String,
        baseSeverity: NotificationSeverity,
        title: String,
        message: String,
        diagnosis: Diagnosis? = null
    ) {
        val lastTrigger = historyDao.getLastTrigger(incubationId, ruleId)
        val now = System.currentTimeMillis()
        val cooldownMs = 4 * 60 * 60 * 1000L

        var effectiveSeverity = baseSeverity
        var shouldTrigger = false

        if (lastTrigger == null) {
            shouldTrigger = true
        } else {
            val diff = now - lastTrigger.timestamp
            if (diff > cooldownMs) {
                if (diff < 24 * 60 * 60 * 1000L && baseSeverity == NotificationSeverity.WARNING) {
                    effectiveSeverity = NotificationSeverity.CRITICAL
                }
                shouldTrigger = true
            }
        }

        if (shouldTrigger) {
            createAndAddEvent(
                events,
                incubationId,
                ruleId,
                NotificationType.ENVIRONMENTAL,
                effectiveSeverity,
                title,
                message,
                diagnosis = diagnosis
            )
        }
    }

    private suspend fun shouldTriggerBasic(incubationId: Long, ruleId: String): Boolean {
        val lastTrigger = historyDao.getLastTrigger(incubationId, ruleId)
        val now = System.currentTimeMillis()
        val cooldownMs = 24 * 60 * 60 * 1000L

        if (lastTrigger == null) return true
        return (now - lastTrigger.timestamp) > cooldownMs
    }

    private suspend fun createAndAddEvent(
        events: MutableList<NotificationEvent>,
        incubationId: Long,
        ruleId: String,
        type: NotificationType,
        severity: NotificationSeverity,
        title: String,
        message: String,
        diagnosis: Diagnosis? = null
    ) {
        val event = NotificationEvent(
            incubationId = incubationId,
            ruleId = ruleId,
            type = type,
            severity = severity,
            title = title,
            message = message,
            aiExplanation = diagnosis?.explanation,
            aiConfidence = diagnosis?.confidence?.name,
            correctiveAction = diagnosis?.immediateActions?.firstOrNull()
        )

        if (prefs.isInQuietHours() && severity != NotificationSeverity.CRITICAL) {
            persistToInbox(event)
            recordNotificationSent(incubationId, ruleId, event.eventId)
            return
        }

        events.add(event)
        persistToInbox(event)
    }

    suspend fun recordNotificationSent(incubationId: Long, ruleId: String, eventId: String? = null) {
        historyDao.insert(NotificationHistory(incubationId = incubationId, ruleId = ruleId, eventId = eventId))
    }

    private suspend fun persistToInbox(event: NotificationEvent) {
        inboxDao.insert(
            InboxNotification(
                incubationId = event.incubationId,
                eventId = event.eventId,
                title = event.title,
                message = event.message,
                severity = event.severity.name,
                timestamp = System.currentTimeMillis()
            )
        )
    }
}
