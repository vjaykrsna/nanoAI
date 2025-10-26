package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Privacy & Security Settings Cards
@Composable
internal fun PrivacyAppLockCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "App Lock",
    infoText =
      "Add an extra layer of security to nanoAI by requiring biometric authentication, PIN, or password to unlock the app. Configure automatic lock timers to secure your conversations and settings when the app is inactive.\n\nSecurity shell will integrate with the existing AppLockManager stub.",
    modifier = modifier,
  )
}

@Composable
internal fun PrivacyDataManagementCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Local Data Management",
    infoText =
      "Monitor and control data stored locally on your device. Clear cached content, manage conversation history, and remove temporary data that's no longer needed to free up storage space.\n\nInline clear actions will reuse DataStore and Room repositories.",
    modifier = modifier,
  )
}

@Composable
internal fun PrivacyEncryptionCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Encryption Settings",
    infoText =
      "Configure encryption settings for maximum security. Set up automatic key rotation schedules, choose encryption algorithms, and control how sensitive data is stored and protected on your device.\n\nAdvanced encryption controls for sensitive data protection.",
    modifier = modifier,
  )
}
