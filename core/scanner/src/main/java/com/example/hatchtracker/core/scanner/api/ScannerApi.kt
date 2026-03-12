package com.example.hatchtracker.core.scanner.api

enum class TextPurpose { 
    INCUBATOR, 
    RECEIPT 
}

sealed interface ScanMode {
    data object Barcode : ScanMode
    data class Text(val purpose: TextPurpose) : ScanMode
}

data class BarcodeScanResult(
    val rawValue: String,
    val format: Int
)

data class IncubatorReadingDraft(
    val temperatureC: Double?,
    val humidityPercent: Int?,
    val rawText: String,
    val warnings: List<String> = emptyList()
)

data class ReceiptDraft(
    val vendor: String?,
    val dateEpochMillis: Long?,
    val total: Double?,
    val currency: String?,
    val rawText: String,
    val lines: List<String>
)
