package com.example.hatchtracker.core.scanner.domain

import com.example.hatchtracker.core.scanner.api.ReceiptDraft
import java.text.SimpleDateFormat
import java.util.Locale

class ReceiptParser {

    fun parse(rawText: String): ReceiptDraft {
        val lines = rawText.split("\n", "\r").map { it.trim() }.filter { it.isNotEmpty() }
        
        var vendor: String? = null
        var total: Double? = null
        var dateMillis: Long? = null
        var currency: String = "EUR" // Default assumption as per requirements

        // 1. Vendor: first meaningful line with letters, not containing VAT/BTW/TOTAL
        for (line in lines) {
            val upper = line.uppercase()
            if (line.any { it.isLetter() } && 
                !upper.contains("VAT") && 
                !upper.contains("BTW") && 
                !upper.contains("TOTAL") &&
                line.length > 2) {
                vendor = line
                break
            }
        }

        // 2. Total: look for keywords
        val totalKeywords = listOf("TOTAL", "TOTAAL", "TVAC", "AMOUNT DUE", "SUM")
        val amountRegex = Regex("""(\d+[.,]\d{2})""")
        
        // First pass: look specifically near total keywords
        for (line in lines) {
            val upper = line.uppercase()
            if (totalKeywords.any { upper.contains(it) }) {
                val match = amountRegex.find(line)
                if (match != null) {
                    val amountStr = match.value.replace(',', '.')
                    total = amountStr.toDoubleOrNull()
                    break
                }
            }
        }
        
        // Second pass: fallback to max monetary value
        if (total == null) {
            val allAmounts = amountRegex.findAll(rawText).mapNotNull { it.value.replace(',', '.').toDoubleOrNull() }.toList()
            if (allAmounts.isNotEmpty()) {
                total = allAmounts.maxOrNull() // Usually the receipt total is the largest number on the ticket
            }
        }

        // 3. Date
        val dateFormats = listOf(
            Regex("""(\d{2})[/.-](\d{2})[/.-](\d{4})"""), // DD/MM/YYYY or similar
            Regex("""(\d{4})[/.-](\d{2})[/.-](\d{2})""")  // YYYY-MM-DD
        )
        
        for (line in lines) {
            for (format in dateFormats) {
                val match = format.find(line)
                if (match != null) {
                    // Try to parse it
                    try {
                        val str = match.value.replace('-', '/').replace('.', '/')
                        val pattern = if (format == dateFormats[0]) "dd/MM/yyyy" else "yyyy/MM/dd"
                        val sdf = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }

                        val date = sdf.parse(str)
                        if (date != null) {
                            dateMillis = date.time
                            break
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            if (dateMillis != null) break
        }

        // 4. Currency: Detect simple symbols
        if (rawText.contains("$")) currency = "USD"
        else if (rawText.contains("£")) currency = "GBP"
        else if (rawText.contains("€")) currency = "EUR"

        return ReceiptDraft(
            vendor = vendor,
            dateEpochMillis = dateMillis,
            total = total,
            currency = currency,
            rawText = rawText,
            lines = lines
        )
    }
}
