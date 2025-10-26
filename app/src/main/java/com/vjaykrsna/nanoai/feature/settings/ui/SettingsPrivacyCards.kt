package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Privacy & Security Settings Cards
@Composable
internal fun PrivacyAppLockCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "App Lock",
    description = "Secure nanoAI with biometrics or a passcode and configure auto-lock timers.",
    supportingText = "Security shell will integrate with the existing AppLockManager stub.",
    modifier = modifier,
    infoDialogContent = {
      Text(
        text =
          "Add an extra layer of security to nanoAI by requiring biometric authentication, PIN, or password to unlock the app. Configure automatic lock timers to secure your conversations and settings when the app is inactive.",
        style = MaterialTheme.typography.bodyLarge,
      )
    },
  )
}

@Composable
internal fun PrivacyDataManagementCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Local Data Management",
    description = "Manage cached prompts, transcripts, and scratch data.",
    supportingText = "Inline clear actions will reuse DataStore and Room repositories.",
    modifier = modifier,
    infoDialogContent = {
      Text(
        text =
          "Monitor and control data stored locally on your device. Clear cached content, manage conversation history, and remove temporary data that's no longer needed to free up storage space.",
        style = MaterialTheme.typography.bodyLarge,
      )
    },
  )
}

@Composable
internal fun PrivacyEncryptionCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Encryption Settings",
    description = "Configure key rotation and secure storage policies.",
    supportingText = "Advanced encryption controls for sensitive data protection.",
    modifier = modifier,
    infoDialogContent = {
      Text(
        text =
          "Configure encryption settings for maximum security. Set up automatic key rotation schedules, choose encryption algorithms, and control how sensitive data is stored and protected on your device.",
        style = MaterialTheme.typography.bodyLarge,
      )
    },
  )
}
