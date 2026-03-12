package com.example.hatchtracker.data.validation

import com.example.hatchtracker.data.AppDatabase
import com.example.hatchtracker.data.models.DomainEventEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataIntegrityValidator @Inject constructor(
    private val db: AppDatabase
) {

    enum class Severity { CRITICAL, MAJOR, MINOR }

    data class IntegrityIssue(
        val category: String,
        val message: String,
        val severity: Severity,
        val entityId: String? = null
    )

    suspend fun validateEverything(): List<IntegrityIssue> {
        val issues = mutableListOf<IntegrityIssue>()

        issues.addAll(validateEggReservesAndLogs())
        issues.addAll(validateIncubationsAndReserves())
        issues.addAll(validateDomainEvents())

        return issues
    }

    /**
     * Rule: productionLog.setForIncubation MUST equal sum(EggReservation.reservedCount) for that log.
     */
    private suspend fun validateEggReservesAndLogs(): List<IntegrityIssue> {
        val issues = mutableListOf<IntegrityIssue>()
        
        // 1. Get aggregation from Room (O(Logs))
        val reservationsByLog = db.eggReservationDao().summarizeAllReservationsByLog().associateBy { it.logId }
        
        // 2. Scan logs (O(Logs))
        val logs = db.eggProductionDao().getAllLogsSync()
        
        for (log in logs) {
            val actualReserved = reservationsByLog[log.id]?.totalReserved ?: 0
            if (log.setForIncubation != actualReserved) {
                issues.add(
                    IntegrityIssue(
                        category = "EGG_ACCOUNTING",
                        message = "Production log setForIncubation (${log.setForIncubation}) mismatch with reservations ($actualReserved)",
                        severity = Severity.CRITICAL,
                        entityId = log.id
                    )
                )
            }
        }
        
        return issues
    }

    /**
     * Rule: incubation.eggsCount SHOULD equal sum(EggReservation.reservedCount) for that incubation.
     */
    private suspend fun validateIncubationsAndReserves(): List<IntegrityIssue> {
        val issues = mutableListOf<IntegrityIssue>()
        
        val reservationsByInc = db.eggReservationDao().summarizeAllReservationsByIncubation().associateBy { it.incubationId }
        val incubations = db.incubationDao().getAllIncubationEntitys()
        
        for (inc in incubations) {
            val actualReserved = reservationsByInc[inc.id]?.totalReserved ?: 0
            if (inc.eggsCount != actualReserved) {
                // This might be a legacy incubation or a bug
                issues.add(
                    IntegrityIssue(
                        category = "INCUBATION_ACCOUNTING",
                        message = "Incubation eggsCount (${inc.eggsCount}) mismatch with reservations ($actualReserved)",
                        severity = Severity.MAJOR,
                        entityId = inc.id.toString()
                    )
                )
            }
        }
        
        return issues
    }

    /**
     * Rule: DomainEvent entries must have non-blank types and valid IDs.
     */
    private suspend fun validateDomainEvents(): List<IntegrityIssue> {
        val issues = mutableListOf<IntegrityIssue>()
        
        // Fetch last 1000 events to check for corruption/bad logging
        // In a real app we might use a DAO query to find these, but checking recent is usually enough for debug.
        val recentEvents = db.domainEventDao().getRecentEvents(1000)
        
        for (event in recentEvents) {
            if (event.aggregateType.isBlank() || event.eventType.isBlank()) {
                issues.add(
                    IntegrityIssue(
                        category = "AUDIT_INTEGRITY",
                        message = "Domain event ${event.id} has blank type fields",
                        severity = Severity.MAJOR,
                        entityId = event.id.toString()
                    )
                )
            }
            if (event.aggregateId.isBlank()) {
                issues.add(
                    IntegrityIssue(
                        category = "AUDIT_INTEGRITY",
                        message = "Domain event ${event.id} has invalid aggregateId (${event.aggregateId})",
                        severity = Severity.MAJOR,
                        entityId = event.id.toString()
                    )
                )
            }
        }
        
        return issues
    }
}
