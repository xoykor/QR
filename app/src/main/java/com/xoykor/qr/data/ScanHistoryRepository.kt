package com.xoykor.qr.data

import android.content.Context
import com.xoykor.qr.model.QrContentType
import com.xoykor.qr.model.QrScanResult
import com.xoykor.qr.model.ScanSource
import com.xoykor.qr.util.LinkDetector
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ScanHistoryRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadHistory(): List<QrScanResult> {
        val rawJson = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val array = JSONArray(rawJson)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val rawValue = obj.optString("rawValue", "")
                    if (rawValue.isBlank()) continue
                    val sourceName = obj.optString("source", ScanSource.CAMERA.name)
                    val source = runCatching { ScanSource.valueOf(sourceName) }.getOrDefault(ScanSource.CAMERA)
                    val typeName = obj.optString("contentType", "")
                    val contentType = runCatching { QrContentType.valueOf(typeName) }
                        .getOrElse { LinkDetector.classifyContent(rawValue) }
                    val displayValue = LinkDetector.formatDisplayValue(rawValue, contentType)
                    val links = LinkDetector.extractLinks(displayValue)
                    val primaryUrl = obj.optString("primaryUrl", "")
                        .takeIf { it.isNotBlank() }
                        ?: links.firstOrNull()?.normalizedUrl
                    add(
                        QrScanResult(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            rawValue = rawValue,
                            displayValue = displayValue,
                            contentType = contentType,
                            links = links,
                            primaryUrl = primaryUrl,
                            source = source,
                            timestampMillis = obj.optLong("timestampMillis", System.currentTimeMillis())
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addScan(result: QrScanResult): List<QrScanResult> {
        val current = loadHistory().toMutableList()
        // Avoid duplicating identical consecutive scan at the top
        if (current.firstOrNull()?.rawValue == result.rawValue && current.firstOrNull()?.source == result.source) {
            current[0] = result
        } else {
            current.add(0, result)
        }
        val trimmed = current.take(MAX_ITEMS)
        saveHistory(trimmed)
        return trimmed
    }

    fun deleteScan(id: String): List<QrScanResult> {
        val updated = loadHistory().filterNot { it.id == id }
        saveHistory(updated)
        return updated
    }

    fun clearHistory(): List<QrScanResult> {
        prefs.edit().remove(KEY_HISTORY).apply()
        return emptyList()
    }

    private fun saveHistory(items: List<QrScanResult>) {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("rawValue", item.rawValue)
                put("contentType", item.contentType.name)
                put("primaryUrl", item.primaryUrl ?: "")
                put("source", item.source.name)
                put("timestampMillis", item.timestampMillis)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "qr_scan_history"
        private const val KEY_HISTORY = "items"
        private const val MAX_ITEMS = 50
    }
}
