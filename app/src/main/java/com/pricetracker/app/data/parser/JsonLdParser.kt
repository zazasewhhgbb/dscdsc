package com.pricetracker.app.data.parser

import com.pricetracker.app.utils.CurrencyUtils
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.nodes.Document

/**
 * METHOD 1 (highest priority): schema.org Product data embedded as JSON-LD
 * (`<script type="application/ld+json">`). This is the most reliable source when present
 * because it's structured data the site itself publishes for search engines, so we try it
 * before any HTML-guessing methods.
 *
 * Handles:
 *  - a single Product object
 *  - an array of JSON-LD objects (finds the Product among them)
 *  - a "@graph" wrapper (used by many SEO plugins, e.g. Yoast/WooCommerce)
 *  - multiple <script> blocks on the same page
 *  - `offers` as a single object OR an array of offers (picks the lowest available price,
 *    which is normally the current selling price rather than a crossed-out original price)
 */
object JsonLdParser {

    fun tryParse(document: Document, sourceUrl: String): ParsedProduct? {
        val scripts = document.select("script[type=application/ld+json]")
        for (script in scripts) {
            val raw = script.data().ifBlank { script.html() }
            if (raw.isBlank()) continue

            val product = try {
                extractProductFromJson(raw, sourceUrl)
            } catch (e: Exception) {
                null // Malformed JSON-LD on this block; try the next script tag.
            }
            if (product != null) return product
        }
        return null
    }

    private fun extractProductFromJson(raw: String, sourceUrl: String): ParsedProduct? {
        val trimmed = raw.trim()
        val candidates: List<JSONObject> = when {
            trimmed.startsWith("[") -> {
                val array = JSONArray(trimmed)
                (0 until array.length()).mapNotNull { array.optJSONObject(it) }
            }
            trimmed.startsWith("{") -> {
                val obj = JSONObject(trimmed)
                if (obj.has("@graph")) {
                    val graph = obj.optJSONArray("@graph") ?: JSONArray()
                    (0 until graph.length()).mapNotNull { graph.optJSONObject(it) }
                } else {
                    listOf(obj)
                }
            }
            else -> emptyList()
        }

        for (obj in candidates) {
            if (isProductType(obj)) {
                productFromObject(obj, sourceUrl)?.let { return it }
            }
        }
        return null
    }

    private fun isProductType(obj: JSONObject): Boolean {
        val type = obj.opt("@type") ?: return false
        return when (type) {
            is String -> type.equals("Product", ignoreCase = true)
            is JSONArray -> (0 until type.length()).any {
                (type.opt(it) as? String)?.equals("Product", ignoreCase = true) == true
            }
            else -> false
        }
    }

    private fun productFromObject(obj: JSONObject, sourceUrl: String): ParsedProduct? {
        val name = obj.optString("name").takeIf { it.isNotBlank() }
        val image = extractImage(obj)

        val offers = obj.opt("offers") ?: return null
        val offerList: List<JSONObject> = when (offers) {
            is JSONObject -> listOf(offers)
            is JSONArray -> (0 until offers.length()).mapNotNull { offers.optJSONObject(it) }
            else -> emptyList()
        }
        if (offerList.isEmpty()) return null

        // If multiple offers exist, prefer the lowest priced one that is actually available -
        // this is normally the current selling price, not a stale/original listing.
        var bestPrice: Double? = null
        var bestCurrency: String? = null
        for (offer in offerList) {
            val priceRaw = offer.opt("price")?.toString() ?: offer.opt("lowPrice")?.toString()
            val price = priceRaw?.let { PriceParser.parse(it) } ?: continue
            val currency = offer.optString("priceCurrency").takeIf { it.isNotBlank() }
                ?: CurrencyUtils.detectCurrency(priceRaw)

            if (bestPrice == null || price < bestPrice!!) {
                bestPrice = price
                bestCurrency = currency
            }
        }

        val price = bestPrice ?: return null
        return ParsedProduct(
            name = name,
            imageUrl = image,
            price = price,
            currency = bestCurrency,
            sourceUrl = sourceUrl,
            extractionMethod = "JSON-LD"
        )
    }

    private fun extractImage(obj: JSONObject): String? {
        val image = obj.opt("image") ?: return null
        return when (image) {
            is String -> image
            is JSONArray -> if (image.length() > 0) image.optString(0) else null
            is JSONObject -> image.optString("url").takeIf { it.isNotBlank() }
            else -> null
        }
    }
}
