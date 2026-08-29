package com.pricetracker.app.data.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * A single shared OkHttp client. A realistic desktop-browser User-Agent is used because many
 * storefronts return a stripped-down (or blocked) response to obvious bot user agents; this is
 * NOT an attempt to bypass bot protection (see README "Website Terms & Limitations") - if a
 * site still blocks us (403/429/challenge page), we fail gracefully and tell the user, we do
 * not try to work around it.
 */
object HttpClientProvider {

    const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }
}
