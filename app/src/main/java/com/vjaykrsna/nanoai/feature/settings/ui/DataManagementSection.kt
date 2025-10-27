package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoSpacing

@Composable
internal fun DataManagementSection(
  onImportBackupClick: () -> Unit,
  onExportBackupClick: () -> Unit,
) {
  BackupRestoreCard(
    onImportBackupClick = onImportBackupClick,
    onExportBackupClick = onExportBackupClick,
  )
}

@Composable
private fun BackupRestoreCard(
  onImportBackupClick: () -> Unit,
  onExportBackupClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  SettingsInteractiveCard(title = "Backup & Restore", modifier = modifier, showInfoButton = false) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      DataManagementItemCard(
        title = "Import Backup",
        description = "Restore personas, providers, and settings from a backup file",
        icon = Icons.Default.Add,
        iconContentDescription = "Import",
        onClick = onImportBackupClick,
      )

      DataManagementItemCard(
        title = "Export Backup",
        description = "Export conversations, personas, and settings",
        icon = Icons.Default.Edit,
        iconContentDescription = "Export",
        onClick = onExportBackupClick,
      )
    }
  }
}

@Composable
private fun DataManagementItemCard(
  title: String,
  description: String,
  icon: ImageVector,
  iconContentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth().clickable { onClick() }.padding(NanoSpacing.md),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
      )
    }
    Icon(icon, iconContentDescription, tint = MaterialTheme.colorScheme.onSurface)
  }
}
