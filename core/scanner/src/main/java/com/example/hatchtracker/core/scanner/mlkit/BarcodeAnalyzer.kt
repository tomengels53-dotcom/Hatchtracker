package com.example.hatchtracker.core.scanner.mlkit

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.hatchtracker.core.scanner.api.BarcodeScanResult
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.io.Closeable

class BarcodeAnalyzer(
    private val onResult: (BarcodeScanResult) -> Unit
) : ImageAnalysis.Analyzer, Closeable {

    private val options = BarcodeScannerOptions.Builder()
        .build() // detect all formats
        
    private val scanner = BarcodeScanning.getClient(options)
    private var isAnalyzing = false
    private var isFinished = false

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        if (isFinished || isAnalyzing) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            isAnalyzing = true
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val first = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                    if (first != null && !isFinished) {
                        isFinished = true
                        onResult(BarcodeScanResult(first.rawValue!!, first.format))
                    }
                }
                .addOnCompleteListener {
                    isAnalyzing = false
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    override fun close() {
        scanner.close()
    }
}
