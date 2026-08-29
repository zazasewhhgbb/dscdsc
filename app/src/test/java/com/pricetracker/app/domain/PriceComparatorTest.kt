package com.pricetracker.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PriceComparatorTest {

    @Test fun `price above target is not reached`() {
        val result = PriceComparator.compare(
            currentPrice = 1099.0, targetPrice = 999.0, previousPrice = 1299.0,
            notificationAlreadySentForThisDip = false
        )
        assertFalse(result.targetReached)
        assertFalse(result.shouldNotify)
    }

    @Test fun `price equal to target is reached`() {
        val result = PriceComparator.compare(
            currentPrice = 999.0, targetPrice = 999.0, previousPrice = 1299.0,
            notificationAlreadySentForThisDip = false
        )
        assertTrue(result.targetReached)
        assertTrue(result.shouldNotify)
    }

    @Test fun `price below target is reached`() {
        val result = PriceComparator.compare(
            currentPrice = 949.0, targetPrice = 999.0, previousPrice = 1499.0,
            notificationAlreadySentForThisDip = false
        )
        assertTrue(result.targetReached)
        assertTrue(result.shouldNotify)
    }

    @Test fun `does not re-notify while price stays reached`() {
        val result = PriceComparator.compare(
            currentPrice = 949.0, targetPrice = 999.0, previousPrice = 949.0,
            notificationAlreadySentForThisDip = true
        )
        assertTrue(result.targetReached)
        assertFalse(result.shouldNotify)
    }

    @Test fun `notifies again after price rises then dips a second time`() {
        // Simulates: 1499 -> 949 (notify) -> 1199 (rises, re-armed) -> 899 (notify again)
        val secondDip = PriceComparator.compare(
            currentPrice = 899.0, targetPrice = 999.0, previousPrice = 1199.0,
            notificationAlreadySentForThisDip = false // caller resets this once price rose above target
        )
        assertTrue(secondDip.shouldNotify)
    }

    @Test fun `detects a price drop`() {
        val result = PriceComparator.compare(
            currentPrice = 1099.0, targetPrice = 500.0, previousPrice = 1299.0,
            notificationAlreadySentForThisDip = false
        )
        assertTrue(result.priceDropped)
        assertEquals(200.0, result.dropAmount!!, 0.001)
    }

    @Test fun `no previous price means no drop detected`() {
        val result = PriceComparator.compare(
            currentPrice = 1099.0, targetPrice = 500.0, previousPrice = null,
            notificationAlreadySentForThisDip = false
        )
        assertFalse(result.priceDropped)
    }
}
