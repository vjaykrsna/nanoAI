package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig

@Composable
internal fun ApiProvidersCard(hasProviders: Boolean, modifier: Modifier = Modifier) {
  var showInfoDialog by remember { mutableStateOf(false) }

  Card(
    modifier = modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "Model Providers",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium,
      )
      IconButton(
        onClick = { showInfoDialog = true },
        modifier = Modifier.semantics { contentDescription = "Show model providers info" },
      ) {
        Icon(
          Icons.Filled.Info,
          contentDescription = "Info",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 0.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "Connect remote APIs or local runtimes to power nanoAI's modes.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (!hasProviders) {
        Text(
          text = "No providers configured yet — use the Add button to link one.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(modifier = Modifier.height(0.dp))
    }
  }

  if (showInfoDialog) {
    AlertDialog(
      onDismissRequest = { showInfoDialog = false },
      title = { Text(text = "Model Providers", style = MaterialTheme.typography.headlineSmall) },
      text = {
        Text(
          text =
            "Connect remote APIs (OpenAI, Anthropic) or local runtimes (Ollama, LM Studio) to power nanoAI's different modes. Multiple providers can be configured for flexibility and redundancy.",
          style = MaterialTheme.typography.bodyLarge,
        )
      },
      confirmButton = { TextButton(onClick = { showInfoDialog = false }) { Text("OK") } },
    )
  }
}

@Composable
internal fun ApiProviderCard(
  provider: APIProviderConfig,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = provider.providerName,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = provider.baseUrl,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Type: ${provider.apiType.name}",
          style = MaterialTheme.typography.labelSmall,
          color =
            if (provider.isEnabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error,
        )
      }

      Row {
        IconButton(
          onClick = onEdit,
          modifier = Modifier.semantics { contentDescription = "Edit ${provider.providerName}" },
        ) {
          Icon(Icons.Default.Edit, contentDescription = "Edit")
        }
        IconButton(
          onClick = onDelete,
          modifier = Modifier.semantics { contentDescription = "Delete ${provider.providerName}" },
        ) {
          Icon(Icons.Default.Delete, contentDescription = "Delete")
        }
      }
    }
  }
}
