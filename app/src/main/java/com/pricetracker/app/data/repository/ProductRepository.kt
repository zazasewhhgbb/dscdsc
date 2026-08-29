package com.pricetracker.app.data.repository

import com.pricetracker.app.data.database.ProductEntity
import kotlinx.coroutines.flow.Flow

/** Outcome of trying to save a new product. */
sealed class SaveProductResult {
    data class Saved(val id: Long) : SaveProductResult()
    data object DuplicateUrl : SaveProductResult()
}

/** Outcome of a single price-check pass over one product. */
sealed class CheckOutcome {
    data class Updated(val product: ProductEntity, val targetReached: Boolean, val shouldNotify: Boolean) : CheckOutcome()
    data class Failed(val product: ProductEntity, val errorMessage: String) : CheckOutcome()
}

interface ProductRepository {
    fun observeProducts(): Flow<List<ProductEntity>>
    suspend fun getProduct(id: Long): ProductEntity?
    suspend fun findExistingByUrl(url: String): ProductEntity?

    suspend fun saveNewProduct(
        url: String,
        name: String?,
        imageUrl: String?,
        currentPrice: Double,
        currency: String?,
        targetPrice: Double
    ): SaveProductResult

    suspend fun deleteProduct(product: ProductEntity)

    /** Re-checks a single product's price and persists the result (used by manual refresh
     *  and by the scheduled worker). */
    suspend fun refreshProduct(product: ProductEntity): CheckOutcome

    /** Re-checks every active product, with limited concurrency and small delays between
     *  requests so we never hammer a website (project rule 35). */
    suspend fun refreshAllProducts(): List<CheckOutcome>
}
