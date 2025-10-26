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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
  Card(
    modifier = modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        text = "Backup & Restore",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium,
      )

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
  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Medium,
        )
        Text(
          text = description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Icon(icon, iconContentDescription)
    }
  }
}
