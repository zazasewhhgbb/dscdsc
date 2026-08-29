package com.pricetracker.app

import android.app.Application
import com.pricetracker.app.data.database.AppDatabase
import com.pricetracker.app.data.repository.ProductRepository
import com.pricetracker.app.data.repository.ProductRepositoryImpl
import com.pricetracker.app.domain.LocalPriceChecker
import com.pricetracker.app.notifications.NotificationHelper
import com.pricetracker.app.utils.PreferencesManager
import com.pricetracker.app.workers.WorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Simple manual dependency container (no DI framework - the project is small enough that a
 * framework like Hilt would add more ceremony than value). Everything the app needs is built
 * once here and exposed to Activities/Workers via [PriceTrackerApp].
 */
class PriceTrackerApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var productRepository: ProductRepository
        private set
    lateinit var preferencesManager: PreferencesManager
        private set

    override fun onCreate() {
        super.onCreate()

        val database = AppDatabase.getInstance(this)
        val priceChecker = LocalPriceChecker()
        productRepository = ProductRepositoryImpl(
            productDao = database.productDao(),
            priceHistoryDao = database.priceHistoryDao(),
            priceChecker = priceChecker
        )
        preferencesManager = PreferencesManager(this)

        NotificationHelper.createChannelIfNeeded(this)

        applicationScope.launch {
            if (preferencesManager.isAutoCheckEnabled()) {
                WorkScheduler.scheduleAll(applicationContext)
            }
        }
    }
}
