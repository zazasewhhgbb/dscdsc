package com.pricetracker.app.parser

import com.pricetracker.app.data.parser.PriceParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PriceParserTest {

    // PriceParser.parse() returns Double? (it returns null for genuinely ambiguous/unparseable
    // input - see PriceParser's own doc comment). JUnit's assertEquals(double, double, double)
    // takes primitives, so every "should parse successfully" case unwraps with `!!`: if parsing
    // unexpectedly returned null, that throws immediately and fails the test with a clear NPE
    // rather than a confusing type error.

    @Test fun `plain integer with currency code`() {
        assertEquals(999.0, PriceParser.parse("999 NOK")!!, 0.001)
    }

    @Test fun `space as thousands separator`() {
        assertEquals(1499.0, PriceParser.parse("1 499 NOK")!!, 0.001)
    }

    @Test fun `dot as thousands separator (European)`() {
        assertEquals(1499.0, PriceParser.parse("1.499 NOK")!!, 0.001)
    }

    @Test fun `comma as thousands separator (US style)`() {
        assertEquals(1499.0, PriceParser.parse("1,499 USD")!!, 0.001)
    }

    @Test fun `dot as decimal separator`() {
        assertEquals(999.50, PriceParser.parse("999.50 EUR")!!, 0.001)
    }

    @Test fun `comma as decimal separator (European)`() {
        assertEquals(999.50, PriceParser.parse("999,50 NOK")!!, 0.001)
    }

    @Test fun `both separators - European convention dot thousands comma decimal`() {
        assertEquals(1234.56, PriceParser.parse("1.234,56 NOK")!!, 0.001)
    }

    @Test fun `both separators - US convention comma thousands dot decimal`() {
        assertEquals(1234.56, PriceParser.parse("1,234.56 USD")!!, 0.001)
    }

    @Test fun `repeated thousands separators`() {
        assertEquals(1234567.0, PriceParser.parse("1.234.567 NOK")!!, 0.001)
    }

    @Test fun `no separators at all`() {
        assertEquals(4990.0, PriceParser.parse("4990")!!, 0.001)
    }

    @Test fun `nbsp used as thousands separator`() {
        assertEquals(2999.0, PriceParser.parse("2\u00A0999 kr")!!, 0.001)
    }

    @Test fun `blank input returns null`() {
        assertNull(PriceParser.parse(""))
    }

    @Test fun `non numeric text returns null`() {
        assertNull(PriceParser.parse("Contact us"))
    }

    @Test fun `genuinely ambiguous fractional digit count returns null`() {
        // 4+ digits after a single separator is not a real-world price format we can trust.
        assertNull(PriceParser.parse("1.23456"))
    }

    @Test fun `unreasonably large value is rejected`() {
        assertNull(PriceParser.parse("999999999999"))
    }

    @Test fun `Nordic no-oere notation with trailing comma-hyphen`() {
        // e.g. XXL.no: "1799,-" means 1799 kr and 0 øre, not "1799 minus something".
        assertEquals(1799.0, PriceParser.parse("1799,-")!!, 0.001)
    }

    @Test fun `Nordic no-oere notation with dot thousands separator`() {
        assertEquals(1799.0, PriceParser.parse("1.799,-")!!, 0.001)
    }

    @Test fun `Nordic no-oere notation with trailing dot-hyphen`() {
        assertEquals(1799.0, PriceParser.parse("1799.-")!!, 0.001)
    }

    @Test fun `Nordic no-oere notation with currency symbol and spacing`() {
        assertEquals(1799.0, PriceParser.parse("kr 1 799,-")!!, 0.001)
    }

    @Test fun `genuine negative number is still treated as negative, not no-oere`() {
        assertEquals(-50.0, PriceParser.parse("-50")!!, 0.001)
    }
}
