package com.pricetracker.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single tracked product.
 *
 * [normalizedUrl] is used for duplicate detection (see UrlNormalizer) so that trivially
 * different URLs (trailing slash, tracking query params, http vs https) are not treated
 * as separate products.
 */
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val url: String,
    val normalizedUrl: String,

    val name: String?,
    val imageUrl: String?,

    val currentPrice: Double?,
    val currency: String?,

    val targetPrice: Double,

    val websiteDomain: String,

    val lastChecked: Long?,
    val previousPrice: Double?,

    /** True once currentPrice <= targetPrice, reset back to false if the price rises again. */
    val priceReached: Boolean = false,

    /** Prevents re-sending a notification every check while the price stays below target. */
    val notificationSent: Boolean = false,

    /** Soft-delete-free "is this still tracked" flag; kept so history isn't orphaned. */
    val active: Boolean = true,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    /** Set when the most recent check failed, so the UI can explain why data looks stale. */
    val lastErrorMessage: String? = null
)
