package com.pricetracker.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrencyUtilsTest {

    @Test fun `detects explicit NOK code`() {
        assertEquals("NOK", CurrencyUtils.detectCurrency("1499 NOK"))
    }

    @Test fun `detects explicit USD code`() {
        assertEquals("USD", CurrencyUtils.detectCurrency("199.99 USD"))
    }

    @Test fun `detects euro symbol`() {
        assertEquals("EUR", CurrencyUtils.detectCurrency("€199.99"))
    }

    @Test fun `detects dollar symbol`() {
        assertEquals("USD", CurrencyUtils.detectCurrency("\$199.99"))
    }

    @Test fun `ambiguous kr symbol without a code is not resolved`() {
        assertNull(CurrencyUtils.detectCurrency("349,90 kr"))
    }

    @Test fun `isKnownCode is case insensitive`() {
        assert(CurrencyUtils.isKnownCode("nok"))
        assert(CurrencyUtils.isKnownCode("NOK"))
        assert(!CurrencyUtils.isKnownCode("XYZ"))
    }
}
