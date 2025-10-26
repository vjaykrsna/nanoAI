package com.vjaykrsna.nanoai.feature.uiux.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity

@Composable
fun ThemePreferenceChips(
  selected: ThemePreference,
  onSelect: (ThemePreference) -> Unit,
  modifier: Modifier = Modifier,
  supportedThemes: List<ThemePreference> = ThemePreference.entries,
  chipModifier: (ThemePreference) -> Modifier = { Modifier },
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier.horizontalScroll(rememberScrollState()),
  ) {
    supportedThemes.forEach { theme ->
      val label = themeLabel(theme)
      FilterChip(
        selected = selected == theme,
        onClick = { if (selected != theme) onSelect(theme) },
        label = { Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = chipModifier(theme).semantics { contentDescription = "Select $label theme" },
      )
    }
  }
}

@Composable
@Suppress("UnusedParameter")
fun VisualDensityChips(
  selected: VisualDensity,
  onSelect: (VisualDensity) -> Unit,
  modifier: Modifier = Modifier,
  supportedDensities: List<VisualDensity> = listOf(VisualDensity.DEFAULT, VisualDensity.COMPACT),
  onUnsupportedSelect: ((VisualDensity) -> Unit)? = null,
  chipModifier: (VisualDensity) -> Modifier = { Modifier },
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier.horizontalScroll(rememberScrollState()),
  ) {
    supportedDensities.forEach { density ->
      val label = densityLabel(density)
      FilterChip(
        selected = selected == density,
        onClick = { if (selected != density) onSelect(density) },
        label = { Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier =
          chipModifier(density).semantics { contentDescription = "Switch to $label layout density" },
      )
    }

    val unsupportedDensities = VisualDensity.values().filterNot { supportedDensities.contains(it) }
    if (unsupportedDensities.isNotEmpty()) {
      Spacer(modifier = Modifier.width(4.dp))
      unsupportedDensities.forEach { density ->
        val label = densityLabel(density)
        FilterChip(
          selected = false,
          onClick = { onUnsupportedSelect?.invoke(density) },
          enabled = onUnsupportedSelect != null,
          label = {
            Text(
              text = label,
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          },
          modifier = Modifier.semantics { contentDescription = "${label} density coming soon" },
        )
      }
    }
  }
}

private fun themeLabel(theme: ThemePreference): String =
  when (theme) {
    ThemePreference.LIGHT -> "Light"
    ThemePreference.DARK -> "Dark"
    ThemePreference.AMOLED -> "AMOLED"
    ThemePreference.SYSTEM -> "System"
  }

private fun densityLabel(density: VisualDensity): String =
  when (density) {
    VisualDensity.DEFAULT -> "Comfortable"
    VisualDensity.COMPACT -> "Compact"
    VisualDensity.EXPANDED -> "Spacious"
  }
