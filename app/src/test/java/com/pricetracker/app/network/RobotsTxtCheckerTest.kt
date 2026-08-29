package com.pricetracker.app.network

import com.pricetracker.app.data.network.RobotsTxtChecker
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RobotsTxtCheckerTest {

    private lateinit var server: MockWebServer
    private lateinit var checker: RobotsTxtChecker

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        checker = RobotsTxtChecker(OkHttpClient())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun urlFor(path: String) = server.url(path).toString()

    @Test fun `disallowed path under a Disallow rule is blocked`() {
        server.enqueue(MockResponse().setBody("User-agent: *\nDisallow: /products/\n"))
        server.enqueue(MockResponse().setResponseCode(404)) // never reached if blocked correctly

        assertFalse(checker.isAllowed(urlFor("/products/some-item")))
    }

    @Test fun `path outside any Disallow rule is allowed`() {
        server.enqueue(MockResponse().setBody("User-agent: *\nDisallow: /admin/\n"))

        assertTrue(checker.isAllowed(urlFor("/products/some-item")))
    }

    @Test fun `no robots-txt at all fails open (allowed)`() {
        server.enqueue(MockResponse().setResponseCode(404))

        assertTrue(checker.isAllowed(urlFor("/products/some-item")))
    }

    @Test fun `Allow rule overrides a shorter Disallow rule`() {
        server.enqueue(
            MockResponse().setBody(
                "User-agent: *\nDisallow: /products/\nAllow: /products/public/\n"
            )
        )

        assertTrue(checker.isAllowed(urlFor("/products/public/some-item")))
    }

    @Test fun `rules under a different named user-agent do not apply to us`() {
        server.enqueue(
            MockResponse().setBody(
                "User-agent: Googlebot\nDisallow: /products/\n\nUser-agent: *\nDisallow:\n"
            )
        )

        assertTrue(checker.isAllowed(urlFor("/products/some-item")))
    }

    @Test fun `wildcard pattern in Disallow is honored`() {
        server.enqueue(MockResponse().setBody("User-agent: *\nDisallow: /*/checkout\n"))

        assertFalse(checker.isAllowed(urlFor("/en/checkout")))
    }

    @Test fun `blank Disallow means everything is allowed`() {
        server.enqueue(MockResponse().setBody("User-agent: *\nDisallow:\n"))

        assertTrue(checker.isAllowed(urlFor("/products/some-item")))
    }
}
