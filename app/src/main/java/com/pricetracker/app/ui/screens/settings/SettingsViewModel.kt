package com.pricetracker.app.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pricetracker.app.utils.PreferencesManager
import com.pricetracker.app.workers.WorkScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class SettingsUiState(
    val autoCheckEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true
)

class SettingsViewModel(
    private val appContext: Context,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                preferencesManager.autoCheckEnabledFlow,
                preferencesManager.notificationsEnabledFlow
            ) { autoCheck, notifications ->
                SettingsUiState(autoCheck, notifications)
            }.collect { _uiState.value = it }
        }
    }

    fun setAutoCheckEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAutoCheckEnabled(enabled)
            if (enabled) {
                WorkScheduler.scheduleAll(appContext)
            } else {
                WorkScheduler.cancelAll(appContext)
            }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setNotificationsEnabled(enabled) }
    }
}
