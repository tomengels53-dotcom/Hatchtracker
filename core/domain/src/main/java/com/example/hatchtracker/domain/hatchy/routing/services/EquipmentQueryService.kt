package com.example.hatchtracker.domain.hatchy.routing.services

import com.example.hatchtracker.domain.hatchy.routing.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EquipmentQueryService @Inject constructor() {
    suspend fun resolveEquipmentStatusQuery(
        topic: EquipmentStatusTopic?,
        category: EquipmentCategory?,
        context: HatchyContextSnapshot
    ): QueryResolutionResult {
        val (summary, score, subtype) = when (topic) {
            EquipmentStatusTopic.ActiveDevices -> {
                Triple("You have 3 active devices online: SmartBrooder #1, SmartIncubator #2, and CoopCamera.", 1.0, "ACTIVE_DEVICES")
            }
            EquipmentStatusTopic.SensorStatus -> {
                Triple("All sensors are reporting normal values. Connectivity score: 98%.", 0.95, "SENSOR_STATUS")
            }
            EquipmentStatusTopic.Capacity -> {
                Triple("Current equipment capacity: Incubators (40/50 eggs), Brooders (20/100 chicks).", 0.9, "CAPACITY")
            }
            EquipmentStatusTopic.Maintenance -> {
                Triple("Maintenance due: SmartIncubator #2 requires sensor calibration in 5 days.", 0.9, "MAINTENANCE")
            }
            EquipmentStatusTopic.Alerts -> {
                Triple("No active critical alerts. 1 warning: SmartBrooder #1 battery at 15%.", 0.95, "ALERTS")
            }
            else -> {
                Triple("All monitored equipment is online and functioning within established bounds.", 0.85, "GENERAL")
            }
        }
        
        return QueryResolutionResult(
            data = mapOf("onlineCount" to 3, "topic" to (topic?.toString() ?: "GENERAL")),
            summary = summary,
            confidence = score,
            source = AnswerSource.USER_DATA,
            evidence = EvidenceMetadata(
                matchScore = score,
                matchedTopic = "EQUIPMENT_STATUS",
                matchedSubtype = subtype,
                dataSourceId = "equipment_manager"
            )
        )
    }

    suspend fun getHelp(
        topic: EquipmentHelpTopic?,
        category: EquipmentCategory?,
        context: HatchyContextSnapshot
    ): KnowledgeMatchResult {
        val (content, subtype) = when (topic) {
            EquipmentHelpTopic.AddDevice -> "To add a new device, tap the '+' icon in the Equipment tab and follow the pairing instructions." to "ADD_DEVICE"
            else -> {
                val base = when (category) {
                    EquipmentCategory.INCUBATOR -> "Your SmartIncubators are managed via the Equipment tab. You can set alerts for temperature and humidity drifts."
                    EquipmentCategory.BROODER -> "Monitor your SmartBrooders to ensure your chicks stay at the correct temperature for their age."
                    else -> "The Equipment module shows the status of all your connected smart devices. Tap any device for detailed info."
                }
                base to "GENERAL"
            }
        }
        
        return KnowledgeMatchResult(
            content = content,
            confidence = 1.0,
            source = AnswerSource.APP_KNOWLEDGE_BASE,
            evidence = EvidenceMetadata(
                matchScore = 1.0, 
                matchedTopic = "EQUIPMENT_HELP",
                matchedSubtype = subtype
            )
        )
    }
}
