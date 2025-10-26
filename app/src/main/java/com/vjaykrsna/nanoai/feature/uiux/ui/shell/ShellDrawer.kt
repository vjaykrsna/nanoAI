@file:Suppress("MatchingDeclarationName")

package com.vjaykrsna.nanoai.feature.uiux.ui.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.R
import com.vjaykrsna.nanoai.feature.uiux.presentation.ModeId
import com.vjaykrsna.nanoai.feature.uiux.presentation.PaletteSource
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoElevation

/** Events emitted by shell drawer components. */
sealed interface ShellDrawerEvent {
  data class ModeSelected(val modeId: ModeId) : ShellDrawerEvent

  data class ShowCommandPalette(val source: PaletteSource) : ShellDrawerEvent

  data object CloseDrawer : ShellDrawerEvent
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShellDrawerContent(
  variant: DrawerVariant,
  activeMode: ModeId,
  onEvent: (ShellDrawerEvent) -> Unit,
) {
  when (variant) {
    DrawerVariant.Modal ->
      ModalDrawerSheet { DrawerSheetContent(activeMode = activeMode, onEvent = onEvent) }
    DrawerVariant.Permanent ->
      PermanentDrawerSheet { DrawerSheetContent(activeMode = activeMode, onEvent = onEvent) }
  }
}

@Composable
private fun DrawerSheetContent(activeMode: ModeId, onEvent: (ShellDrawerEvent) -> Unit) {
  Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Search/Command palette at the top
    Surface(
      tonalElevation = NanoElevation.level1,
      shape = RoundedCornerShape(12.dp),
      onClick = {
        onEvent(ShellDrawerEvent.ShowCommandPalette(PaletteSource.TOP_APP_BAR))
        onEvent(ShellDrawerEvent.CloseDrawer)
      },
    ) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("drawer_command_palette"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Outlined.Search, contentDescription = null)
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            stringResource(R.string.nano_shell_search),
            style = MaterialTheme.typography.titleSmall,
          )
          Text(
            "Ctrl+K",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
          )
        }
      }
    }

    // Navigation items
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      // Home button
      DrawerNavigationItem(
        icon = Icons.Outlined.Home,
        title = "Home",
        selected = activeMode == ModeId.HOME,
        onClick = {
          onEvent(ShellDrawerEvent.ModeSelected(ModeId.HOME))
          onEvent(ShellDrawerEvent.CloseDrawer)
        },
      )

      // Settings button
      DrawerNavigationItem(
        icon = Icons.Outlined.Settings,
        title = "Settings",
        selected = activeMode == ModeId.SETTINGS,
        onClick = {
          onEvent(ShellDrawerEvent.ModeSelected(ModeId.SETTINGS))
          onEvent(ShellDrawerEvent.CloseDrawer)
        },
      )

      // Chat History button
      DrawerNavigationItem(
        icon = Icons.Default.History,
        title = "Chat History",
        selected = activeMode == ModeId.HISTORY,
        onClick = {
          onEvent(ShellDrawerEvent.ModeSelected(ModeId.HISTORY))
          onEvent(ShellDrawerEvent.CloseDrawer)
        },
      )

      // Mode cards removed intentionally - only Home, Settings, and Chat History required
    }
  }
}

@Composable
private fun DrawerNavigationItem(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  selected: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    tonalElevation = if (selected) NanoElevation.level3 else NanoElevation.level0,
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.fillMaxWidth().testTag("drawer_nav_${title.lowercase()}"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.weight(1f),
      )
    }
  }
}
