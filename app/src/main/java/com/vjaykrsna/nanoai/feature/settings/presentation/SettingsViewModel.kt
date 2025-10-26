@file:Suppress("LargeClass")

package com.vjaykrsna.nanoai.feature.settings.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.onFailure
import com.vjaykrsna.nanoai.core.common.onSuccess
import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreference
import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreferenceStore
import com.vjaykrsna.nanoai.core.data.preferences.RetentionPolicy
import com.vjaykrsna.nanoai.core.data.preferences.UiPreferencesStore
import com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.feature.library.domain.ModelDownloadsAndExportUseCase
import com.vjaykrsna.nanoai.feature.settings.domain.ApiProviderConfigUseCase
import com.vjaykrsna.nanoai.feature.settings.domain.ImportService
import com.vjaykrsna.nanoai.feature.settings.domain.ImportSummary
import com.vjaykrsna.nanoai.feature.settings.domain.huggingface.HuggingFaceAuthCoordinator
import com.vjaykrsna.nanoai.feature.settings.domain.huggingface.HuggingFaceAuthState
import com.vjaykrsna.nanoai.feature.settings.domain.huggingface.HuggingFaceDeviceAuthState
import com.vjaykrsna.nanoai.feature.settings.domain.huggingface.HuggingFaceOAuthConfig
import com.vjaykrsna.nanoai.feature.uiux.domain.ObserveUserProfileUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.SettingsOperationsUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.ToggleCompactModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.jvm.JvmName
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
  private val apiProviderConfigUseCase: ApiProviderConfigUseCase,
  private val modelDownloadsAndExportUseCase: ModelDownloadsAndExportUseCase,
  private val privacyPreferenceStore: PrivacyPreferenceStore,
  private val uiPreferencesStore: UiPreferencesStore,
  private val importService: ImportService,
  private val observeUserProfileUseCase: ObserveUserProfileUseCase,
  private val settingsOperationsUseCase: SettingsOperationsUseCase,
  private val toggleCompactModeUseCase: ToggleCompactModeUseCase,
  private val huggingFaceAuthCoordinator: HuggingFaceAuthCoordinator,
  private val huggingFaceOAuthConfig: HuggingFaceOAuthConfig,
) : ViewModel() {
  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _errorEvents = MutableSharedFlow<SettingsError>()
  val errorEvents = _errorEvents.asSharedFlow()

  private val _exportSuccess = MutableSharedFlow<String>()
  val exportSuccess = _exportSuccess.asSharedFlow()

  private val _importSuccess = MutableSharedFlow<ImportSummary>()
  val importSuccess = _importSuccess.asSharedFlow()

  private val _uiUxState = MutableStateFlow(SettingsUiUxState())
  val uiUxState: StateFlow<SettingsUiUxState> = _uiUxState.asStateFlow()
  private var previousUiUxState: SettingsUiUxState? = null

  val huggingFaceAuthState: StateFlow<HuggingFaceAuthState> = huggingFaceAuthCoordinator.state

  val huggingFaceDeviceAuthState: StateFlow<HuggingFaceDeviceAuthState?> =
    huggingFaceAuthCoordinator.deviceAuthState

  val apiProviders: StateFlow<List<APIProviderConfig>> =
    apiProviderConfigUseCase
      .observeAllProviders()
      .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val privacyPreferences: StateFlow<PrivacyPreference> =
    privacyPreferenceStore.privacyPreference.stateIn(
      viewModelScope,
      SharingStarted.Eagerly,
      PrivacyPreference(
        exportWarningsDismissed = false,
        telemetryOptIn = false,
        consentAcknowledgedAt = null,
        disclaimerShownCount = 0,
        retentionPolicy = RetentionPolicy.INDEFINITE,
      ),
    )

  val uiPreferences: StateFlow<com.vjaykrsna.nanoai.core.data.preferences.UiPreferences> =
    uiPreferencesStore.uiPreferences.stateIn(
      viewModelScope,
      SharingStarted.Eagerly,
      com.vjaykrsna.nanoai.core.data.preferences.UiPreferences(),
    )

  init {
    observeUserProfileUseCase.flow
      .onEach { result ->
        val profile = result.userProfile
        _uiUxState.update { current ->
          current.copy(
            themePreference = profile?.themePreference ?: ThemePreference.SYSTEM,
            compactModeEnabled = profile?.compactMode ?: false,
            undoAvailable = previousUiUxState != null,
          )
        }
      }
      .launchIn(viewModelScope)

    uiPreferences
      .onEach { prefs ->
        _uiUxState.update { current ->
          current.copy(highContrastEnabled = prefs.highContrastEnabled)
        }
      }
      .launchIn(viewModelScope)

    huggingFaceAuthCoordinator.deviceAuthState
      .map { deviceState ->
        val announcement = deviceState?.lastErrorAnnouncement
        when {
          !announcement.isNullOrBlank() -> announcement
          !deviceState?.lastError.isNullOrBlank() -> deviceState?.lastError
          else -> null
        }
      }
      .distinctUntilChanged()
      .onEach { announcement ->
        if (announcement != null) {
          _uiUxState.update { current -> current.copy(statusMessage = announcement) }
        }
      }
      .launchIn(viewModelScope)
  }

  fun addApiProvider(config: APIProviderConfig) {
    viewModelScope.launch {
      _isLoading.value = true
      apiProviderConfigUseCase.addProvider(config).onFailure { error ->
        _errorEvents.emit(
          SettingsError.ProviderAddFailed(error.message ?: "Failed to add provider")
        )
      }
      _isLoading.value = false
    }
  }

  fun updateApiProvider(config: APIProviderConfig) {
    viewModelScope.launch {
      _isLoading.value = true
      apiProviderConfigUseCase.updateProvider(config).onFailure { error ->
        _errorEvents.emit(
          SettingsError.ProviderUpdateFailed(error.message ?: "Failed to update provider")
        )
      }
      _isLoading.value = false
    }
  }

  fun deleteApiProvider(providerId: String) {
    viewModelScope.launch {
      _isLoading.value = true
      apiProviderConfigUseCase.deleteProvider(providerId).onFailure { error ->
        _errorEvents.emit(
          SettingsError.ProviderDeleteFailed(error.message ?: "Failed to delete provider")
        )
      }
      _isLoading.value = false
    }
  }

  fun exportBackup(destinationPath: String, includeChatHistory: Boolean = false) {
    viewModelScope.launch {
      _isLoading.value = true
      modelDownloadsAndExportUseCase
        .exportBackup(destinationPath, includeChatHistory)
        .onSuccess { path -> _exportSuccess.emit(path) }
        .onFailure { error ->
          _errorEvents.emit(SettingsError.ExportFailed(error.message ?: "Export failed"))
        }
      _isLoading.value = false
    }
  }

  fun importBackup(uri: Uri) {
    viewModelScope.launch {
      _isLoading.value = true
      runCatching { importService.importBackup(uri) }
        .fold(
          onSuccess = { result ->
            result
              .onSuccess { summary -> _importSuccess.emit(summary) }
              .onFailure { error ->
                _errorEvents.emit(SettingsError.ImportFailed(error.message ?: "Import failed"))
              }
          },
          onFailure = { error ->
            _errorEvents.emit(SettingsError.UnexpectedError(error.message ?: "Unexpected error"))
          },
        )
      _isLoading.value = false
    }
  }

  fun setTelemetryOptIn(optIn: Boolean) {
    viewModelScope.launch {
      runCatching { privacyPreferenceStore.setTelemetryOptIn(optIn) }
        .onFailure { error ->
          _errorEvents.emit(
            SettingsError.PreferenceUpdateFailed(error.message ?: "Failed to update preference")
          )
        }
    }
  }

  fun acknowledgeConsent() {
    viewModelScope.launch {
      runCatching { privacyPreferenceStore.acknowledgeConsent(Clock.System.now()) }
        .onFailure { error ->
          _errorEvents.emit(
            SettingsError.PreferenceUpdateFailed(error.message ?: "Failed to acknowledge consent")
          )
        }
    }
  }

  fun setRetentionPolicy(policy: RetentionPolicy) {
    viewModelScope.launch {
      runCatching { privacyPreferenceStore.setRetentionPolicy(policy) }
        .onFailure { error ->
          _errorEvents.emit(
            SettingsError.PreferenceUpdateFailed(error.message ?: "Failed to set retention policy")
          )
        }
    }
  }

  fun dismissExportWarnings() {
    viewModelScope.launch {
      runCatching { privacyPreferenceStore.setExportWarningsDismissed(true) }
        .onFailure { error ->
          _errorEvents.emit(
            SettingsError.PreferenceUpdateFailed(error.message ?: "Failed to dismiss warnings")
          )
        }
    }
  }

  fun setThemePreference(themePreference: ThemePreference) {
    previousUiUxState = _uiUxState.value
    _uiUxState.update {
      it.copy(
        themePreference = themePreference,
        undoAvailable = true,
        statusMessage = "Theme updated",
      )
    }
    viewModelScope.launch { settingsOperationsUseCase.updateTheme(themePreference) }
  }

  fun applyDensityPreference(compactModeEnabled: Boolean) {
    setCompactMode(compactModeEnabled)
  }

  @JvmName("internalSetLayoutMode")
  fun setCompactMode(enabled: Boolean) {
    previousUiUxState = _uiUxState.value
    _uiUxState.update {
      it.copy(
        compactModeEnabled = enabled,
        undoAvailable = true,
        statusMessage = if (enabled) "Compact mode enabled" else "Compact mode disabled",
      )
    }
    viewModelScope.launch { toggleCompactModeUseCase.toggle(enabled) }
  }

  fun setHighContrastEnabled(enabled: Boolean) {
    _uiUxState.update {
      it.copy(
        highContrastEnabled = enabled,
        undoAvailable = true,
        statusMessage = if (enabled) "High contrast enabled" else "High contrast disabled",
      )
    }
    viewModelScope.launch { uiPreferencesStore.setHighContrastEnabled(enabled) }
  }

  fun undoUiPreferenceChange() {
    val previous = previousUiUxState ?: return
    _uiUxState.value = previous.copy(undoAvailable = false, statusMessage = "Preferences restored")
    previousUiUxState = null
    viewModelScope.launch {
      settingsOperationsUseCase.updateTheme(previous.themePreference)
      toggleCompactModeUseCase.toggle(previous.compactModeEnabled)
    }
  }

  fun onMigrationSuccess() {
    _uiUxState.update { it.copy(showMigrationSuccessNotification = true) }
  }

  fun dismissMigrationSuccessNotification() {
    _uiUxState.update { it.copy(showMigrationSuccessNotification = false) }
  }

  fun saveHuggingFaceApiKey(apiKey: String) {
    viewModelScope.launch {
      huggingFaceAuthCoordinator
        .savePersonalAccessToken(apiKey)
        .onSuccess { authState ->
          if (authState.isAuthenticated) {
            _uiUxState.update {
              it.copy(statusMessage = "Hugging Face connected", undoAvailable = false)
            }
          } else if (authState.lastError != null) {
            _errorEvents.emit(SettingsError.HuggingFaceAuthFailed(authState.lastError))
          }
        }
        .onFailure { throwable ->
          _errorEvents.emit(
            SettingsError.HuggingFaceAuthFailed(
              throwable.message ?: "Failed to save Hugging Face API key"
            )
          )
        }
    }
  }

  fun refreshHuggingFaceAccount() {
    viewModelScope.launch { huggingFaceAuthCoordinator.refreshAccount() }
  }

  fun startHuggingFaceOAuthLogin() {
    viewModelScope.launch {
      val clientId = huggingFaceOAuthConfig.clientId.trim()
      val scope = huggingFaceOAuthConfig.scope.ifBlank { DEFAULT_OAUTH_SCOPE }

      if (clientId.isBlank()) {
        _errorEvents.emit(
          SettingsError.HuggingFaceAuthFailed("Hugging Face OAuth client ID is not configured")
        )
        return@launch
      }

      huggingFaceAuthCoordinator
        .beginDeviceAuthorization(clientId = clientId, scope = scope)
        .onFailure { throwable ->
          _errorEvents.emit(
            SettingsError.HuggingFaceAuthFailed(
              throwable.message ?: "Unable to start Hugging Face sign-in"
            )
          )
        }
    }
  }

  fun cancelHuggingFaceOAuthLogin() {
    viewModelScope.launch { huggingFaceAuthCoordinator.cancelDeviceAuthorization() }
  }

  private companion object {
    private const val DEFAULT_OAUTH_SCOPE = "all offline_access"
  }

  fun disconnectHuggingFaceAccount() {
    viewModelScope.launch {
      huggingFaceAuthCoordinator.clearCredentials()
      _uiUxState.update {
        it.copy(statusMessage = "Hugging Face disconnected", undoAvailable = false)
      }
    }
  }

  fun clearStatusMessage() {
    _uiUxState.update { it.copy(statusMessage = null) }
  }
}

sealed class SettingsError {
  data class ProviderAddFailed(val message: String) : SettingsError()

  data class ProviderUpdateFailed(val message: String) : SettingsError()

  data class ProviderDeleteFailed(val message: String) : SettingsError()

  data class ExportFailed(val message: String) : SettingsError()

  data class ImportFailed(val message: String) : SettingsError()

  data class PreferenceUpdateFailed(val message: String) : SettingsError()

  data class UnexpectedError(val message: String) : SettingsError()

  data class HuggingFaceAuthFailed(val message: String) : SettingsError()
}

data class SettingsUiUxState(
  val themePreference: ThemePreference = ThemePreference.SYSTEM,
  val compactModeEnabled: Boolean = false,
  val highContrastEnabled: Boolean = false,
  val undoAvailable: Boolean = false,
  val statusMessage: String? = null,
  val showMigrationSuccessNotification: Boolean = false,
)
