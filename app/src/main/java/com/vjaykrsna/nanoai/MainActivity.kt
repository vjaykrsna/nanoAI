package com.vjaykrsna.nanoai

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.metrics.performance.JankStats
import com.vjaykrsna.nanoai.feature.uiux.presentation.AppViewModel
import com.vjaykrsna.nanoai.feature.uiux.presentation.ModeId
import com.vjaykrsna.nanoai.feature.uiux.presentation.RightPanel
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellViewModel
import com.vjaykrsna.nanoai.feature.uiux.presentation.navigation.NavigationScaffold
import com.vjaykrsna.nanoai.feature.uiux.ui.shell.ShellUiEvent
import com.vjaykrsna.nanoai.shared.ui.theme.NanoAITheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main entry point for the nanoAI application.
 *
 * This activity hosts the entire Compose UI and sets up the navigation scaffold.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  private var jankStats: JankStats? = null
  private val shellViewModel: ShellViewModel by viewModels()
  private lateinit var backPressedCallback: OnBackPressedCallback

  private val notificationPermissionLauncher: ActivityResultLauncher<String> =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      Log.d("MainActivity", "Notification permission ${if (granted) "granted" else "denied"}")
    }

  @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Create notification channels
    createNotificationChannels()

    // Request notification permission on Android 13+
    requestNotificationPermissionIfNeeded()

    backPressedCallback =
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          val layout = shellViewModel.uiState.value.layout
          when {
            layout.isRightDrawerOpen -> {
              val panel = layout.activeRightPanel ?: RightPanel.MODEL_SELECTOR
              shellViewModel.onEvent(ShellUiEvent.ToggleRightDrawer(panel))
            }
            layout.isLeftDrawerOpen -> shellViewModel.onEvent(ShellUiEvent.ToggleLeftDrawer)
            layout.activeMode != ModeId.HOME ->
              shellViewModel.onEvent(ShellUiEvent.ModeSelected(ModeId.HOME))
            else -> {
              isEnabled = false
              onBackPressedDispatcher.onBackPressed()
              isEnabled = true
            }
          }
        }
      }
    onBackPressedDispatcher.addCallback(this, backPressedCallback)
    jankStats =
      JankStats.createAndTrack(window) { frameData ->
        if (frameData.isJank) {
          val durationMs = frameData.frameDurationUiNanos / NANOS_PER_MILLISECOND
          Log.w(JANK_TAG, "Jank frame detected: duration=${"%.2f".format(durationMs)}ms")
        }
      }
    jankStats?.isTrackingEnabled = false
    setContent {
      val windowSizeClass = calculateWindowSizeClass(activity = this@MainActivity)
      val appViewModel: AppViewModel = hiltViewModel()
      val appUiState by appViewModel.uiState.collectAsStateWithLifecycle()

      NanoAITheme(
        themePreference = appUiState.themePreference,
        highContrastEnabled = appUiState.highContrastEnabled,
      ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          if (appUiState.disclaimer.shouldShow) {
            NavigationScaffold(
              appState = appUiState,
              windowSizeClass = windowSizeClass,
              shellViewModel = shellViewModel,
              onDisclaimerShow = appViewModel::onDisclaimerDisplayed,
              onDisclaimerAccept = appViewModel::onDisclaimerAccepted,
            )
          } else if (appUiState.isHydrating) {
            AppHydrationState(isOffline = appUiState.offline)
          } else {
            NavigationScaffold(
              appState = appUiState,
              windowSizeClass = windowSizeClass,
              shellViewModel = shellViewModel,
              onDisclaimerShow = appViewModel::onDisclaimerDisplayed,
              onDisclaimerAccept = appViewModel::onDisclaimerAccepted,
            )
          }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    jankStats?.isTrackingEnabled = true
  }

  override fun onPause() {
    jankStats?.isTrackingEnabled = false
    super.onPause()
  }

  override fun onDestroy() {
    if (::backPressedCallback.isInitialized) {
      backPressedCallback.remove()
    }
    jankStats?.isTrackingEnabled = false
    jankStats = null
    super.onDestroy()
  }

  private fun createNotificationChannels() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
        NotificationChannel(
            WORK_MANAGER_NOTIFICATION_CHANNEL_ID,
            WORK_MANAGER_NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
          )
          .apply { description = "Notifications for background tasks like model downloads" }

      val notificationManager = getSystemService(NotificationManager::class.java)
      notificationManager.createNotificationChannel(channel)
    }
  }

  private fun requestNotificationPermissionIfNeeded() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      when (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)) {
        PackageManager.PERMISSION_GRANTED -> {
          // Permission already granted
          Log.d("MainActivity", "Notification permission already granted")
        }
        else -> {
          // Request permission using Activity Result API
          notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
      }
    }
  }

  private companion object {
    const val JANK_TAG = "NanoAI-Jank"
    const val NANOS_PER_MILLISECOND = 1_000_000f
    const val WORK_MANAGER_NOTIFICATION_CHANNEL_ID = "work_manager_channel"
    const val WORK_MANAGER_NOTIFICATION_CHANNEL_NAME = "Background Tasks"
  }
}

@Composable
private fun AppHydrationState(isOffline: Boolean, modifier: Modifier = Modifier) {
  Box(
    modifier = modifier.fillMaxSize().testTag("app_hydration_state"),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      CircularProgressIndicator()
      Spacer(modifier = Modifier.height(16.dp))
      val message =
        if (isOffline) "Reconnecting to cached workspace…" else "Loading your workspace…"
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
