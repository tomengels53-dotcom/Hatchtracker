package com.example.hatchtracker.domain.hatchy.routing

private fun normalizeTopicToken(raw: String): String {
    return raw
        .substringAfterLast('$')
        .substringBefore('@')
        .substringAfterLast('.')
        .trim()
        .replace(" ", "_")
        .uppercase()
}

fun parseIncubationTopic(value: String): IncubationTopic? = when (normalizeTopicToken(value)) {
    "DURATION" -> IncubationTopic.Duration
    "HUMIDITY" -> IncubationTopic.Humidity
    "TEMPERATURE" -> IncubationTopic.Temperature
    "CANDLING" -> IncubationTopic.Candling
    "TURNING" -> IncubationTopic.Turning
    "LOCKDOWN" -> IncubationTopic.Lockdown
    "HATCHDAY", "HATCH_DAY" -> IncubationTopic.HatchDay
    "HEALTH" -> IncubationTopic.Health
    "OTHER" -> IncubationTopic.Other
    else -> null
}

fun parseNurseryStatusTopic(value: String): NurseryStatusTopic? = when (normalizeTopicToken(value)) {
    "ACTIVECOUNT", "ACTIVE_COUNT" -> NurseryStatusTopic.ActiveCount
    "READYTOMOVE", "READY_TO_MOVE" -> NurseryStatusTopic.ReadyToMove
    "BROODERSTATUS", "BROODER_STATUS" -> NurseryStatusTopic.BrooderStatus
    "LOSSES" -> NurseryStatusTopic.Losses
    "AGEGROUPS", "AGE_GROUPS" -> NurseryStatusTopic.AgeGroups
    "OTHER" -> NurseryStatusTopic.Other
    else -> null
}

fun parseNurseryGuidanceTopic(value: String): NurseryGuidanceTopic? = when (normalizeTopicToken(value)) {
    "TEMPERATURE" -> NurseryGuidanceTopic.Temperature
    "FEEDING" -> NurseryGuidanceTopic.Feeding
    "COOPTRANSITION", "COOP_TRANSITION" -> NurseryGuidanceTopic.CoopTransition
    "OTHER" -> NurseryGuidanceTopic.Other
    else -> null
}

fun parseFinanceSummaryTopic(value: String): FinanceSummaryTopic? = when (normalizeTopicToken(value)) {
    "TOTALSPEND", "TOTAL_SPEND" -> FinanceSummaryTopic.TotalSpend
    "CATEGORYBREAKDOWN", "CATEGORY_BREAKDOWN" -> FinanceSummaryTopic.CategoryBreakdown
    "FLOCKCOST", "FLOCK_COST" -> FinanceSummaryTopic.FlockCost
    "MONTHLYTREND", "MONTHLY_TREND" -> FinanceSummaryTopic.MonthlyTrend
    "RECENTEXPENSES", "RECENT_EXPENSES" -> FinanceSummaryTopic.RecentExpenses
    "OTHER" -> FinanceSummaryTopic.Other
    else -> null
}

fun parseFinanceHelpTopic(value: String): FinanceHelpTopic? = when (normalizeTopicToken(value)) {
    "LOGEXPENSE", "LOG_EXPENSE" -> FinanceHelpTopic.LogExpense
    "EDITENTRY", "EDIT_ENTRY" -> FinanceHelpTopic.EditEntry
    "OTHER" -> FinanceHelpTopic.Other
    else -> null
}

fun parseEquipmentStatusTopic(value: String): EquipmentStatusTopic? = when (normalizeTopicToken(value)) {
    "ACTIVEDEVICES", "ACTIVE_DEVICES" -> EquipmentStatusTopic.ActiveDevices
    "SENSORSTATUS", "SENSOR_STATUS" -> EquipmentStatusTopic.SensorStatus
    "CAPACITY" -> EquipmentStatusTopic.Capacity
    "MAINTENANCE" -> EquipmentStatusTopic.Maintenance
    "ALERTS" -> EquipmentStatusTopic.Alerts
    "OTHER" -> EquipmentStatusTopic.Other
    else -> null
}

fun parseEquipmentHelpTopic(value: String): EquipmentHelpTopic? = when (normalizeTopicToken(value)) {
    "ADDDEVICE", "ADD_DEVICE" -> EquipmentHelpTopic.AddDevice
    "OTHER" -> EquipmentHelpTopic.Other
    else -> null
}

fun parseBreedingTopic(value: String): BreedingTopic? = when (normalizeTopicToken(value)) {
    "LINEBREEDING", "LINE_BREEDING" -> BreedingTopic.LineBreeding
    "CROSSBREEDING", "CROSS_BREEDING" -> BreedingTopic.CrossBreeding
    "SELECTION" -> BreedingTopic.Selection
    "GENETICS" -> BreedingTopic.Genetics
    "STABILITY" -> BreedingTopic.Stability
    "ADVICE" -> BreedingTopic.Advice
    "SIMULATION" -> BreedingTopic.Simulation
    "OUTCOME" -> BreedingTopic.Outcome
    "OTHER" -> BreedingTopic.Other
    else -> null
}
