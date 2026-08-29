package com.pricetracker.app.domain

import com.pricetracker.app.domain.UrlNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlNormalizerTest {

    @Test fun `valid http and https urls are accepted`() {
        assertTrue(UrlNormalizer.isValid("https://example.com/product/1"))
        assertTrue(UrlNormalizer.isValid("http://example.com/product/1"))
    }

    @Test fun `urls without a scheme are rejected`() {
        assertFalse(UrlNormalizer.isValid("example.com/product/1"))
    }

    @Test fun `blank and garbage input is rejected`() {
        assertFalse(UrlNormalizer.isValid(""))
        assertFalse(UrlNormalizer.isValid("not a url"))
    }

    @Test fun `http and https of the same page normalize the same`() {
        assertEquals(
            UrlNormalizer.normalize("http://example.com/product/1"),
            UrlNormalizer.normalize("https://example.com/product/1")
        )
    }

    @Test fun `www prefix is ignored`() {
        assertEquals(
            UrlNormalizer.normalize("https://example.com/product/1"),
            UrlNormalizer.normalize("https://www.example.com/product/1")
        )
    }

    @Test fun `trailing slash is ignored`() {
        assertEquals(
            UrlNormalizer.normalize("https://example.com/product/1"),
            UrlNormalizer.normalize("https://example.com/product/1/")
        )
    }

    @Test fun `tracking parameters are stripped`() {
        assertEquals(
            UrlNormalizer.normalize("https://example.com/product/1"),
            UrlNormalizer.normalize("https://example.com/product/1?utm_source=ig&utm_campaign=x")
        )
    }

    @Test fun `different products remain distinct`() {
        assertTrue(
            UrlNormalizer.normalize("https://example.com/product/1") !=
                UrlNormalizer.normalize("https://example.com/product/2")
        )
    }

    @Test fun `extractDomain strips www`() {
        assertEquals("example.com", UrlNormalizer.extractDomain("https://www.example.com/product/1"))
    }
}
