package com.pricetracker.app.data.network

/** Outcome of trying to download a web page. Kept separate from parsing outcomes
 *  (see ParseResult) because a page can be fetched successfully yet still fail to parse. */
sealed class FetchResult {
    data class Success(val html: String, val finalUrl: String) : FetchResult()
    sealed class Failure : FetchResult() {
        data object NoConnection : Failure()
        data object Timeout : Failure()
        data class HttpError(val code: Int) : Failure()
        data class NetworkError(val message: String) : Failure()
        data object EmptyResponse : Failure()
        /** The site's robots.txt disallows automated access to this path. We never fetch a page
         *  its own robots policy tells us not to, even though nothing else would stop us. */
        data object RobotsDisallowed : Failure()
    }
}
