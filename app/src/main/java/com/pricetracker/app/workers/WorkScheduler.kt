package com.pricetracker.app.workers

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Schedules the three daily price-check windows (09:00, 19:00, 01:00, device local time -
 * project rule 11/31).
 *
 * IMPORTANT LIMITATION: Android does not guarantee that ordinary background work (WorkManager,
 * AlarmManager without exact-alarm privileges, JobScheduler) runs at an exact wall-clock time.
 * Doze mode, battery optimisation, and OS-level work batching can delay execution by anywhere
 * from a few minutes to a few hours, especially if the device is idle/asleep at the scheduled
 * moment. WorkManager's `setInitialDelay` only guarantees the work will not run BEFORE that
 * delay elapses - it is a scheduled CHECK WINDOW, not a promise of second-accurate execution.
 * We deliberately do not use a persistent foreground service to force exact timing, because
 * that would drain the battery continuously (project rule 11 explicitly forbids this).
 *
 * Each run reschedules itself for the following occurrence of the same time-of-day, which is
 * simpler and more robust across reboots/timezone changes than a single long-lived periodic
 * request tied to app install time.
 */
object WorkScheduler {

    private const val WORK_NAME_PREFIX = "price_check_"
    val CHECK_HOURS = listOf(9, 19, 1) // 09:00, 19:00, 01:00 local time

    fun scheduleAll(context: Context) {
        val workManager = WorkManager.getInstance(context)
        for (hour in CHECK_HOURS) {
            scheduleNextRun(context, hour, workManager)
        }
    }

    fun cancelAll(context: Context) {
        val workManager = WorkManager.getInstance(context)
        for (hour in CHECK_HOURS) {
            workManager.cancelUniqueWork(uniqueWorkName(hour))
        }
    }

    fun scheduleNextRun(context: Context, hour: Int, workManager: WorkManager = WorkManager.getInstance(context)) {
        val delayMillis = millisUntilNext(hour)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<PriceCheckWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES)
            .setInputData(PriceCheckWorker.inputDataFor(hour))
            .build()

        workManager.enqueueUniqueWork(
            uniqueWorkName(hour),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun uniqueWorkName(hour: Int) = "$WORK_NAME_PREFIX$hour"

    /** Milliseconds from now until the next occurrence of [hour]:00 in the device's local
     *  timezone (today if that time hasn't passed yet, otherwise tomorrow). */
    fun millisUntilNext(hour: Int, now: Calendar = Calendar.getInstance()): Long {
        val target = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}
