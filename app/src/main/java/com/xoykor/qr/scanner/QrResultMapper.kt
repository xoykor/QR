package com.xoykor.qr.scanner

import android.graphics.PointF
import android.graphics.RectF
import com.google.mlkit.vision.barcode.common.Barcode
import com.xoykor.qr.model.NormalizedBox
import com.xoykor.qr.model.QrScanResult
import com.xoykor.qr.model.ScanSource
import com.xoykor.qr.util.LinkDetector
import java.util.UUID

object QrResultMapper {

    fun fromRawText(
        rawValue: String,
        source: ScanSource,
        mlKitType: Int = -1,
        mlKitUrl: String? = null,
        normalizedBox: NormalizedBox? = null
    ): QrScanResult {
        val contentType = LinkDetector.classifyContent(rawValue, mlKitType)
        val displayValue = LinkDetector.formatDisplayValue(rawValue, contentType)
        val links = LinkDetector.extractLinks(displayValue)
        val primaryUrl = when {
            !mlKitUrl.isNullOrBlank() -> LinkDetector.normalizeUrl(mlKitUrl)
            links.isNotEmpty() -> links.first().normalizedUrl
            else -> null
        }

        return QrScanResult(
            id = UUID.randomUUID().toString(),
            rawValue = rawValue,
            displayValue = displayValue,
            contentType = contentType,
            links = links,
            primaryUrl = primaryUrl,
            normalizedBox = normalizedBox,
            source = source
        )
    }

    fun fromMlKitBarcode(
        barcode: Barcode,
        sourceWidth: Int,
        sourceHeight: Int,
        rotationDegrees: Int,
        mirrorX: Boolean,
        source: ScanSource
    ): QrScanResult? {
        val raw = barcode.rawValue ?: barcode.displayValue ?: return null
        if (raw.isBlank()) return null

        val box = barcode.boundingBox?.let { rect ->
            val mappedRect = mapRectToNormalized(
                left = rect.left.toFloat(),
                top = rect.top.toFloat(),
                right = rect.right.toFloat(),
                bottom = rect.bottom.toFloat(),
                sourceWidth = sourceWidth,
                sourceHeight = sourceHeight,
                rotationDegrees = rotationDegrees,
                mirrorX = mirrorX
            )
            val points = barcode.cornerPoints?.map { pt ->
                mapPointToNormalized(
                    x = pt.x.toFloat(),
                    y = pt.y.toFloat(),
                    sourceWidth = sourceWidth,
                    sourceHeight = sourceHeight,
                    rotationDegrees = rotationDegrees,
                    mirrorX = mirrorX
                )
            }.orEmpty()
            NormalizedBox(mappedRect, points)
        }

        return fromRawText(
            rawValue = raw,
            source = source,
            mlKitType = barcode.valueType,
            mlKitUrl = barcode.url?.url,
            normalizedBox = box
        )
    }

    /**
     * Maps coordinates from upright/rotated image space.
     * Note: ML Kit already returns boundingBox/cornerPoints in the *upright* coordinate space
     * when InputImage is created with rotationDegrees. The upright dimensions swap width/height
     * for 90 and 270 degree rotations.
     */
    private fun mapPointToNormalized(
        x: Float,
        y: Float,
        sourceWidth: Int,
        sourceHeight: Int,
        rotationDegrees: Int,
        mirrorX: Boolean
    ): PointF {
        val uprightW = if (rotationDegrees == 90 || rotationDegrees == 270) sourceHeight else sourceWidth
        val uprightH = if (rotationDegrees == 90 || rotationDegrees == 270) sourceWidth else sourceHeight
        val nx = (x / uprightW.coerceAtLeast(1)).coerceIn(0f, 1f)
        val ny = (y / uprightH.coerceAtLeast(1)).coerceIn(0f, 1f)
        return PointF(if (mirrorX) 1f - nx else nx, ny)
    }

    private fun mapRectToNormalized(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        sourceWidth: Int,
        sourceHeight: Int,
        rotationDegrees: Int,
        mirrorX: Boolean
    ): RectF {
        val p1 = mapPointToNormalized(left, top, sourceWidth, sourceHeight, rotationDegrees, mirrorX)
        val p2 = mapPointToNormalized(right, bottom, sourceWidth, sourceHeight, rotationDegrees, mirrorX)
        return RectF(
            minOf(p1.x, p2.x),
            minOf(p1.y, p2.y),
            maxOf(p1.x, p2.x),
            maxOf(p1.y, p2.y)
        )
    }
}
