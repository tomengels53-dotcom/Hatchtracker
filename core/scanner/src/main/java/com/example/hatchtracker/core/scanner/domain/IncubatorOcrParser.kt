package com.example.hatchtracker.core.scanner.domain

import com.example.hatchtracker.core.scanner.api.IncubatorReadingDraft

class IncubatorOcrParser {

    fun parse(rawText: String): IncubatorReadingDraft {
        val lines = rawText.split("\n", "\r").map { it.trim() }.filter { it.isNotEmpty() }
        
        var bestTemp: Double? = null
        var bestHumidity: Int? = null
        val warnings = mutableListOf<String>()

        // 1. Find temperature (often numbers with decimals 36.5, 37,5 etc)
        // Regex looks for digits, an optional dot/comma, and more digits.
        val decimalRegex = Regex("""(\d{2})[.,](\d)""")
        val possibleTemps = mutableListOf<Double>()
        
        for (line in lines) {
            val matches = decimalRegex.findAll(line)
            for (match in matches) {
                // normalize comma to dot
                val tempStr = match.value.replace(',', '.')
                val tempDouble = tempStr.toDoubleOrNull()
                if (tempDouble != null) {
                    possibleTemps.add(tempDouble)
                }
            }
        }
        
        // Find best temp near 37C
        bestTemp = possibleTemps.minByOrNull { Math.abs(it - 37.5) }
        
        if (bestTemp != null) {
            if (bestTemp < 20.0 || bestTemp > 45.0) {
                warnings.add("Temperature ($bestTemp) is out of expected incubation range.")
            }
        } else {
            warnings.add("Could not confidently identify temperature.")
        }

        // 2. Find humidity (often an integer followed by % or near HUM)
        val percentRegex = Regex("""(\d{2,3})\s*%""")
        for (line in lines) {
            val match = percentRegex.find(line)
            if (match != null) {
                val humStr = match.groupValues[1]
                bestHumidity = humStr.toIntOrNull()
                break
            }
        }
        
        // Fallback for humidity: any 2-digit number 20..99 not used as temp
        if (bestHumidity == null) {
            val rawNumbers = Regex("""\b(\d{2})\b""").findAll(rawText).mapNotNull { it.groupValues[1].toIntOrNull() }.toList()
            // Assume the one around 40-70 might be humidity
            bestHumidity = rawNumbers.filter { it in 20..90 }.minByOrNull { Math.abs(it - 50) }
        }
        
        if (bestHumidity == null) {
            warnings.add("Could not confidently identify humidity.")
        }

        return IncubatorReadingDraft(
            temperatureC = bestTemp,
            humidityPercent = bestHumidity,
            rawText = rawText,
            warnings = warnings
        )
    }
}
