package com.pricetracker.app.domain

/**
 * Abstraction over "how do we get a current price for a product". Deliberately an interface
 * (project rule 12): version 1 ships [LocalPriceChecker], which fetches and parses the page
 * on-device. A future version can add a `RemotePriceChecker` that instead calls a backend
 * (see docs/BACKEND.md) WITHOUT any other part of the app needing to change - repositories,
 * workers and the UI only depend on this interface, never on the concrete implementation.
 */
interface PriceChecker {
    suspend fun checkProduct(url: String): PriceCheckOutcome
}

sealed class PriceCheckOutcome {
    data class Success(
        val name: String?,
        val imageUrl: String?,
        val price: Double,
        val currency: String?
    ) : PriceCheckOutcome()

    data class Error(val message: String) : PriceCheckOutcome()
}
