package com.pricetracker.app.data.parser

sealed class ParseResult {
    data class Success(val product: ParsedProduct) : ParseResult()
    sealed class Failure(val userMessageKey: FailureReason) : ParseResult()
    data class NoPriceFound(val reason: FailureReason = FailureReason.NO_PRICE) : Failure(reason)
    data class AmbiguousPrice(val reason: FailureReason = FailureReason.AMBIGUOUS_PRICE) : Failure(reason)
    data class UnreadablePage(val reason: FailureReason = FailureReason.UNREADABLE) : Failure(reason)
}

enum class FailureReason {
    NO_PRICE,
    AMBIGUOUS_PRICE,
    UNREADABLE
}
