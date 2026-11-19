package com.xoykor.qr.model

import android.graphics.PointF
import android.graphics.RectF

enum class ScanSource(val label: String) {
    CAMERA("Câmera"),
    IMAGE("Imagem")
}

enum class QrContentType(val label: String) {
    URL("Link"),
    EMAIL("E-mail"),
    PHONE("Telefone"),
    SMS("SMS"),
    WIFI("Wi-Fi"),
    GEO("Localização"),
    TEXT("Texto")
}

data class DetectedLink(
    val startIndex: Int,
    val endIndex: Int,
    val displayText: String,
    val normalizedUrl: String
)

/**
 * Normalized coordinates in [0f, 1f] relative to the displayed image or camera frame.
 */
data class NormalizedBox(
    val boundingBox: RectF,
    val cornerPoints: List<PointF> = emptyList()
)

data class QrScanResult(
    val id: String,
    val rawValue: String,
    val displayValue: String,
    val contentType: QrContentType,
    val links: List<DetectedLink>,
    val primaryUrl: String?,
    val normalizedBox: NormalizedBox? = null,
    val source: ScanSource,
    val timestampMillis: Long = System.currentTimeMillis()
)
