package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.feature.uiux.ui.components.primitives.NanoCard

@Composable
internal fun ApiProvidersCard(hasProviders: Boolean, modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Model Providers",
    infoText =
      "Connect remote APIs (OpenAI, Anthropic) or local runtimes (Ollama, LM Studio) to power nanoAI's different modes. Multiple providers can be configured for flexibility and redundancy.${if (!hasProviders) "\n\nNo providers configured yet — use the Add button to link one." else ""}",
    modifier = modifier,
  )
}

@Composable
internal fun ApiProviderCard(
  provider: APIProviderConfig,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  NanoCard(
    title = provider.providerName,
    subtitle = provider.baseUrl,
    supportingText = "Type: ${provider.apiType.name}",
    modifier = modifier,
    trailingContent = {
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
    },
  )
}
