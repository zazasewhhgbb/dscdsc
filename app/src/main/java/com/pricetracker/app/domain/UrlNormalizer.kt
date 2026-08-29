package com.pricetracker.app.domain

import java.net.URI

/**
 * Normalizes a URL so trivially-different links to the same product (http vs https, trailing
 * slash, common tracking query parameters, "www." prefix) are recognised as duplicates
 * (project rule 23).
 */
object UrlNormalizer {

    private val TRACKING_PARAMS = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "gclid", "fbclid", "ref", "ref_", "igshid", "spm"
    )

    fun isValid(url: String): Boolean {
        return try {
            val uri = URI(url.trim())
            (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrBlank()
        } catch (e: Exception) {
            false
        }
    }

    fun normalize(url: String): String {
        return try {
            val uri = URI(url.trim())
            val host = uri.host?.lowercase()?.removePrefix("www.") ?: return url.trim()
            val path = uri.path?.trimEnd('/') ?: ""
            val query = uri.query
                ?.split("&")
                ?.filter { param ->
                    val key = param.substringBefore("=").lowercase()
                    key !in TRACKING_PARAMS
                }
                ?.sorted()
                ?.joinToString("&")
                .orEmpty()

            buildString {
                append(host)
                append(path)
                if (query.isNotBlank()) {
                    append("?")
                    append(query)
                }
            }
        } catch (e: Exception) {
            url.trim()
        }
    }

    fun extractDomain(url: String): String {
        return try {
            URI(url.trim()).host?.removePrefix("www.") ?: url
        } catch (e: Exception) {
            url
        }
    }
}
