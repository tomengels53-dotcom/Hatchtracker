package com.example.hatchtracker.troubleshooting

enum class ConfidenceLevel {
    LOW,
    MEDIUM,
    HIGH
}

enum class RiskFactor {
    TEMP_LOW,
    TEMP_HIGH,
    HUMIDITY_LOW,
    HUMIDITY_HIGH,
    PIPPING_DELAYED,
    POWER_OUTAGE,
    NONE
}

data class Diagnosis(
    val issue: String,
    val explanation: String,
    val confidence: ConfidenceLevel,
    val immediateActions: List<String>,
    val preventionTips: List<String>,
    val riskFactor: RiskFactor
)
