package com.pricetracker.app.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One recorded price point for a product. Not surfaced in the UI yet (see README "Future
 * Improvements" -> price history charts) but every successful check writes a row here so
 * that feature can be added later without a schema migration for the core data.
 */
@Entity(
    tableName = "price_history",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("productId")]
)
data class PriceHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val price: Double,
    val currency: String?,
    val timestamp: Long = System.currentTimeMillis()
)
