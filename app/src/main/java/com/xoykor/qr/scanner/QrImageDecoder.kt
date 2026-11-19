package com.xoykor.qr.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.xoykor.qr.model.NormalizedBox
import com.xoykor.qr.model.QrScanResult
import com.xoykor.qr.model.ScanSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ImageDecodeOutcome(
    val bitmap: Bitmap,
    val results: List<QrScanResult>
)

object QrImageDecoder {

    private const val MAX_DIMENSION = 2048

    private val scanner by lazy {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(options)
    }

    suspend fun decodeFromUri(context: Context, uri: Uri): ImageDecodeOutcome =
        withContext(Dispatchers.IO) {
            val bitmap = loadScaledSoftwareBitmap(context, uri)
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val mlKitBarcodes = scanWithMlKit(inputImage)

            val results = mlKitBarcodes.mapNotNull { barcode ->
                QrResultMapper.fromMlKitBarcode(
                    barcode = barcode,
                    sourceWidth = bitmap.width,
                    sourceHeight = bitmap.height,
                    rotationDegrees = 0,
                    mirrorX = false,
                    source = ScanSource.IMAGE
                )
            }.toMutableList()

            if (results.isEmpty()) {
                val zxingResult = ZxingDecoder.decodeBitmap(bitmap)
                if (zxingResult != null) {
                    val box = buildNormalizedBoxFromPoints(
                        points = zxingResult.resultPoints,
                        width = bitmap.width,
                        height = bitmap.height
                    )
                    results += QrResultMapper.fromRawText(
                        rawValue = zxingResult.text,
                        source = ScanSource.IMAGE,
                        normalizedBox = box
                    )
                }
            }

            ImageDecodeOutcome(bitmap = bitmap, results = results)
        }

    private fun loadScaledSoftwareBitmap(context: Context, uri: Uri): Bitmap {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val w = info.size.width
            val h = info.size.height
            val maxSide = maxOf(w, h)
            if (maxSide > MAX_DIMENSION) {
                val scale = MAX_DIMENSION.toFloat() / maxSide.toFloat()
                decoder.setTargetSize(
                    (w * scale).toInt().coerceAtLeast(1),
                    (h * scale).toInt().coerceAtLeast(1)
                )
            }
        }
    }

    private suspend fun scanWithMlKit(inputImage: InputImage): List<Barcode> =
        suspendCancellableCoroutine { cont ->
            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    if (cont.isActive) cont.resume(barcodes)
                }
                .addOnFailureListener { error ->
                    if (cont.isActive) cont.resumeWithException(error)
                }
        }

    private fun buildNormalizedBoxFromPoints(
        points: List<Pair<Float, Float>>,
        width: Int,
        height: Int
    ): NormalizedBox? {
        if (points.isEmpty() || width <= 0 || height <= 0) return null
        val normPts = points.map { (x, y) ->
            PointF((x / width).coerceIn(0f, 1f), (y / height).coerceIn(0f, 1f))
        }
        val minX = normPts.minOf { it.x }
        val minY = normPts.minOf { it.y }
        val maxX = normPts.maxOf { it.x }
        val maxY = normPts.maxOf { it.y }
        return NormalizedBox(
            boundingBox = RectF(minX, minY, maxX, maxY),
            cornerPoints = normPts
        )
    }
}
