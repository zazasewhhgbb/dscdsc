package com.pricetracker.app.data.parser

import com.pricetracker.app.utils.CurrencyUtils
import org.jsoup.nodes.Document

/**
 * METHOD 2: Open Graph / product-specific meta tags (og:title, og:image, product:price:amount,
 * etc). Common on stores that don't emit full JSON-LD Product data but still tag their pages
 * for social sharing.
 */
object OpenGraphParser {

    fun tryParse(document: Document, sourceUrl: String): ParsedProduct? {
        val title = meta(document, "og:title")
        val image = meta(document, "og:image")

        val priceRaw = meta(document, "product:price:amount")
            ?: meta(document, "og:price:amount")
        val currencyMeta = meta(document, "product:price:currency")
            ?: meta(document, "og:price:currency")

        val price = priceRaw?.let { PriceParser.parse(it) } ?: return null
        val currency = currencyMeta?.takeIf { CurrencyUtils.isKnownCode(it) }
            ?: priceRaw.let { CurrencyUtils.detectCurrency(it) }

        return ParsedProduct(
            name = title,
            imageUrl = image,
            price = price,
            currency = currency,
            sourceUrl = sourceUrl,
            extractionMethod = "OpenGraph"
        )
    }

    private fun meta(document: Document, property: String): String? {
        val el = document.selectFirst("meta[property=\"$property\"]")
            ?: document.selectFirst("meta[name=\"$property\"]")
        return el?.attr("content")?.takeIf { it.isNotBlank() }
    }
}
