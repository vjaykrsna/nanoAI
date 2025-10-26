package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreference
import com.vjaykrsna.nanoai.core.data.preferences.RetentionPolicy

@Composable
internal fun PrivacySection(
  privacyPreferences: PrivacyPreference,
  onTelemetryToggle: (Boolean) -> Unit,
  onRetentionPolicyChange: (RetentionPolicy) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    PrivacyTelemetryCard(
      telemetryOptIn = privacyPreferences.telemetryOptIn,
      onTelemetryToggle = onTelemetryToggle,
    )

    PrivacyRetentionCard(
      selectedPolicy = privacyPreferences.retentionPolicy,
      onRetentionPolicyChange = onRetentionPolicyChange,
    )

    PrivacyNoticeCard()
  }
}

@Composable
private fun PrivacyTelemetryCard(
  telemetryOptIn: Boolean,
  onTelemetryToggle: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  SettingsInteractiveCard(
    title = "Usage Analytics",
    modifier = modifier,
    infoContent = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Usage analytics help us understand how nanoAI is used so we can improve the app.",
          style = MaterialTheme.typography.bodyMedium,
        )
        Text(
          text = "What's collected:",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("• Screen navigation patterns", style = MaterialTheme.typography.bodySmall)
          Text("• Feature usage frequency", style = MaterialTheme.typography.bodySmall)
          Text("• App performance metrics", style = MaterialTheme.typography.bodySmall)
          Text("• Error and crash reports", style = MaterialTheme.typography.bodySmall)
        }
        Text(
          text = "We never collect:",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("• Personal messages or content", style = MaterialTheme.typography.bodySmall)
          Text("• Model inputs or outputs", style = MaterialTheme.typography.bodySmall)
          Text("• API keys or credentials", style = MaterialTheme.typography.bodySmall)
        }
      }
    },
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(text = "Share anonymous usage data", style = MaterialTheme.typography.bodyLarge)
      }
      Switch(
        checked = telemetryOptIn,
        onCheckedChange = onTelemetryToggle,
        modifier = Modifier.semantics { contentDescription = "Toggle usage analytics" },
      )
    }
  }
}

@Composable
private fun PrivacyRetentionCard(
  selectedPolicy: RetentionPolicy,
  onRetentionPolicyChange: (RetentionPolicy) -> Unit,
  modifier: Modifier = Modifier,
) {
  SettingsInteractiveCard(
    title = "Message Retention",
    modifier = modifier,
    infoContent = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Choose how long your conversation data is kept locally on your device.",
          style = MaterialTheme.typography.bodyMedium,
        )
        Text(
          text = "Retention policies:",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(text = "Keep indefinitely:", style = MaterialTheme.typography.labelSmall)
          Text(
            text =
              "All messages are stored until manually deleted. Best for keeping full conversation history.",
            style = MaterialTheme.typography.bodySmall,
          )
          Text(text = "Manual purge only:", style = MaterialTheme.typography.labelSmall)
          Text(
            text =
              "Messages are kept until you manually clear them. Recommended for privacy-conscious users.",
            style = MaterialTheme.typography.bodySmall,
          )
        }
      }
    },
  ) {
    Column {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RetentionPolicy.values().forEach { policy ->
          FilterChip(
            selected = selectedPolicy == policy,
            onClick = { onRetentionPolicyChange(policy) },
            label = { Text(policy.displayLabel()) },
          )
        }
      }
    }
  }
}

@Composable
private fun PrivacyNoticeCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Local Data Storage",
    infoText =
      "All your data is stored locally on your device. nanoAI operates without cloud dependencies, ensuring your conversations and settings remain private and accessible even offline.",
    modifier = modifier,
  )
}

private fun RetentionPolicy.displayLabel(): String =
  when (this) {
    RetentionPolicy.INDEFINITE -> "Keep indefinitely"
    RetentionPolicy.MANUAL_PURGE_ONLY -> "Manual purge only"
  }
