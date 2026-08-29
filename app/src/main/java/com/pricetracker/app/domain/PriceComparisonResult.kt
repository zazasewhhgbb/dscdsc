package com.pricetracker.app.domain

/** Result of comparing a freshly checked price against a product's target and previous price. */
data class PriceComparisonResult(
    val targetReached: Boolean,
    val priceDropped: Boolean,
    val dropAmount: Double?,
    /** True only when the target has JUST been reached this check (i.e. we should notify) -
     *  see project rule 20: don't repeat the same alert every check while price stays low. */
    val shouldNotify: Boolean
)

object PriceComparator {

    fun compare(
        currentPrice: Double,
        targetPrice: Double,
        previousPrice: Double?,
        notificationAlreadySentForThisDip: Boolean
    ): PriceComparisonResult {
        val targetReached = currentPrice <= targetPrice
        val priceDropped = previousPrice != null && currentPrice < previousPrice
        val dropAmount = if (priceDropped) previousPrice!! - currentPrice else null

        // Only notify the first time we cross into "reached" territory. If it was already
        // reached last check and we already sent a notification, stay quiet (rule 20) - but if
        // the price rose back above target and dips again later, notificationAlreadySentForThisDip
        // will have been reset to false by the caller, so we notify again.
        val shouldNotify = targetReached && !notificationAlreadySentForThisDip

        return PriceComparisonResult(
            targetReached = targetReached,
            priceDropped = priceDropped,
            dropAmount = dropAmount,
            shouldNotify = shouldNotify
        )
    }
}
