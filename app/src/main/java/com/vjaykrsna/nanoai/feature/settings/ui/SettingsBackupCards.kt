package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Backup & Sync Settings Cards
@Composable
internal fun BackupAutomatedCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Automated Backups",
    description = "Schedule recurring backups and configure backup destinations.",
    supportingText = "Will integrate with WorkManager once backup destinations are ready.",
    modifier = modifier,
    infoDialogContent = {
      Text(
        text =
          "Set up automatic backups of your conversations, settings, and custom models. Schedule daily, weekly, or manual backups with options to save to local storage, cloud services, or external drives for maximum data protection.",
        style = MaterialTheme.typography.bodyLarge,
      )
    },
  )
}

@Composable
internal fun BackupCloudSyncCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Cloud Sync",
    description = "Sync settings and data across devices with end-to-end encryption.",
    supportingText = "Privacy-first sync will use encrypted cloud storage.",
    modifier = modifier,
    infoDialogContent = {
      Text(
        text =
          "Synchronize your nanoAI data across multiple devices with bank-grade encryption. All data is encrypted on-device before syncing to the cloud, ensuring your conversations and settings remain private even during transmission.",
        style = MaterialTheme.typography.bodyLarge,
      )
    },
  )
}

@Composable
internal fun BackupDataMigrationCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Data Migration",
    description = "Import from other AI apps and convert between formats.",
    supportingText = "Support for common AI assistant data formats and conversion tools.",
    modifier = modifier,
    infoDialogContent = {
      Text(
        text =
          "Easily migrate your data from other AI assistant applications. Import conversations, settings, and custom configurations with automatic format conversion. Supports popular formats from ChatGPT, Claude, Gemini, and other AI tools.",
        style = MaterialTheme.typography.bodyLarge,
      )
    },
  )
}
