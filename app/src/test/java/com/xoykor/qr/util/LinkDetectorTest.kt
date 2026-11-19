package com.xoykor.qr.util

import com.xoykor.qr.model.QrContentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkDetectorTest {

    @Test
    fun `detects full https url`() {
        val text = "https://github.com/xoykor/QR"
        val links = LinkDetector.extractLinks(text)
        assertEquals(1, links.size)
        assertEquals("https://github.com/xoykor/QR", links.first().normalizedUrl)
        assertTrue(LinkDetector.isSingleLink(text))
        assertEquals(QrContentType.URL, LinkDetector.classifyContent(text))
    }

    @Test
    fun `detects bare domain and normalizes to https`() {
        val text = "github.com/xoykor/QR"
        val links = LinkDetector.extractLinks(text)
        assertEquals(1, links.size)
        assertEquals("https://github.com/xoykor/QR", links.first().normalizedUrl)
        assertTrue(LinkDetector.isSingleLink(text))
        assertEquals(QrContentType.URL, LinkDetector.classifyContent(text))
    }

    @Test
    fun `extracts multiple links embedded inside plain text and strips trailing punctuation`() {
        val text = "Acesse https://example.com/guia. Ou visite www.wikipedia.org/wiki/QR_code_(test)!"
        val links = LinkDetector.extractLinks(text)
        assertEquals(2, links.size)
        assertEquals("https://example.com/guia", links[0].displayText)
        assertEquals("https://example.com/guia", links[0].normalizedUrl)
        assertEquals("www.wikipedia.org/wiki/QR_code_(test)", links[1].displayText)
        assertEquals("https://www.wikipedia.org/wiki/QR_code_(test)", links[1].normalizedUrl)
        assertFalse(LinkDetector.isSingleLink(text))
        assertEquals(QrContentType.TEXT, LinkDetector.classifyContent(text))
    }

    @Test
    fun `detects mailto and tel schemes`() {
        val mail = LinkDetector.extractLinks("Contato: mailto:teste@exemplo.com")
        assertEquals(1, mail.size)
        assertEquals("mailto:teste@exemplo.com", mail.first().normalizedUrl)

        val phone = LinkDetector.extractLinks("tel:+5511999999999")
        assertEquals(1, phone.size)
        assertEquals(QrContentType.PHONE, LinkDetector.classifyContent("tel:+5511999999999"))
    }

    @Test
    fun `formats wifi qr payload cleanly and finds no web links`() {
        val wifi = "WIFI:T:WPA;S:MinhaRede;P:Senha123;;"
        assertEquals(QrContentType.WIFI, LinkDetector.classifyContent(wifi))
        val formatted = LinkDetector.formatDisplayValue(wifi, QrContentType.WIFI)
        assertTrue(formatted.contains("Rede: MinhaRede"))
        assertTrue(formatted.contains("Segurança: WPA"))
        assertTrue(formatted.contains("Senha: Senha123"))
        assertTrue(LinkDetector.extractLinks(formatted).isEmpty())
    }

    @Test
    fun `plain text without links produces empty link list`() {
        val text = "Código de verificação: 849201"
        assertEquals(QrContentType.TEXT, LinkDetector.classifyContent(text))
        assertTrue(LinkDetector.extractLinks(text).isEmpty())
    }
}
