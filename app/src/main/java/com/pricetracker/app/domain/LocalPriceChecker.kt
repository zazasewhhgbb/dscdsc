package com.pricetracker.app.domain

import com.pricetracker.app.data.network.FetchResult
import com.pricetracker.app.data.network.PageFetcher
import com.pricetracker.app.data.parser.ParseResult
import com.pricetracker.app.data.parser.ProductParser

/**
 * On-device implementation of [PriceChecker]: downloads the page with [PageFetcher] and runs
 * it through [ProductParser]. This is what the app uses in v1 (see project rule 12/36 about
 * keeping this replaceable by a future backend implementation).
 */
class LocalPriceChecker(
    private val fetcher: PageFetcher = PageFetcher()
) : PriceChecker {

    override suspend fun checkProduct(url: String): PriceCheckOutcome {
        return when (val fetchResult = fetcher.fetch(url)) {
            is FetchResult.Success -> {
                when (val parsed = ProductParser.parse(fetchResult.html, fetchResult.finalUrl)) {
                    is ParseResult.Success -> PriceCheckOutcome.Success(
                        name = parsed.product.name,
                        imageUrl = parsed.product.imageUrl,
                        price = parsed.product.price,
                        currency = parsed.product.currency
                    )
                    is ParseResult.NoPriceFound ->
                        PriceCheckOutcome.Error("A price could not be found on this page.")
                    is ParseResult.AmbiguousPrice ->
                        PriceCheckOutcome.Error("The price format on this page could not be confidently understood.")
                    is ParseResult.UnreadablePage ->
                        PriceCheckOutcome.Error("This page could not be read.")
                }
            }
            is FetchResult.Failure.NoConnection ->
                PriceCheckOutcome.Error("No internet connection.")
            is FetchResult.Failure.Timeout ->
                PriceCheckOutcome.Error("The website took too long to respond.")
            is FetchResult.Failure.HttpError -> {
                val msg = when (fetchResult.code) {
                    403 -> "This website blocked the request (403)."
                    404 -> "This page could not be found (404)."
                    429 -> "This website is rate-limiting requests (429)."
                    in 500..599 -> "The website's server returned an error (${fetchResult.code})."
                    else -> "The website returned an error (${fetchResult.code})."
                }
                PriceCheckOutcome.Error(msg)
            }
            is FetchResult.Failure.EmptyResponse ->
                PriceCheckOutcome.Error("The website returned an empty response.")
            is FetchResult.Failure.RobotsDisallowed ->
                PriceCheckOutcome.Error("This website's robots.txt doesn't allow automated access to this page, so it can't be tracked.")
            is FetchResult.Failure.NetworkError ->
                PriceCheckOutcome.Error("Unable to read this website automatically. It may block automated access or require JavaScript.")
        }
    }
}
