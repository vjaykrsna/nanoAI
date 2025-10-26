package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoSpacing
import com.vjaykrsna.nanoai.feature.uiux.ui.components.primitives.NanoCard

/**
 * Unified settings card component that provides consistent styling and behavior. Built on top of
 * NanoCard to ensure design system compliance.
 *
 * Clean card design with minimal text - detailed information available via info dialog.
 *
 * @param title The main title displayed prominently on the card
 * @param modifier Modifier to be applied to the card
 * @param onClick Optional click handler for interactive cards
 * @param showInfoButton Whether to show the info button (recommended for all cards)
 * @param infoTitle Title for the info dialog (defaults to card title)
 * @param infoContent Content composable for the info dialog with detailed explanations
 * @param content Custom content composable to place inside the card body
 */
@Composable
internal fun SettingsCard(
  title: String,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  showInfoButton: Boolean = true,
  infoTitle: String = title,
  infoContent: @Composable () -> Unit,
  content: (@Composable () -> Unit)? = null,
) {
  var showInfoDialog by remember { mutableStateOf(false) }

  NanoCard(
    title = title,
    onClick = onClick,
    modifier = modifier,
    trailingContent =
      if (showInfoButton) {
        {
          IconButton(
            onClick = { showInfoDialog = true },
            modifier = Modifier.semantics { contentDescription = "Show $title info" },
          ) {
            Icon(
              Icons.Filled.Info,
              contentDescription = "Info",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      } else null,
    supportingContent = content,
  )

  if (showInfoDialog) {
    AlertDialog(
      onDismissRequest = { showInfoDialog = false },
      title = { Text(text = infoTitle, style = MaterialTheme.typography.headlineSmall) },
      text = infoContent,
      confirmButton = { TextButton(onClick = { showInfoDialog = false }) { Text("OK") } },
    )
  }
}

/**
 * Convenience composable for settings cards with detailed info dialog. Use for cards that need
 * extensive explanation beyond title.
 */
@Composable
internal fun SettingsInfoCard(
  title: String,
  infoText: String,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  showInfoButton: Boolean = true,
) {
  SettingsCard(
    title = title,
    onClick = onClick,
    showInfoButton = showInfoButton,
    infoTitle = title,
    modifier = modifier,
    infoContent = { Text(text = infoText, style = MaterialTheme.typography.bodyLarge) },
  )
}

/**
 * Settings card designed for interactive elements like toggles, switches, and chips. Uses Material
 * 3 semantic colors for clear visual hierarchy.
 */
@Composable
internal fun SettingsInteractiveCard(
  title: String,
  modifier: Modifier = Modifier,
  showInfoButton: Boolean = true,
  infoContent: @Composable () -> Unit = {},
  content: @Composable () -> Unit,
) {
  SettingsCard(
    title = title,
    showInfoButton = showInfoButton,
    modifier = modifier,
    infoContent = infoContent,
    content = { InteractiveContentContainer { content() } },
  )
}

/**
 * Container for interactive content within settings cards. Uses Material 3 secondary container
 * color for clear visual distinction.
 */
@Composable
private fun InteractiveContentContainer(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  androidx.compose.foundation.layout.Box(
    modifier = modifier.fillMaxWidth().padding(NanoSpacing.md)
  ) {
    content()
  }
}
