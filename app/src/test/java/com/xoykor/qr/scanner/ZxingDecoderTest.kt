package com.xoykor.qr.scanner

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ZxingDecoderTest {

    @Test
    fun `decodes standard black-on-white qr code pixels`() {
        val payload = "https://github.com/xoykor/QR"
        val (pixels, w, h) = renderQrPixels(payload, inverted = false)
        val decoded = ZxingDecoder.decodePixels(pixels, w, h)
        assertNotNull(decoded)
        assertEquals(payload, decoded!!.text)
    }

    @Test
    fun `decodes inverted white-on-black qr code pixels`() {
        val payload = "Conteúdo com acentuação e link: https://example.org/teste?ok=1"
        val (pixels, w, h) = renderQrPixels(payload, inverted = true)
        val decoded = ZxingDecoder.decodePixels(pixels, w, h)
        assertNotNull(decoded)
        assertEquals(payload, decoded!!.text)
    }

    private fun renderQrPixels(
        content: String,
        size: Int = 256,
        inverted: Boolean = false
    ): Triple<IntArray, Int, Int> {
        val hints = mapOf(EncodeHintType.CHARACTER_SET to "UTF-8")
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val pixels = IntArray(size * size)
        val dark = if (inverted) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        val light = if (inverted) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        for (y in 0 until size) {
            for (x in 0 until size) {
                pixels[y * size + x] = if (matrix.get(x, y)) dark else light
            }
        }
        return Triple(pixels, size, size)
    }
}
