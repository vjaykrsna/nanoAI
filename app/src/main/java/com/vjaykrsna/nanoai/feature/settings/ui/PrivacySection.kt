package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreference
import com.vjaykrsna.nanoai.core.data.preferences.RetentionPolicy

@Composable
internal fun PrivacySection(
  privacyPreferences: PrivacyPreference,
  onTelemetryToggle: (Boolean) -> Unit,
  onRetentionPolicyChange: (RetentionPolicy) -> Unit,
) {
  PrivacySettings(
    preferences = privacyPreferences,
    onTelemetryToggle = onTelemetryToggle,
    onRetentionPolicyChange = onRetentionPolicyChange,
  )
}

@Composable
internal fun PrivacySettings(
  preferences: PrivacyPreference?,
  onTelemetryToggle: (Boolean) -> Unit,
  onRetentionPolicyChange: (RetentionPolicy) -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    val telemetryOptIn = preferences?.telemetryOptIn ?: false
    val selectedPolicy = preferences?.retentionPolicy ?: RetentionPolicy.INDEFINITE
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        text = "Privacy & Telemetry",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium,
      )

      TelemetryPreferenceRow(telemetryOptIn = telemetryOptIn, onTelemetryToggle = onTelemetryToggle)

      RetentionPolicySection(
        selectedPolicy = selectedPolicy,
        onRetentionPolicyChange = onRetentionPolicyChange,
      )

      PrivacyNoticeSection()
    }
  }
}

@Composable
private fun TelemetryPreferenceRow(telemetryOptIn: Boolean, onTelemetryToggle: (Boolean) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = "Usage Analytics",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium,
      )
      Text(
        text = "Help improve the app by sharing anonymous usage data",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(
      checked = telemetryOptIn,
      onCheckedChange = onTelemetryToggle,
      modifier = Modifier.semantics { contentDescription = "Toggle usage analytics" },
    )
  }
}

@Composable
private fun RetentionPolicySection(
  selectedPolicy: RetentionPolicy,
  onRetentionPolicyChange: (RetentionPolicy) -> Unit,
) {
  Column {
    Text(
      text = "Message Retention",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Medium,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = "Message retention policy: ${selectedPolicy.name}",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
      modifier = Modifier.horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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

@Composable
private fun PrivacyNoticeSection() {
  Column {
    Text(
      text = "Privacy Notice",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Medium,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = "All data is stored locally on your device.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

private fun RetentionPolicy.displayLabel(): String =
  when (this) {
    RetentionPolicy.INDEFINITE -> "Keep indefinitely"
    RetentionPolicy.MANUAL_PURGE_ONLY -> "Manual purge only"
  }
