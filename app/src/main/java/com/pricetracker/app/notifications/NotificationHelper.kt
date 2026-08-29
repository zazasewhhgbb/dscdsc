package com.pricetracker.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pricetracker.app.R
import com.pricetracker.app.data.database.ProductEntity

/**
 * Wraps notification-channel creation and the single "price alert" notification the app sends.
 * One notification per product (notification id = product id) so a repeat alert for the same
 * product updates/replaces the previous one instead of stacking duplicates.
 */
object NotificationHelper {

    private const val CHANNEL_ID = "price_alerts"

    fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    fun sendPriceAlert(context: Context, product: ProductEntity) {
        val price = product.currentPrice ?: return
        val currency = product.currency.orEmpty()

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(product.url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            product.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = context.getString(
            R.string.notification_body,
            product.name ?: context.getString(R.string.unknown_product),
            "$price $currency".trim(),
            "${product.targetPrice} $currency".trim()
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(product.name ?: context.getString(R.string.notification_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // POST_NOTIFICATIONS is a runtime permission on Android 13+. If it hasn't been granted,
        // notify() can throw a SecurityException - we catch it rather than crash a background
        // worker over a missing permission; the user simply won't see the alert until granted.
        try {
            NotificationManagerCompat.from(context).notify(product.id.toInt(), notification)
        } catch (e: SecurityException) {
            // Permission not granted - nothing more we can do here.
        }
    }
}
