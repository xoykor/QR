package com.xoykor.qr.scanner

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ResultPoint
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer

data class ZxingDecodedQr(
    val text: String,
    val resultPoints: List<Pair<Float, Float>>
)

object ZxingDecoder {

    private val hints: Map<DecodeHintType, Any> = mapOf(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.CHARACTER_SET to "UTF-8"
    )

    fun decodeBitmap(bitmap: Bitmap): ZxingDecodedQr? {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return null

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        return decodePixels(pixels, width, height)
    }

    fun decodePixels(pixels: IntArray, width: Int, height: Int): ZxingDecodedQr? {
        val source: LuminanceSource = RGBLuminanceSource(width, height, pixels)
        val reader = MultiFormatReader().apply { setHints(hints) }

        val candidates = listOf(
            BinaryBitmap(HybridBinarizer(source)),
            BinaryBitmap(GlobalHistogramBinarizer(source)),
            BinaryBitmap(HybridBinarizer(source.invert())),
            BinaryBitmap(GlobalHistogramBinarizer(source.invert()))
        )

        for (binaryBitmap in candidates) {
            try {
                reader.reset()
                val result = reader.decodeWithState(binaryBitmap)
                val text = result.text
                if (!text.isNullOrBlank()) {
                    val points = result.resultPoints
                        ?.filterNotNull()
                        ?.map { pt: ResultPoint -> pt.x to pt.y }
                        .orEmpty()
                    return ZxingDecodedQr(text = text, resultPoints = points)
                }
            } catch (_: Exception) {
                // Try next binarizer / inverted candidate
            }
        }
        return null
    }
}
