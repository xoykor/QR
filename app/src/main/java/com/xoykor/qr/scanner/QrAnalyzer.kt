package com.xoykor.qr.scanner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.xoykor.qr.model.QrScanResult
import com.xoykor.qr.model.ScanSource

class QrAnalyzer(
    private val isFrontCamera: () -> Boolean,
    private val isPaused: () -> Boolean,
    private val onQrDetected: (List<QrScanResult>, Int, Int) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    @Volatile
    private var isProcessing = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isPaused() || isProcessing) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        isProcessing = true
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val sourceW = imageProxy.width
        val sourceH = imageProxy.height
        val uprightW = if (rotationDegrees == 90 || rotationDegrees == 270) sourceH else sourceW
        val uprightH = if (rotationDegrees == 90 || rotationDegrees == 270) sourceW else sourceH
        val mirrorX = isFrontCamera()

        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)
        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val mapped = barcodes.mapNotNull { barcode ->
                    QrResultMapper.fromMlKitBarcode(
                        barcode = barcode,
                        sourceWidth = sourceW,
                        sourceHeight = sourceH,
                        rotationDegrees = rotationDegrees,
                        mirrorX = mirrorX,
                        source = ScanSource.CAMERA
                    )
                }
                onQrDetected(mapped, uprightW, uprightH)
            }
            .addOnCompleteListener {
                isProcessing = false
                imageProxy.close()
            }
    }

    fun close() {
        scanner.close()
    }
}
