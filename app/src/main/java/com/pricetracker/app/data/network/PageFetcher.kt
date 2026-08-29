package com.pricetracker.app.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Downloads a product page's raw HTML. Deliberately does nothing beyond a plain GET request:
 * no headless browser, no JS execution, no CAPTCHA solving (see project rule 34 - the app must
 * not attempt to bypass anti-bot protections). Pages that require JavaScript will simply fail
 * to produce parseable data downstream, and the user is told so.
 *
 * Before requesting the page itself, checks the site's robots.txt via [robotsChecker] and
 * refuses to proceed if it's disallowed for us - e.g. Dressmann.com disallows automated access
 * to product pages, so we must not fetch them, full stop, regardless of whether the request
 * would otherwise succeed.
 */
class PageFetcher(
    private val client: okhttp3.OkHttpClient = HttpClientProvider.client,
    private val robotsChecker: RobotsTxtChecker = RobotsTxtChecker(client)
) {

    suspend fun fetch(url: String): FetchResult = withContext(Dispatchers.IO) {
        if (!robotsChecker.isAllowed(url)) {
            return@withContext FetchResult.Failure.RobotsDisallowed
        }

        val request = try {
            Request.Builder()
                .url(url)
                .header("User-Agent", HttpClientProvider.USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()
        } catch (e: IllegalArgumentException) {
            return@withContext FetchResult.Failure.NetworkError("Invalid URL")
        }

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext FetchResult.Failure.HttpError(response.code)
                }
                val body = response.body?.string()
                if (body.isNullOrBlank()) {
                    return@withContext FetchResult.Failure.EmptyResponse
                }
                FetchResult.Success(html = body, finalUrl = response.request.url.toString())
            }
        } catch (e: SocketTimeoutException) {
            FetchResult.Failure.Timeout
        } catch (e: UnknownHostException) {
            FetchResult.Failure.NoConnection
        } catch (e: SSLException) {
            FetchResult.Failure.NetworkError("SSL error: ${e.message ?: "unknown"}")
        } catch (e: IOException) {
            FetchResult.Failure.NetworkError(e.message ?: "Connection failed")
        }
    }
}
