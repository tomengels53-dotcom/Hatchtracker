package com.example.hatchtracker.core.scanner.ui

import androidx.compose.runtime.*
import com.example.hatchtracker.core.scanner.api.BarcodeScanResult
import com.example.hatchtracker.core.scanner.mlkit.BarcodeAnalyzer

@Composable
fun ScanRingQrScreen(
    onResult: (BarcodeScanResult) -> Unit,
    onBack: () -> Unit
) {
    val analyzer = remember {
        BarcodeAnalyzer(onResult = { result ->
            onResult(result)
        })
    }
    DisposableEffect(analyzer) {
        onDispose { analyzer.close() }
    }
    
    ScanScaffold(
        title = "Scan Ring QR/Barcode",
        onBack = onBack,
        analyzer = analyzer,
        overlay = {
            ScannerOverlayGuide(widthPct = 0.6f, heightPct = 0.4f, title = "Align ring code in square")
        }
    )
}
