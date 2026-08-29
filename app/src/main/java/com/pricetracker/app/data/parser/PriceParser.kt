package com.pricetracker.app.data.parser

/**
 * Turns a raw price string (e.g. "1.499 NOK", "999,50", "1 499") into a Double, without
 * guessing when the format is genuinely ambiguous (project rule 15/43: never invent or
 * silently misinterpret a price).
 *
 * Strategy (deliberately locale-light, digit-pattern based, because currency alone doesn't
 * reliably tell you the separator convention a specific website used):
 *
 *  1. Strip everything that isn't a digit, '.', ',' or a minus sign (currency symbols/codes,
 *     letters, non-breaking spaces are removed - whitespace is ALWAYS a thousands separator,
 *     never a decimal separator, in every currency we support, so stripping it is always safe).
 *  2. If both '.' and ',' appear, the one that appears LAST in the string is the decimal
 *     separator (standard convention: "1.234,56" -> European, "1,234.56" -> US/UK).
 *  3. If only one kind of separator appears:
 *       - More than one occurrence -> it's a thousands (grouping) separator, drop it.
 *       - Exactly one occurrence -> look at how many digits follow it:
 *           * 3 digits after  -> thousands separator (e.g. "1.499" / "1,499" -> 1499)
 *           * 1 or 2 digits after -> decimal separator (e.g. "999.50" / "999,50" -> 999.5)
 *           * anything else (0 digits, or 4+) -> genuinely ambiguous, refuse to guess.
 *  4. If neither separator appears, the digits are parsed as-is.
 *  5. Nordic "kr, no øre" notation - a trailing ",-" or ".-" (e.g. "1799,-", "1.499,-", common
 *     on XXL.no and other Nordic retailers) means "and zero øre", not a minus sign. This is
 *     checked before anything else so the general minus-sign handling below never sees it.
 */
object PriceParser {

    private val ALLOWED_CHARS = Regex("[^0-9.,\\-]")
    private val NO_OERE_SUFFIX = Regex("^(\\d[\\d.,]*)[,.]-$")

    fun parse(rawText: String): Double? {
        val whitespaceStripped = rawText.replace(Regex("[\\s\\u00A0]"), "")
        val cleaned = ALLOWED_CHARS.replace(whitespaceStripped, "")
        if (cleaned.isBlank()) return null

        NO_OERE_SUFFIX.find(cleaned)?.let { match ->
            val normalized = normalizeSeparators(match.groupValues[1]) ?: return null
            val value = normalized.toDoubleOrNull() ?: return null
            return if (value.isNaN() || value.isInfinite() || value < 0.0 || value > 100_000_000.0) null else value
        }

        val negative = cleaned.startsWith("-")
        val digitsAndSeparators = cleaned.removePrefix("-")
        if (digitsAndSeparators.isEmpty()) return null

        val normalized = normalizeSeparators(digitsAndSeparators) ?: return null
        val value = normalized.toDoubleOrNull() ?: return null
        if (value.isNaN() || value.isInfinite() || value < 0.0 || value > 100_000_000.0) return null

        return if (negative) -value else value
    }

    private fun normalizeSeparators(text: String): String? {
        val dotCount = text.count { it == '.' }
        val commaCount = text.count { it == ',' }

        return when {
            dotCount == 0 && commaCount == 0 -> {
                if (text.all { it.isDigit() }) text else null
            }

            dotCount > 0 && commaCount > 0 -> {
                val lastDot = text.lastIndexOf('.')
                val lastComma = text.lastIndexOf(',')
                if (lastDot > lastComma) {
                    // Dot is the decimal separator; commas are grouping.
                    val intPart = text.substring(0, lastDot).replace(",", "")
                    val fracPart = text.substring(lastDot + 1)
                    buildDecimal(intPart, fracPart)
                } else {
                    // Comma is the decimal separator; dots are grouping.
                    val intPart = text.substring(0, lastComma).replace(".", "")
                    val fracPart = text.substring(lastComma + 1)
                    buildDecimal(intPart, fracPart)
                }
            }

            commaCount > 0 -> resolveSingleSeparator(text, ',')
            else -> resolveSingleSeparator(text, '.')
        }
    }

    private fun resolveSingleSeparator(text: String, separator: Char): String? {
        val occurrences = text.count { it == separator }
        if (occurrences > 1) {
            // Repeated separator can only be a thousands grouping, e.g. "1.234.567".
            return if (text.replace(separator.toString(), "").all { it.isDigit() }) {
                text.replace(separator.toString(), "")
            } else null
        }

        val index = text.indexOf(separator)
        val intPart = text.substring(0, index)
        val fracPart = text.substring(index + 1)
        if (!intPart.all { it.isDigit() } || !fracPart.all { it.isDigit() }) return null

        return when (fracPart.length) {
            3 -> intPart + fracPart // thousands grouping, e.g. "1.499" -> "1499"
            1, 2 -> buildDecimal(intPart, fracPart) // decimal separator
            else -> null // ambiguous: e.g. "1.4" is fine (handled above), "1.49999" is not typical currency
        }
    }

    private fun buildDecimal(intPart: String, fracPart: String): String? {
        val safeIntPart = intPart.ifBlank { "0" }
        if (!safeIntPart.all { it.isDigit() } || !fracPart.all { it.isDigit() }) return null
        return "$safeIntPart.$fracPart"
    }
}
