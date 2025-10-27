package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.feature.settings.presentation.SettingsUiUxState
import com.vjaykrsna.nanoai.feature.uiux.ui.components.ThemePreferenceChips
import com.vjaykrsna.nanoai.feature.uiux.ui.components.VisualDensityChips

@Composable
internal fun AppearanceThemeCard(
  uiUxState: SettingsUiUxState,
  onThemeChange: (ThemePreference) -> Unit,
  onHighContrastChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  SettingsInteractiveCard(
    title = "Theme & High Contrast",
    modifier = modifier,
    infoContent = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text =
            "Theme options control the overall appearance and color scheme of the app interface.",
          style = MaterialTheme.typography.bodyMedium,
        )
        Text(
          text = "Available themes:",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            "• System: Matches your device's theme setting",
            style = MaterialTheme.typography.bodySmall,
          )
          Text(
            "• Light: Clean, bright interface for well-lit environments",
            style = MaterialTheme.typography.bodySmall,
          )
          Text(
            "• Dark: Easy on the eyes in low-light conditions",
            style = MaterialTheme.typography.bodySmall,
          )
          Text(
            "• AMOLED: Pure black background for OLED screens",
            style = MaterialTheme.typography.bodySmall,
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text =
            "High contrast increases color differences for better accessibility. Use this if you have visual impairments or are in bright sunlight.",
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    },
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
internal fun AppearanceDensityCard(
  uiUxState: SettingsUiUxState,
  onDensityChange: (VisualDensity) -> Unit,
  modifier: Modifier = Modifier,
) {
  val selectedDensity =
    if (uiUxState.compactModeEnabled) VisualDensity.COMPACT else VisualDensity.DEFAULT

  SettingsInteractiveCard(
    title = "Layout Density",
    modifier = modifier,
    infoContent = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Layout density controls how much space UI elements take up on screen.",
          style = MaterialTheme.typography.bodyMedium,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Default:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text =
              "Standard spacing between elements. Good balance of information density and readability.",
            style = MaterialTheme.typography.bodySmall,
          )
          Text(
            text = "Compact:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text =
              "Tighter spacing for more content on screen. Better for small screens or users who want to see more information at once.",
            style = MaterialTheme.typography.bodySmall,
          )
        }
        Text(
          text = "Spacious mode coming in future updates for tablet users.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
  ) {
    VisualDensityChips(
      selected = selectedDensity,
      onSelect = onDensityChange,
      modifier = Modifier.fillMaxWidth(),
      onUnsupportedSelect = null,
    )
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
  SettingsInfoCard(
    title = "Typography & Spacing",
    infoText =
      "Custom font scaling and text spacing controls will be available in a future update. Currently uses system font settings and standard Material 3 typography.",
    modifier = modifier,
  )
}

@Composable
internal fun AppearanceAnimationPreferencesCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Animation Preferences",
    infoText =
      "Animation speed controls and reduced motion settings will be available in a future update. Currently uses standard Material 3 motion guidelines for optimal user experience.",
    modifier = modifier,
  )
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
