package com.pricetracker.app.parser

import com.pricetracker.app.data.parser.ParseResult
import com.pricetracker.app.data.parser.ProductParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductParserTest {

    private fun load(name: String): String {
        val stream = javaClass.classLoader!!.getResourceAsStream("sample_html/$name")
            ?: error("Missing test fixture: $name")
        return stream.bufferedReader().use { it.readText() }
    }

    @Test fun `simple JSON-LD product is parsed`() {
        val result = ProductParser.parse(load("simple_jsonld.html"), "https://example.com/p/1")
        assertTrue(result is ParseResult.Success)
        val product = (result as ParseResult.Success).product
        assertEquals("Sony WH-1000XM6", product.name)
        assertEquals(1499.0, product.price, 0.001)
        assertEquals("NOK", product.currency)
        assertEquals("JSON-LD", product.extractionMethod)
    }

    @Test fun `JSON-LD with multiple offers picks the lowest current price`() {
        val result = ProductParser.parse(load("jsonld_with_offers_array.html"), "https://example.com/p/2")
        assertTrue(result is ParseResult.Success)
        val product = (result as ParseResult.Success).product
        assertEquals(999.0, product.price, 0.001) // not the 1299 original/high offer
    }

    @Test fun `sale price is preferred over the crossed-out original`() {
        val result = ProductParser.parse(load("sale_price.html"), "https://example.com/p/3")
        assertTrue(result is ParseResult.Success)
        val product = (result as ParseResult.Success).product
        assertEquals(699.50, product.price, 0.001)
    }

    @Test fun `product without an image still parses`() {
        val result = ProductParser.parse(load("no_image.html"), "https://example.com/p/4")
        assertTrue(result is ParseResult.Success)
        val product = (result as ParseResult.Success).product
        assertNull(product.imageUrl)
        assertEquals(249.0, product.price, 0.001)
    }

    @Test fun `page with no price anywhere fails gracefully`() {
        val result = ProductParser.parse(load("no_price.html"), "https://example.com/p/5")
        assertTrue(result is ParseResult.NoPriceFound)
    }

    @Test fun `invalid JSON-LD falls back to Open Graph`() {
        val result = ProductParser.parse(load("invalid_jsonld.html"), "https://example.com/p/6")
        // No price is present in the OG tags in this fixture, so it correctly reports failure
        // rather than crashing on the malformed JSON-LD block.
        assertTrue(result is ParseResult.NoPriceFound)
    }

    @Test fun `multiple JSON-LD blocks - the Product one is found`() {
        val result = ProductParser.parse(load("multiple_jsonld_blocks.html"), "https://example.com/p/7")
        assertTrue(result is ParseResult.Success)
        val product = (result as ParseResult.Success).product
        assertEquals("Coffee Grinder", product.name)
        assertEquals(899.0, product.price, 0.001)
    }

    @Test fun `Open Graph only page is parsed via method 2`() {
        val result = ProductParser.parse(load("open_graph_only.html"), "https://example.com/p/8")
        assertTrue(result is ParseResult.Success)
        val product = (result as ParseResult.Success).product
        assertEquals("Wireless Mouse", product.name)
        assertEquals(299.0, product.price, 0.001)
        assertEquals("NOK", product.currency)
        assertEquals("OpenGraph", product.extractionMethod)
    }

    @Test fun `@graph wrapped JSON-LD is parsed`() {
        val result = ProductParser.parse(load("jsonld_graph_wrapper.html"), "https://example.com/p/9")
        assertTrue(result is ParseResult.Success)
        val product = (result as ParseResult.Success).product
        assertEquals("Backpack", product.name)
        assertEquals(549.0, product.price, 0.001)
        assertEquals("DKK", product.currency)
    }

    @Test fun `HTML fallback via itemprop attributes`() {
        val result = ProductParser.parse(load("html_fallback_itemprop.html"), "https://example.com/p/10")
        assertTrue(result is ParseResult.Success)
        val product = (result as ParseResult.Success).product
        assertEquals(349.90, product.price, 0.001)
        assertEquals("NOK", product.currency)
        assertEquals("HTML-fallback", product.extractionMethod)
    }
}
