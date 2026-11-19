package com.xoykor.qr.util

import com.xoykor.qr.model.DetectedLink
import com.xoykor.qr.model.QrContentType

object LinkDetector {

    private val schemeUrlRegex = Regex(
        """(?i)\b(?:https?://|ftp://)[^\s<>"'{}|\\^`]+"""
    )

    private val specialSchemeRegex = Regex(
        """(?i)\b(?:mailto:[^\s<>"']+|tel:[+\d().\-]{3,}|sms:[+\d().\-]{3,}|geo:-?\d+(?:\.\d+)?,-?\d+(?:\.\d+)?[^\s]*)"""
    )

    private val bareDomainRegex = Regex(
        """(?i)(?<![@/.\w])(?:www\.[a-z0-9-]+(?:\.[a-z0-9-]+)+|[a-z0-9-]+(?:\.[a-z0-9-]+)*\.(?:com|net|org|edu|gov|io|dev|app|br|uk|de|fr|es|pt|it|jp|co|me|info|biz|xyz|tv|ai|gg|ly|to|cc|tech|online|site|store|cloud)(?:\.[a-z]{2})?)(?::\d{1,5})?(?:/[^\s<>"'{}|\\^`]*)?"""
    )

    private val trailingPunctuation = setOf('.', ',', ';', ':', '!', '?', '"', '\'', '>', ']', '}')

    fun extractLinks(text: String): List<DetectedLink> {
        if (text.isBlank()) return emptyList()

        val candidates = mutableListOf<DetectedLink>()

        fun addMatches(regex: Regex) {
            for (match in regex.findAll(text)) {
                val raw = trimTrailingPunctuation(match.value)
                if (raw.isNotEmpty()) {
                    val start = match.range.first
                    val end = start + raw.length
                    candidates += DetectedLink(
                        startIndex = start,
                        endIndex = end,
                        displayText = raw,
                        normalizedUrl = normalizeUrl(raw)
                    )
                }
            }
        }

        addMatches(schemeUrlRegex)
        addMatches(specialSchemeRegex)
        addMatches(bareDomainRegex)

        // Sort by startIndex, prefer longer matches if starting at same index, remove overlaps
        val sorted = candidates.sortedWith(
            compareBy<DetectedLink> { it.startIndex }.thenByDescending { it.endIndex - it.startIndex }
        )

        val result = mutableListOf<DetectedLink>()
        var lastEnd = 0
        for (link in sorted) {
            if (link.startIndex >= lastEnd) {
                result += link
                lastEnd = link.endIndex
            }
        }
        return result
    }

    fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim()
        val lower = trimmed.lowercase()
        return when {
            lower.startsWith("http://") -> "http://" + trimmed.substring(7)
            lower.startsWith("https://") -> "https://" + trimmed.substring(8)
            lower.startsWith("ftp://") -> "ftp://" + trimmed.substring(6)
            lower.startsWith("mailto:") -> "mailto:" + trimmed.substring(7)
            lower.startsWith("tel:") -> "tel:" + trimmed.substring(4)
            lower.startsWith("sms:") -> "sms:" + trimmed.substring(4)
            lower.startsWith("geo:") -> "geo:" + trimmed.substring(4)
            else -> "https://$trimmed"
        }
    }

    fun isSingleLink(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        val links = extractLinks(trimmed)
        return links.size == 1 && links.first().displayText == trimmed
    }

    fun classifyContent(
        rawValue: String,
        mlKitType: Int = -1
    ): QrContentType {
        val trimmed = rawValue.trim()
        val lower = trimmed.lowercase()

        // ML Kit Barcode.TYPE_* constants:
        // TYPE_EMAIL=2, TYPE_PHONE=4, TYPE_SMS=6, TYPE_URL=8, TYPE_WIFI=9, TYPE_GEO=10
        return when {
            mlKitType == 8 || lower.startsWith("http://") || lower.startsWith("https://") || isSingleLink(trimmed) -> {
                when {
                    lower.startsWith("mailto:") -> QrContentType.EMAIL
                    lower.startsWith("tel:") -> QrContentType.PHONE
                    lower.startsWith("sms:") -> QrContentType.SMS
                    lower.startsWith("geo:") -> QrContentType.GEO
                    else -> QrContentType.URL
                }
            }
            mlKitType == 9 || lower.startsWith("wifi:") -> QrContentType.WIFI
            mlKitType == 2 || lower.startsWith("mailto:") || lower.startsWith("matmsg:") -> QrContentType.EMAIL
            mlKitType == 4 || lower.startsWith("tel:") -> QrContentType.PHONE
            mlKitType == 6 || lower.startsWith("sms:") || lower.startsWith("smsto:") -> QrContentType.SMS
            mlKitType == 10 || lower.startsWith("geo:") -> QrContentType.GEO
            else -> QrContentType.TEXT
        }
    }

    fun formatDisplayValue(rawValue: String, contentType: QrContentType): String {
        val trimmed = rawValue.trim()
        if (contentType == QrContentType.WIFI && trimmed.uppercase().startsWith("WIFI:")) {
            val body = trimmed.substring(5)
            val fields = body.split(";").filter { it.isNotBlank() }
            val ssid = fields.firstOrNull { it.startsWith("S:", ignoreCase = true) }?.substring(2)
            val auth = fields.firstOrNull { it.startsWith("T:", ignoreCase = true) }?.substring(2)
            val pass = fields.firstOrNull { it.startsWith("P:", ignoreCase = true) }?.substring(2)
            return buildString {
                if (!ssid.isNullOrBlank()) appendLine("Rede: $ssid")
                if (!auth.isNullOrBlank()) appendLine("Segurança: $auth")
                if (!pass.isNullOrBlank()) append("Senha: $pass")
            }.trim().ifEmpty { trimmed }
        }
        return trimmed
    }

    private fun trimTrailingPunctuation(input: String): String {
        var current = input
        while (current.isNotEmpty()) {
            val last = current.last()
            if (last in trailingPunctuation) {
                current = current.dropLast(1)
            } else if (last == ')') {
                val opens = current.count { it == '(' }
                val closes = current.count { it == ')' }
                if (closes > opens) {
                    current = current.dropLast(1)
                } else {
                    break
                }
            } else {
                break
            }
        }
        return current
    }
}
