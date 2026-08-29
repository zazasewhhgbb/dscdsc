package com.pricetracker.app.data.parser

/** Product data extracted from a page. [name] and [imageUrl] are optional (the UI shows a
 *  fallback), but [price] and [currency] are required for a product to be saveable. */
data class ParsedProduct(
    val name: String?,
    val imageUrl: String?,
    val price: Double,
    val currency: String?,
    val sourceUrl: String,
    /** Which extraction method produced this result, useful for debugging/tests. */
    val extractionMethod: String
)
