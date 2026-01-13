package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.feature.settings.presentation.state.PrivacyDashboardSummary

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

/**
 * Privacy Dashboard Card showing a summary of privacy-related settings. Displays consent status,
 * telemetry preference, and data retention policy.
 */
@Composable
internal fun PrivacyDashboardCard(summary: PrivacyDashboardSummary, modifier: Modifier = Modifier) {
  SettingsInteractiveCard(
    title = "Privacy Dashboard",
    modifier = modifier.semantics { contentDescription = "Privacy settings summary dashboard" },
    showInfoButton = true,
    infoContent = { PrivacyDashboardInfoContent() },
  ) {
    PrivacyDashboardSummaryContent(summary)
  }
}

@Composable
private fun PrivacyDashboardInfoContent() {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = "This dashboard provides an overview of your privacy settings.",
      style = MaterialTheme.typography.bodyMedium,
    )
    Text(
      text =
        "All your data is stored locally on your device. " +
          "nanoAI operates without cloud dependencies by default, " +
          "ensuring your conversations remain private.",
      style = MaterialTheme.typography.bodySmall,
    )
  }
}

@Composable
private fun PrivacyDashboardSummaryContent(summary: PrivacyDashboardSummary) {
  Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    PrivacyStatusRow(
      label = "Privacy Consent",
      isEnabled = summary.isConsentAcknowledged,
      enabledText = "Acknowledged",
      disabledText = "Pending",
    )

    PrivacyStatusRow(
      label = "Usage Analytics",
      isEnabled = summary.isTelemetryEnabled,
      enabledText = "Enabled",
      disabledText = "Disabled",
    )

    PrivacyStatusRow(
      label = "Export Warnings",
      isEnabled = !summary.exportWarningsDismissed,
      enabledText = "Active",
      disabledText = "Dismissed",
    )

    DataRetentionRow(summary)

    if (summary.disclaimerShownCount > 0) {
      DisclaimerCountRow(summary)
    }
  }
}

@Composable
private fun DataRetentionRow(summary: PrivacyDashboardSummary) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = "Data Retention",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
      text =
        summary.retentionPolicy.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.primary,
    )
  }
}

@Composable
private fun DisclaimerCountRow(summary: PrivacyDashboardSummary) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = "Disclaimer Views",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text =
        "${summary.disclaimerShownCount} time${if (summary.disclaimerShownCount == 1) "" else "s"}",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun PrivacyStatusRow(
  label: String,
  isEnabled: Boolean,
  enabledText: String,
  disabledText: String,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Row(
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = if (isEnabled) Icons.Filled.CheckCircle else Icons.Filled.Warning,
        contentDescription = null,
        tint =
          if (isEnabled) {
            MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.outline
          },
      )
      Text(
        text = if (isEnabled) enabledText else disabledText,
        style = MaterialTheme.typography.bodyMedium,
        color =
          if (isEnabled) {
            MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
      )
    }
  }
}
