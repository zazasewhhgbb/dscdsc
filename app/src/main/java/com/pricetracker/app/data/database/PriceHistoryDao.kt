package com.pricetracker.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PriceHistoryDao {

    @Insert
    suspend fun insert(entry: PriceHistoryEntity): Long

    @Query("SELECT * FROM price_history WHERE productId = :productId ORDER BY timestamp DESC")
    suspend fun getHistoryForProduct(productId: Long): List<PriceHistoryEntity>

    @Query("SELECT MIN(price) FROM price_history WHERE productId = :productId")
    suspend fun getLowestPrice(productId: Long): Double?

    @Query("SELECT MAX(price) FROM price_history WHERE productId = :productId")
    suspend fun getHighestPrice(productId: Long): Double?
}
