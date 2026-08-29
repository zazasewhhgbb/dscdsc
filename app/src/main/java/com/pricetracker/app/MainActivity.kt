package com.pricetracker.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pricetracker.app.domain.LocalPriceChecker
import com.pricetracker.app.ui.navigation.AppNavigation
import com.pricetracker.app.ui.screens.home.HomeViewModel
import com.pricetracker.app.ui.screens.settings.SettingsViewModel
import com.pricetracker.app.ui.theme.PriceTrackerTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()

        val app = application as PriceTrackerApp

        setContent {
            PriceTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val homeViewModel: HomeViewModel = viewModel(
                        factory = HomeViewModelFactory(app.productRepository)
                    )
                    val settingsViewModel: SettingsViewModel = viewModel(
                        factory = SettingsViewModelFactory(applicationContext, app.preferencesManager)
                    )
                    AppNavigation(homeViewModel = homeViewModel, settingsViewModel = settingsViewModel)
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

private class HomeViewModelFactory(
    private val repository: com.pricetracker.app.data.repository.ProductRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(repository, LocalPriceChecker()) as T
    }
}

private class SettingsViewModelFactory(
    private val context: android.content.Context,
    private val preferencesManager: com.pricetracker.app.utils.PreferencesManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SettingsViewModel(context, preferencesManager) as T
    }
}
