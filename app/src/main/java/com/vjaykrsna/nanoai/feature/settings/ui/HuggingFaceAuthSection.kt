package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
  Card(
    modifier = modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Hugging Face Account",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.height(4.dp))

          val statusText =
            when {
              state.isVerifying -> "Verifying credential…"
              state.isAuthenticated && state.accountLabel != null ->
                "Connected as ${state.accountLabel}"
              state.isAuthenticated -> "Connected"
              else -> "Not connected"
            }

          Text(
            text = "Connect your Hugging Face account to access models and datasets.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Spacer(modifier = Modifier.height(8.dp))

          val statusColor =
            when {
              state.isAuthenticated -> MaterialTheme.colorScheme.primary
              state.isVerifying -> MaterialTheme.colorScheme.tertiary
              else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

          Text(text = statusText, style = MaterialTheme.typography.bodySmall, color = statusColor)

          val supportingText = supportingStatusLine(state)
          if (supportingText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = supportingText,
              style = MaterialTheme.typography.bodySmall,
              color =
                if (state.lastError != null) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = onLoginClick,
          modifier =
            Modifier.weight(1f).semantics { contentDescription = "Login with Hugging Face account" },
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
          modifier = Modifier.weight(1f).semantics { contentDescription = "Enter API key manually" },
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
  }
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
