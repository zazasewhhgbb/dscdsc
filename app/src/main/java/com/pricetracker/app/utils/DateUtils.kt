package com.pricetracker.app.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    /** Formats a timestamp using the device's local timezone and locale (project rule 25/31). */
    fun formatLastChecked(timestampMillis: Long?): String? {
        if (timestampMillis == null) return null
        val formatter = SimpleDateFormat("d MMM yyyy HH:mm", Locale.getDefault())
        return formatter.format(Date(timestampMillis))
    }
}
