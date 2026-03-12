package com.example.hatchtracker.troubleshooting

import com.example.hatchtracker.model.IncubationLike
import com.example.hatchtracker.core.common.IncubationManager
import com.example.hatchtracker.core.common.IncubationPhase

class IncubationTroubleshooter {

    fun analyze(
        incubation: IncubationLike,
        currentTemp: Double?,
        currentHumidity: Double?
    ): List<Diagnosis> {
        val diagnoses = mutableListOf<Diagnosis>()
        val config = IncubationManager.getConfig(incubation)
        val status = IncubationManager.getStatus(incubation)
        val targets = IncubationManager.getTargets(incubation)

        // 1. Temperature Analysis
        if (currentTemp != null) {
            if (currentTemp < targets.tempMin) {
                // Low Temp Logic
                val diff = targets.tempMin - currentTemp
                val confidence = if (diff > 2.0) ConfidenceLevel.HIGH else ConfidenceLevel.MEDIUM
                
                diagnoses.add(Diagnosis(
                    issue = "Temperature too Low",
                    explanation = "Low temperature slows down metabolic rate. This can lead to delayed hatches (22+ days for chickens) and weak chicks.",
                    confidence = confidence,
                    immediateActions = listOf(
                        "Check thermostat setting immediately.",
                        "Direct heat source closer if using lamp.",
                        "Insulate incubator with towel (ensure ventilation remains)."
                    ),
                    preventionTips = listOf("Pre-warm incubator 24h before setting eggs."),
                    riskFactor = RiskFactor.TEMP_LOW
                ))
            } else if (currentTemp > targets.tempMax) {
                // High Temp Logic
                val diff = currentTemp - targets.tempMax
                val confidence = if (diff > 1.0) ConfidenceLevel.HIGH else ConfidenceLevel.MEDIUM
                
                diagnoses.add(Diagnosis(
                    issue = "Temperature too High",
                    explanation = "High temperatures are lethal. Even short spikes >103°F (39°C) can cause blood rings, early death, or malformed chicks.",
                    confidence = confidence,
                    immediateActions = listOf(
                        "Open incubator lid briefly to vent heat.",
                        "Lower thermostat setting.",
                        "Move incubator to a cooler room spot."
                    ),
                    preventionTips = listOf("Keep incubator away from direct sunlight/windows."),
                    riskFactor = RiskFactor.TEMP_HIGH
                ))
            }
        }

        // 2. Humidity Analysis
        if (currentHumidity != null) {
            if (currentHumidity < targets.humidityMin) {
                // Low Humidity Logic
                val isLockdown = status.phase == IncubationPhase.LOCKDOWN || status.phase == IncubationPhase.HATCH_WINDOW
                
                if (isLockdown) {
                    // Critical for Lockdown
                    diagnoses.add(Diagnosis(
                        issue = "Humidity Critical (Lockdown)",
                        explanation = "Low humidity during hatch causes membranes to dry and toughen. Chicks may pip but be unable to zip (shrink-wrapped).",
                        confidence = ConfidenceLevel.HIGH,
                        immediateActions = listOf(
                            "Add warm water to reservoirs immediately.",
                            "Insert wet sponges or paper towels to boost surface area.",
                            "Do NOT open the incubator unnecessarily."
                        ),
                        preventionTips = listOf("Use a hygrometer to track humidity trends."),
                        riskFactor = RiskFactor.HUMIDITY_LOW
                    ))
                } else {
                    // Less critical for early stages
                    diagnoses.add(Diagnosis(
                        issue = "Humidity Low",
                        explanation = "Low humidity causes excessive evaporation, resulting in large air cells and dehydrated chicks.",
                        confidence = ConfidenceLevel.LOW,
                        immediateActions = listOf("Add water to incubator channels."),
                        preventionTips = listOf("Check water levels daily."),
                        riskFactor = RiskFactor.HUMIDITY_LOW
                    ))
                }
            } else if (currentHumidity > targets.humidityMax) {
                // High Humidity Logic
                diagnoses.add(Diagnosis(
                    issue = "Humidity High",
                    explanation = "Excess humidity prevents air cell growth. Chicks may drown in shell or be too large to rotate for hatching.",
                    confidence = ConfidenceLevel.MEDIUM,
                    immediateActions = listOf(
                        "Remove water/plugs to increase ventilation.",
                        "Add dry rice (in a sock) to absorb moisture."
                    ),
                    preventionTips = listOf("Run 'dry' incubation for first 18 days if local climate is humid."),
                    riskFactor = RiskFactor.HUMIDITY_HIGH
                ))
            }
        }

        // 3. Species-Specific Checks
        // Example: Goose Cooling
        if (config.speciesName.equals("Goose", ignoreCase = true) || config.speciesName.equals("Duck", ignoreCase = true)) {
             if (status.day >= (config.coolingStartDay ?: 999)) {
                 // Info diagnostic if we don't know if they cooled
                 // In a real app, we'd check if "Cooling" task was checked off for today.
                 // For now, we can just add a reminder diagnosis if currently missing sensor data?
                 // Let's rely on the Task system for this, but could add a "Troubleshoot" item here if user reports "None"
             }
        }

        return diagnoses
    }
}


