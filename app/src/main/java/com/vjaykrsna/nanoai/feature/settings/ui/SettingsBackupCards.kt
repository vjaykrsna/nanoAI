package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Backup & Sync Settings Cards
@Composable
internal fun BackupAutomatedCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Automated Backups",
    infoText =
      "Set up automatic backups of your conversations, settings, and custom models. Schedule daily, weekly, or manual backups with options to save to local storage, cloud services, or external drives for maximum data protection.\n\nWill integrate with WorkManager once backup destinations are ready.",
    modifier = modifier,
  )
}

@Composable
internal fun BackupCloudSyncCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Cloud Sync",
    infoText =
      "Synchronize your nanoAI data across multiple devices with bank-grade encryption. All data is encrypted on-device before syncing to the cloud, ensuring your conversations and settings remain private even during transmission.\n\nPrivacy-first sync will use encrypted cloud storage.",
    modifier = modifier,
  )
}

@Composable
internal fun BackupDataMigrationCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Data Migration",
    infoText =
      "Easily migrate your data from other AI assistant applications. Import conversations, settings, and custom configurations with automatic format conversion. Supports popular formats from ChatGPT, Claude, Gemini, and other AI tools.\n\nSupport for common AI assistant data formats and conversion tools.",
    modifier = modifier,
  )
}
