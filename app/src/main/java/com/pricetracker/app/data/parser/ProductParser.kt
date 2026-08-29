package com.pricetracker.app.data.parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Orchestrates product extraction from raw HTML, trying each strategy in priority order
 * (project rule 14):
 *   1. JSON-LD structured data
 *   2. Open Graph / product meta tags
 *   3/4. Standard HTML metadata + common ecommerce HTML patterns
 *
 * The first strategy that yields BOTH a name-or-image AND a confidently-parsed price wins.
 * If a strategy finds a price string it cannot confidently parse (e.g. "call for price",
 * or a genuinely ambiguous number format), extraction moves on to the next strategy rather
 * than guessing - only if every strategy fails do we report "no price found".
 */
object ProductParser {

    fun parse(html: String, sourceUrl: String): ParseResult {
        val document: Document = try {
            Jsoup.parse(html, sourceUrl)
        } catch (e: Exception) {
            return ParseResult.UnreadablePage()
        }

        val strategies = listOf(
            { JsonLdParser.tryParse(document, sourceUrl) },
            { OpenGraphParser.tryParse(document, sourceUrl) },
            { HtmlFallbackParser.tryParse(document, sourceUrl) }
        )

        var sawPriceLikeTextButFailedToParse = false

        for (strategy in strategies) {
            val product = try {
                strategy()
            } catch (e: Exception) {
                null
            }
            if (product != null) {
                return ParseResult.Success(product)
            }
        }

        // None of the strategies produced a confident result. Distinguish "we found something
        // that looked like a price but couldn't safely parse it" from "no price at all" only
        // where we can cheaply check; otherwise default to NoPriceFound so we never invent data.
        if (containsAmbiguousPriceLikeText(document)) {
            sawPriceLikeTextButFailedToParse = true
        }

        return if (sawPriceLikeTextButFailedToParse) {
            ParseResult.AmbiguousPrice()
        } else {
            ParseResult.NoPriceFound()
        }
    }

    private fun containsAmbiguousPriceLikeText(document: Document): Boolean {
        val bodyText = document.body()?.text().orEmpty()
        // A very rough heuristic: numbers with 4+ digits after a separator, which our
        // PriceParser deliberately refuses to interpret.
        return Regex("\\d[.,]\\d{4,}").containsMatchIn(bodyText)
    }
}
