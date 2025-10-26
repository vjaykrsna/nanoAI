package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoSpacing

/**
 * Placeholder card for features that are planned but not yet implemented. This component provides
 * consistent placeholder styling.
 */
@Composable
internal fun SettingsPlaceholderCard(
  description: String,
  supportingText: String? = null,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth().padding(NanoSpacing.md),
    verticalArrangement = Arrangement.spacedBy(NanoSpacing.sm),
  ) {
    Text(
      text = description,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    supportingText?.let {
      Text(
        text = it,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
      )
    }
  }
}
