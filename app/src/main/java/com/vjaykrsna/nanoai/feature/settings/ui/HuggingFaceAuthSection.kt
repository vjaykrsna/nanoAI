package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.feature.settings.domain.huggingface.HuggingFaceAuthState
import com.vjaykrsna.nanoai.feature.settings.domain.huggingface.HuggingFaceTokenSource
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoSpacing

@Composable
internal fun HuggingFaceAuthSectionHeader(modifier: Modifier = Modifier) {
  SettingsSection(title = "Hugging Face", modifier = modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(
        text = "Connect your Hugging Face account to access models and datasets.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
internal fun HuggingFaceAuthCard(
  state: HuggingFaceAuthState,
  onLoginClick: () -> Unit,
  onApiKeyClick: () -> Unit,
  onDisconnectClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  SettingsCard(
    title = "Hugging Face Account",
    modifier = modifier,
    showInfoButton = true,
    infoContent = {
      Text(
        text =
          "Connect your Hugging Face account to access models and datasets from the Hugging Face Hub.",
        style = MaterialTheme.typography.bodyLarge,
      )
    },
    content = {
      Column(
        modifier = Modifier.fillMaxWidth().padding(NanoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(NanoSpacing.md),
      ) {
        val statusText =
          when {
            state.isVerifying -> "Verifying credential…"
            state.isAuthenticated && state.accountLabel != null ->
              "Connected as ${state.accountLabel}"
            state.isAuthenticated -> "Connected"
            else -> "Not connected"
          }

        val statusColor =
          when {
            state.isAuthenticated -> MaterialTheme.colorScheme.primary
            state.isVerifying -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
          }

        Text(
          text = statusText,
          style = MaterialTheme.typography.bodyMedium,
          color = statusColor,
          fontWeight = FontWeight.Medium,
        )

        val supportingText = supportingStatusLine(state)
        if (supportingText != null) {
          Text(
            text = supportingText,
            style = MaterialTheme.typography.bodySmall,
            color =
              if (state.lastError != null) MaterialTheme.colorScheme.error
              else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(NanoSpacing.sm),
        ) {
          OutlinedButton(
            onClick = onLoginClick,
            modifier =
              Modifier.weight(1f).semantics {
                contentDescription = "Login with Hugging Face account"
              },
          ) {
            Icon(
              Icons.AutoMirrored.Filled.Login,
              contentDescription = null,
              modifier = Modifier.padding(end = 8.dp),
            )
            Text("Login")
          }

          FilledTonalButton(
            onClick = onApiKeyClick,
            modifier =
              Modifier.weight(1f).semantics { contentDescription = "Enter API key manually" },
          ) {
            Icon(
              Icons.Default.Key,
              contentDescription = null,
              modifier = Modifier.padding(end = 8.dp),
            )
            Text("API Key")
          }
        }

        if (state.isAuthenticated) {
          TextButton(
            onClick = onDisconnectClick,
            modifier =
              Modifier.align(Alignment.End).semantics {
                contentDescription = "Disconnect Hugging Face account"
              },
          ) {
            Text("Disconnect")
          }
        }
      }
    },
  )
}

private fun supportingStatusLine(state: HuggingFaceAuthState): String? =
  when {
    state.lastError != null -> state.lastError
    state.isAuthenticated ->
      when (state.tokenSource) {
        HuggingFaceTokenSource.API_TOKEN -> "Authenticated via API token"
        HuggingFaceTokenSource.OAUTH -> "Authenticated via OAuth"
        else -> null
      }
    else -> null
  }
