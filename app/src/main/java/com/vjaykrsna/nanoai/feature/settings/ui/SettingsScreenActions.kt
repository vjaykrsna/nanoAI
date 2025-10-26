package com.vjaykrsna.nanoai.feature.settings.ui

import android.net.Uri
import android.os.Environment
import androidx.activity.compose.ManagedActivityResultLauncher
import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreference
import com.vjaykrsna.nanoai.core.data.preferences.RetentionPolicy
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.core.model.APIType
import com.vjaykrsna.nanoai.feature.settings.presentation.SettingsViewModel
import java.util.UUID

internal data class SettingsScreenActions(
  val onAddProviderClick: () -> Unit,
  val onProviderEdit: (APIProviderConfig) -> Unit,
  val onProviderDelete: (APIProviderConfig) -> Unit,
  val onImportBackupClick: () -> Unit,
  val onExportBackupClick: () -> Unit,
  val onTelemetryToggle: (Boolean) -> Unit,
  val onRetentionPolicyChange: (RetentionPolicy) -> Unit,
  val onDismissMigrationSuccess: () -> Unit,
  val onThemePreferenceChange: (ThemePreference) -> Unit,
  val onVisualDensityChange: (VisualDensity) -> Unit,
  val onHighContrastChange: (Boolean) -> Unit,
  val onHuggingFaceLoginClick: () -> Unit,
  val onHuggingFaceApiKeyClick: () -> Unit,
  val onHuggingFaceDisconnectClick: () -> Unit,
  val onStatusMessageShow: () -> Unit,
)

internal data class SettingsActionDependencies(
  val viewModel: SettingsViewModel,
  val privacyPreferences: PrivacyPreference,
  val importBackupLauncher: ManagedActivityResultLauncher<Array<String>, Uri?>,
)

internal data class ProviderDialogCallbacks(
  val onAddProviderClick: () -> Unit,
  val onEditProvider: (APIProviderConfig) -> Unit,
  val onDeleteProvider: (APIProviderConfig) -> Unit,
  val onShowExportDialog: () -> Unit,
)

internal fun createActions(
  viewModel: SettingsViewModel,
  privacyPreferences: PrivacyPreference,
  importBackupLauncher: ManagedActivityResultLauncher<Array<String>, Uri?>,
  dialogState: MutableSettingsDialogState,
): SettingsScreenActions {
  val dependencies =
    SettingsActionDependencies(
      viewModel = viewModel,
      privacyPreferences = privacyPreferences,
      importBackupLauncher = importBackupLauncher,
    )

  val callbacks =
    ProviderDialogCallbacks(
      onAddProviderClick = { dialogState.showAddProviderDialog.value = true },
      onEditProvider = { provider -> dialogState.editingProvider.value = provider },
      onDeleteProvider = { provider -> viewModel.deleteApiProvider(provider.providerId) },
      onShowExportDialog = { dialogState.showExportDialog.value = true },
    )

  return createSettingsActions(
    dependencies = dependencies,
    dialogCallbacks = callbacks,
    dialogState = dialogState,
  )
}

internal fun createSettingsActions(
  dependencies: SettingsActionDependencies,
  dialogCallbacks: ProviderDialogCallbacks,
  dialogState: MutableSettingsDialogState,
): SettingsScreenActions {
  val (viewModel, privacyPreferences, importBackupLauncher) = dependencies

  return SettingsScreenActions(
    onAddProviderClick = dialogCallbacks.onAddProviderClick,
    onProviderEdit = dialogCallbacks.onEditProvider,
    onProviderDelete = dialogCallbacks.onDeleteProvider,
    onImportBackupClick = {
      importBackupLauncher.launch(
        arrayOf("application/json", "application/zip", "application/octet-stream")
      )
    },
    onExportBackupClick = {
      handleExportRequest(
        warningsDismissed = privacyPreferences.exportWarningsDismissed,
        onExport = { viewModel.exportBackupToDownloads() },
        onShowDialog = dialogCallbacks.onShowExportDialog,
      )
    },
    onTelemetryToggle = viewModel::setTelemetryOptIn,
    onRetentionPolicyChange = viewModel::setRetentionPolicy,
    onDismissMigrationSuccess = viewModel::dismissMigrationSuccessNotification,
    onThemePreferenceChange = viewModel::setThemePreference,
    onVisualDensityChange = { density ->
      when (density) {
        VisualDensity.COMPACT -> viewModel.setCompactMode(true)
        VisualDensity.DEFAULT,
        VisualDensity.EXPANDED -> viewModel.setCompactMode(false)
      }
    },
    onHighContrastChange = viewModel::setHighContrastEnabled,
    onHuggingFaceLoginClick = { dialogState.showHuggingFaceLoginDialog.value = true },
    onHuggingFaceApiKeyClick = { dialogState.showHuggingFaceApiKeyDialog.value = true },
    onHuggingFaceDisconnectClick = viewModel::disconnectHuggingFaceAccount,
    onStatusMessageShow = viewModel::clearStatusMessage,
  )
}

internal fun handleExportRequest(
  warningsDismissed: Boolean,
  onExport: () -> Unit,
  onShowDialog: () -> Unit,
) {
  if (warningsDismissed) {
    onExport()
  } else {
    onShowDialog()
  }
}

internal fun confirmExport(
  dontShowAgain: Boolean,
  dismissWarnings: () -> Unit,
  export: () -> Unit,
) {
  if (dontShowAgain) {
    dismissWarnings()
  }
  export()
}

internal data class ProviderMutation(
  val editingProvider: APIProviderConfig?,
  val onAdd: (APIProviderConfig) -> Unit,
  val onUpdate: (APIProviderConfig) -> Unit,
)

internal fun saveProvider(
  mutation: ProviderMutation,
  name: String,
  baseUrl: String,
  apiKey: String?,
) {
  mutation.editingProvider?.let { provider ->
    mutation.onUpdate(provider.copy(providerName = name, baseUrl = baseUrl, apiKey = apiKey ?: ""))
  }
    ?: mutation.onAdd(
      APIProviderConfig(
        providerId = UUID.randomUUID().toString(),
        providerName = name,
        baseUrl = baseUrl,
        apiKey = apiKey ?: "",
        apiType = APIType.OPENAI_COMPATIBLE,
        isEnabled = true,
      )
    )
}

internal fun SettingsViewModel.exportBackupToDownloads(includeChatHistory: Boolean = true) {
  val downloadsPath =
    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath +
      "/nanoai-backup-${System.currentTimeMillis()}.json"
  exportBackup(downloadsPath, includeChatHistory)
}
