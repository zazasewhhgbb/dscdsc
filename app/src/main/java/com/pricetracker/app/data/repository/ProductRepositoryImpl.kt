package com.pricetracker.app.data.repository

import com.pricetracker.app.data.database.PriceHistoryDao
import com.pricetracker.app.data.database.PriceHistoryEntity
import com.pricetracker.app.data.database.ProductDao
import com.pricetracker.app.data.database.ProductEntity
import com.pricetracker.app.domain.PriceChecker
import com.pricetracker.app.domain.PriceCheckOutcome
import com.pricetracker.app.domain.PriceComparator
import com.pricetracker.app.domain.UrlNormalizer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

/**
 * [ProductRepository] backed by Room + [PriceChecker]. Kept small and dependency-injected
 * (constructor params, no singletons besides Room's own instance holder) so it's easy to test
 * with fakes - see app/src/test.
 */
class ProductRepositoryImpl(
    private val productDao: ProductDao,
    private val priceHistoryDao: PriceHistoryDao,
    private val priceChecker: PriceChecker
) : ProductRepository {

    override fun observeProducts(): Flow<List<ProductEntity>> = productDao.observeActiveProducts()

    override suspend fun getProduct(id: Long): ProductEntity? = productDao.getById(id)

    override suspend fun findExistingByUrl(url: String): ProductEntity? {
        return productDao.findByNormalizedUrl(UrlNormalizer.normalize(url))
    }

    override suspend fun saveNewProduct(
        url: String,
        name: String?,
        imageUrl: String?,
        currentPrice: Double,
        currency: String?,
        targetPrice: Double
    ): SaveProductResult {
        val normalized = UrlNormalizer.normalize(url)
        val existing = productDao.findByNormalizedUrl(normalized)
        if (existing != null) return SaveProductResult.DuplicateUrl

        val now = System.currentTimeMillis()
        val entity = ProductEntity(
            url = url,
            normalizedUrl = normalized,
            name = name,
            imageUrl = imageUrl,
            currentPrice = currentPrice,
            currency = currency,
            targetPrice = targetPrice,
            websiteDomain = UrlNormalizer.extractDomain(url),
            lastChecked = now,
            previousPrice = null,
            priceReached = currentPrice <= targetPrice,
            notificationSent = false,
            active = true,
            createdAt = now,
            updatedAt = now
        )
        val id = productDao.insert(entity)
        priceHistoryDao.insert(PriceHistoryEntity(productId = id, price = currentPrice, currency = currency, timestamp = now))
        return SaveProductResult.Saved(id)
    }

    override suspend fun deleteProduct(product: ProductEntity) {
        productDao.delete(product)
    }

    override suspend fun refreshProduct(product: ProductEntity): CheckOutcome {
        return when (val outcome = priceChecker.checkProduct(product.url)) {
            is PriceCheckOutcome.Success -> {
                val now = System.currentTimeMillis()
                val comparison = PriceComparator.compare(
                    currentPrice = outcome.price,
                    targetPrice = product.targetPrice,
                    previousPrice = product.currentPrice,
                    notificationAlreadySentForThisDip = product.notificationSent && product.priceReached
                )

                // If the price rose back above target, allow future notifications again
                // (project rule 20's "re-arm" behaviour).
                val notificationSent = when {
                    comparison.shouldNotify -> true
                    !comparison.targetReached -> false
                    else -> product.notificationSent
                }

                val updated = product.copy(
                    name = outcome.name ?: product.name,
                    imageUrl = outcome.imageUrl ?: product.imageUrl,
                    currentPrice = outcome.price,
                    currency = outcome.currency ?: product.currency,
                    previousPrice = product.currentPrice,
                    priceReached = comparison.targetReached,
                    notificationSent = notificationSent,
                    lastChecked = now,
                    updatedAt = now,
                    lastErrorMessage = null
                )
                productDao.update(updated)
                priceHistoryDao.insert(
                    PriceHistoryEntity(productId = product.id, price = outcome.price, currency = outcome.currency, timestamp = now)
                )
                CheckOutcome.Updated(updated, comparison.targetReached, comparison.shouldNotify)
            }
            is PriceCheckOutcome.Error -> {
                val now = System.currentTimeMillis()
                val updated = product.copy(lastChecked = now, updatedAt = now, lastErrorMessage = outcome.message)
                productDao.update(updated)
                CheckOutcome.Failed(updated, outcome.message)
            }
        }
    }

    override suspend fun refreshAllProducts(): List<CheckOutcome> {
        val products = productDao.getActiveProducts()
        val results = mutableListOf<CheckOutcome>()

        // Limited concurrency + a small delay between requests so we never send a burst of
        // simultaneous requests to (potentially the same) websites (project rule 35).
        val maxConcurrent = 3
        products.chunked(maxConcurrent).forEach { batch ->
            for (product in batch) {
                results.add(refreshProduct(product))
                delay(750L) // gentle pacing between individual requests
            }
        }
        return results
    }
}
