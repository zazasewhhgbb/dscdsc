package com.pricetracker.app.utils

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "price_tracker_settings")

/** Small wrapper around Jetpack DataStore for the two user-facing toggles in Settings. */
class PreferencesManager(private val context: Context) {

    companion object {
        private val AUTO_CHECK_ENABLED = booleanPreferencesKey("auto_check_enabled")
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }

    val autoCheckEnabledFlow: Flow<Boolean> =
        context.dataStore.data.map { it[AUTO_CHECK_ENABLED] ?: true }

    val notificationsEnabledFlow: Flow<Boolean> =
        context.dataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: true }

    suspend fun isAutoCheckEnabled(): Boolean = autoCheckEnabledFlow.first()
    suspend fun areNotificationsEnabled(): Boolean = notificationsEnabledFlow.first()

    suspend fun setAutoCheckEnabled(enabled: Boolean) {
        context.dataStore.edit { it[AUTO_CHECK_ENABLED] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }
}
