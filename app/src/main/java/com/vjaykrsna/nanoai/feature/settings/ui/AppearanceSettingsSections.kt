package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.feature.settings.presentation.SettingsUiUxState
import com.vjaykrsna.nanoai.feature.uiux.ui.components.ThemePreferenceChips
import com.vjaykrsna.nanoai.feature.uiux.ui.components.VisualDensityChips

@Composable
internal fun AppearanceThemeHeader(modifier: Modifier = Modifier) {
  SettingsSection(title = "Theme", modifier = modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(
        text = "Choose how the app looks and feels with theme and contrast options.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
internal fun AppearanceThemeCard(
  uiUxState: SettingsUiUxState,
  onThemeChange: (ThemePreference) -> Unit,
  onHighContrastChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Theme",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }

      ThemePreferenceChips(
        selected = uiUxState.themePreference,
        onSelect = onThemeChange,
        modifier = Modifier.fillMaxWidth(),
        supportedThemes =
          listOf(
            ThemePreference.SYSTEM,
            ThemePreference.LIGHT,
            ThemePreference.DARK,
            ThemePreference.AMOLED,
          ),
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(text = "High contrast", style = MaterialTheme.typography.bodyLarge)
        Switch(checked = uiUxState.highContrastEnabled, onCheckedChange = onHighContrastChange)
      }
    }
  }
}

@Composable
internal fun AppearanceDensityHeader(modifier: Modifier = Modifier) {
  SettingsSection(title = "Layout density", modifier = modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(
        text = "Choose how compact or spacious the app interface feels.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
internal fun AppearanceDensityCard(
  uiUxState: SettingsUiUxState,
  onDensityChange: (VisualDensity) -> Unit,
  modifier: Modifier = Modifier,
) {
  val selectedDensity =
    if (uiUxState.compactModeEnabled) VisualDensity.COMPACT else VisualDensity.DEFAULT
  var showInfoDialog by remember { mutableStateOf(false) }

  Card(
    modifier = modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Layout density",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        IconButton(onClick = { showInfoDialog = true }, modifier = Modifier.align(Alignment.Top)) {
          Icon(
            Icons.Filled.Info,
            contentDescription = "Show density options info",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      VisualDensityChips(
        selected = selectedDensity,
        onSelect = onDensityChange,
        modifier = Modifier.fillMaxWidth(),
        onUnsupportedSelect = null,
      )

      Text(
        text = "Spacious mode coming soon and will enable a more relaxed layout for tablets.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }

  if (showInfoDialog) {
    AlertDialog(
      onDismissRequest = { showInfoDialog = false },
      title = { Text(text = "Layout Density", style = MaterialTheme.typography.headlineSmall) },
      text = {
        Text(
          text =
            "Choose how compact or spacious the app interface feels. Compact mode makes elements tighter while spacious mode (coming soon) will enable a more relaxed layout for tablets.",
          style = MaterialTheme.typography.bodyLarge,
        )
      },
      confirmButton = { TextButton(onClick = { showInfoDialog = false }) { Text("OK") } },
    )
  }
}

@Composable
internal fun AppearanceTypographyHeader(modifier: Modifier = Modifier) {
  SettingsSection(title = "Typography & spacing", modifier = modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(
        text = "Customize font scale and text spacing throughout the app.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
internal fun AppearanceTypographyCard(modifier: Modifier = Modifier) {
  Card(
    modifier = modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Typography & spacing",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }

      Text(
        text = "Adjusting font scale and chat bubble density will land in Phase 2.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      SettingsPlaceholderCard(
        description = "Custom font scale presets and per-surface density controls.",
        supportingText =
          "Specs for typography tokens live in specs/003-UI-UX/plan.md " +
            "and will be wired once shared theming surfaces are ready.",
      )
    }
  }
}

@Composable
internal fun AppearanceAnimationPreferencesCard(modifier: Modifier = Modifier) {
  Card(
    modifier = modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Animation preferences",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }

      Text(
        text =
          "Control motion effects and transition speed for better accessibility and reduced motion.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      SettingsPlaceholderCard(
        description = "Animation speed, transition effects, and reduced motion controls.",
        supportingText = "Planning for domain-specific animation tokens in the UI build system.",
      )
    }
  }
}
