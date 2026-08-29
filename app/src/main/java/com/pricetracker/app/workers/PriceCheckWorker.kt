package com.pricetracker.app.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.pricetracker.app.PriceTrackerApp
import com.pricetracker.app.data.repository.CheckOutcome
import com.pricetracker.app.notifications.NotificationHelper
import com.pricetracker.app.utils.PreferencesManager

/**
 * Runs one scheduled price-check pass over every tracked product, then reschedules itself for
 * the next occurrence of its own time slot (see [WorkScheduler] for why this self-rescheduling
 * approach is used instead of a single periodic request).
 */
class PriceCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val hour = inputData.getInt(KEY_HOUR, -1)

        val prefs = PreferencesManager(applicationContext)
        val autoCheckEnabled = prefs.isAutoCheckEnabled()

        if (!autoCheckEnabled) {
            if (hour >= 0) WorkScheduler.scheduleNextRun(applicationContext, hour)
            return Result.success()
        }

        val app = applicationContext as PriceTrackerApp
        val repository = app.productRepository
        NotificationHelper.createChannelIfNeeded(applicationContext)

        return try {
            val outcomes = repository.refreshAllProducts()
            val notificationsEnabled = prefs.areNotificationsEnabled()
            if (notificationsEnabled) {
                for (outcome in outcomes) {
                    if (outcome is CheckOutcome.Updated && outcome.shouldNotify) {
                        NotificationHelper.sendPriceAlert(applicationContext, outcome.product)
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        } finally {
            // Always reschedule the next window for this slot, success or failure, so a single
            // bad run doesn't silently disable future checks.
            if (hour >= 0) WorkScheduler.scheduleNextRun(applicationContext, hour)
        }
    }

    companion object {
        private const val KEY_HOUR = "hour"
        fun inputDataFor(hour: Int) = workDataOf(KEY_HOUR to hour)
    }
}
