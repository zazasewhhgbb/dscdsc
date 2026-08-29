package com.pricetracker.app.utils

/**
 * Detects an ISO 4217 currency code from raw page text. Deliberately conservative: symbols
 * that are shared by multiple currencies (e.g. "kr" is used by NOK, SEK and DKK) are only
 * resolved when no unambiguous ISO code is present elsewhere in the text - if we truly can't
 * tell, we return null rather than guess (project rule 16: never invent currency/price data).
 */
object CurrencyUtils {

    val KNOWN_CODES = setOf(
        "NOK", "SEK", "DKK", "EUR", "USD", "GBP", "CHF",
        "PLN", "CZK", "ISK", "JPY", "CNY", "CAD", "AUD", "NZD"
    )

    private val SYMBOL_TO_UNAMBIGUOUS_CODE = mapOf(
        "€" to "EUR",
        "$" to "USD",
        "£" to "GBP",
        "¥" to "JPY"
    )

    /** Ambiguous multi-currency symbols: we only use these as a last resort, and mark them
     *  distinctly so callers can decide whether to trust a bare symbol match. */
    private val AMBIGUOUS_SYMBOLS = setOf("kr", "Kr", "KR")

    fun detectCurrency(text: String): String? {
        val upper = text.uppercase()
        for (code in KNOWN_CODES) {
            if (Regex("\\b$code\\b").containsMatchIn(upper)) return code
        }
        for ((symbol, code) in SYMBOL_TO_UNAMBIGUOUS_CODE) {
            if (text.contains(symbol)) return code
        }
        // Ambiguous "kr" is intentionally not resolved to a specific code here; callers that
        // have additional context (e.g. the page's TLD or hreflang) may resolve it themselves.
        return null
    }

    fun isKnownCode(code: String?): Boolean = code != null && code.uppercase() in KNOWN_CODES
}
