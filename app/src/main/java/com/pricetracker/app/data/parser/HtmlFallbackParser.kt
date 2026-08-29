package com.pricetracker.app.data.parser

import com.pricetracker.app.utils.CurrencyUtils
import org.jsoup.nodes.Document

/**
 * METHOD 3 & 4: standard <title>/meta description, plus a small set of very common ecommerce
 * HTML patterns (itemprop=price, common "price"/"product-price" class names, <meta name=twitter:*>).
 * Intentionally NOT a large per-website selector library (project rule 14 explicitly warns
 * against that) - this is a last-resort, best-effort pass. If it can't find a confident price,
 * it returns null and the caller reports "could not read this website" rather than guessing.
 */
object HtmlFallbackParser {

    private val PRICE_SELECTORS = listOf(
        "[itemprop=price]",
        "meta[itemprop=price]",
        ".price .amount",
        ".product-price",
        ".product__price",
        "[data-price]",
        ".price"
    )

    fun tryParse(document: Document, sourceUrl: String): ParsedProduct? {
        val name = document.selectFirst("meta[name=\"twitter:title\"]")?.attr("content")
            ?.takeIf { it.isNotBlank() }
            ?: document.selectFirst("title")?.text()?.takeIf { it.isNotBlank() }

        val image = document.selectFirst("meta[name=\"twitter:image\"]")?.attr("content")
            ?.takeIf { it.isNotBlank() }
            ?: document.selectFirst("meta[itemprop=image]")?.attr("content")
            ?: document.selectFirst("meta[name=description]")?.let { null } // no image fallback beyond this

        val priceRaw = findPriceText(document) ?: return null
        val price = PriceParser.parse(priceRaw) ?: return null
        val currency = CurrencyUtils.detectCurrency(priceRaw)
            ?: document.selectFirst("meta[itemprop=priceCurrency]")?.attr("content")
                ?.takeIf { CurrencyUtils.isKnownCode(it) }

        return ParsedProduct(
            name = name,
            imageUrl = image,
            price = price,
            currency = currency,
            sourceUrl = sourceUrl,
            extractionMethod = "HTML-fallback"
        )
    }

    private fun findPriceText(document: Document): String? {
        for (selector in PRICE_SELECTORS) {
            val el = document.selectFirst(selector) ?: continue
            val candidate = el.attr("content").takeIf { it.isNotBlank() }
                ?: el.attr("data-price").takeIf { it.isNotBlank() }
                ?: el.text().takeIf { it.isNotBlank() }
            if (candidate != null && Regex("\\d").containsMatchIn(candidate)) {
                return candidate
            }
        }
        return null
    }
}
