package com.pricetracker.app.data.network

import okhttp3.Request
import java.io.IOException
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

/**
 * Checks a URL against its site's robots.txt before [PageFetcher] is allowed to request it
 * (e.g. Dressmann.com disallows automated access to product pages - we must not fetch them
 * even though nothing else would stop us technically). We only ever evaluate the generic "*"
 * group, since this app identifies as a normal browser User-Agent rather than a named bot, so a
 * site-specific group (e.g. "User-agent: Googlebot") would never apply to us anyway.
 *
 * Fails OPEN on network/parsing trouble: if robots.txt can't be fetched at all (404, timeout,
 * no robots.txt served), the page is treated as allowed - many sites simply don't publish one.
 * Any rule that *is* present and matches is always honored, though; this only fails open on
 * "we couldn't read the policy," never on "the policy said no."
 *
 * Results are cached per host for the lifetime of this instance so repeated checks against the
 * same site don't refetch robots.txt every time.
 */
class RobotsTxtChecker(private val client: okhttp3.OkHttpClient = HttpClientProvider.client) {

    private data class Rules(val disallow: List<String>, val allow: List<String>)

    private val cache = ConcurrentHashMap<String, Rules?>()

    /** Returns true if [url] may be fetched according to the site's robots.txt (or if the
     *  policy couldn't be determined at all - see class doc on failing open). */
    fun isAllowed(url: String): Boolean {
        val uri = try {
            URI(url)
        } catch (e: Exception) {
            return true
        }
        val host = uri.host ?: return true
        val scheme = uri.scheme?.takeIf { it == "http" || it == "https" } ?: "https"
        val path = (uri.rawPath.takeIf { it.isNotEmpty() } ?: "/") +
            (uri.rawQuery?.let { "?$it" } ?: "")

        val rules = cache.getOrPut(host) { fetchRules(scheme, host) } ?: return true

        val bestAllowLength = rules.allow
            .filter { pathMatches(path, it) }
            .maxOfOrNull { it.length } ?: -1
        val bestDisallowLength = rules.disallow
            .filter { pathMatches(path, it) }
            .maxOfOrNull { it.length } ?: -1

        // Longest matching rule wins (standard robots.txt convention); a tie favors Allow.
        return bestDisallowLength <= bestAllowLength
    }

    private fun fetchRules(scheme: String, host: String): Rules? {
        return try {
            val request = Request.Builder()
                .url("$scheme://$host/robots.txt")
                .header("User-Agent", HttpClientProvider.USER_AGENT)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                parse(body)
            }
        } catch (e: IOException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    /** Groups consecutive "User-agent:" lines together (they share the rules that follow), then
     *  returns only the rules for the "*" group - see class doc for why. */
    private fun parse(text: String): Rules {
        data class Group(
            val agents: MutableSet<String> = mutableSetOf(),
            val disallow: MutableList<String> = mutableListOf(),
            val allow: MutableList<String> = mutableListOf()
        )

        val groups = mutableListOf<Group>()
        var current: Group? = null
        var lastLineWasUserAgent = false

        for (rawLine in text.lineSequence()) {
            val line = rawLine.substringBefore('#').trim()
            if (line.isEmpty()) continue
            val colonIndex = line.indexOf(':')
            if (colonIndex == -1) continue
            val key = line.substring(0, colonIndex).trim().lowercase()
            val value = line.substring(colonIndex + 1).trim()

            when (key) {
                "user-agent" -> {
                    if (!lastLineWasUserAgent || current == null) {
                        current = Group()
                        groups.add(current)
                    }
                    current.agents.add(value.lowercase())
                    lastLineWasUserAgent = true
                }
                "disallow" -> {
                    if (value.isNotEmpty()) current?.disallow?.add(value)
                    lastLineWasUserAgent = false
                }
                "allow" -> {
                    if (value.isNotEmpty()) current?.allow?.add(value)
                    lastLineWasUserAgent = false
                }
                else -> lastLineWasUserAgent = false
            }
        }

        val wildcardGroup = groups.firstOrNull { "*" in it.agents }
        return Rules(
            disallow = wildcardGroup?.disallow ?: emptyList(),
            allow = wildcardGroup?.allow ?: emptyList()
        )
    }

    /** robots.txt patterns are prefix matches, except "*" (matches any run of characters) and a
     *  trailing "$" (anchors the match to the end of the path). */
    private fun pathMatches(path: String, pattern: String): Boolean {
        if (pattern.isEmpty()) return false
        val anchoredEnd = pattern.endsWith("$")
        val body = if (anchoredEnd) pattern.dropLast(1) else pattern

        val regex = buildString {
            append('^')
            for (c in body) {
                if (c == '*') append(".*") else append(Regex.escape(c.toString()))
            }
            if (!anchoredEnd) append(".*")
            append('$')
        }
        return Regex(regex).matches(path)
    }
}
