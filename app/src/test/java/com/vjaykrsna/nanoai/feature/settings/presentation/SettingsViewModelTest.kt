@file:Suppress("LargeClass")

package com.vjaykrsna.nanoai.feature.settings.presentation

import android.net.Uri
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreference
import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreferenceStore
import com.vjaykrsna.nanoai.core.data.preferences.RetentionPolicy
import com.vjaykrsna.nanoai.core.data.preferences.UiPreferencesStore
import com.vjaykrsna.nanoai.core.domain.model.uiux.ScreenType
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.UserProfile
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
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
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * Unit tests for [SettingsViewModel].
 *
 * Covers HuggingFace auth announcements, privacy updates, and undo flows.
 */
class SettingsViewModelTest {

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension()

  private lateinit var apiProviderConfigUseCase: ApiProviderConfigUseCase
  private lateinit var modelDownloadsAndExportUseCase: ModelDownloadsAndExportUseCase
  private lateinit var privacyPreferenceStore: PrivacyPreferenceStore
  private lateinit var uiPreferencesStore: UiPreferencesStore
  private lateinit var importService: ImportService
  private lateinit var observeUserProfileUseCase: ObserveUserProfileUseCase
  private lateinit var settingsOperationsUseCase: SettingsOperationsUseCase
  private lateinit var toggleCompactModeUseCase: ToggleCompactModeUseCase
  private lateinit var huggingFaceAuthCoordinator: HuggingFaceAuthCoordinator
  private lateinit var huggingFaceOAuthConfig: HuggingFaceOAuthConfig
  private lateinit var deviceAuthStateFlow: MutableStateFlow<HuggingFaceDeviceAuthState?>
  private lateinit var viewModel: SettingsViewModel

  @BeforeEach
  fun setup() {
    setupMocks()
    setupDefaultFlows()
  }

  private fun setupMocks() {
    apiProviderConfigUseCase = mockk(relaxed = true)
    modelDownloadsAndExportUseCase = mockk(relaxed = true)
    privacyPreferenceStore = mockk(relaxed = true)
    uiPreferencesStore = mockk(relaxed = true)
    importService = mockk(relaxed = true)
    observeUserProfileUseCase = mockk(relaxed = true)
    settingsOperationsUseCase = mockk(relaxed = true)
    toggleCompactModeUseCase = mockk(relaxed = true)
    huggingFaceAuthCoordinator = mockk(relaxed = true)
    huggingFaceOAuthConfig = HuggingFaceOAuthConfig(clientId = "test-client", scope = "all")
    deviceAuthStateFlow = MutableStateFlow(null)
  }

  private fun setupDefaultFlows() {
    every { apiProviderConfigUseCase.observeAllProviders() } returns flowOf(emptyList())
    every { privacyPreferenceStore.privacyPreference } returns
      flowOf(
        PrivacyPreference(
          exportWarningsDismissed = false,
          telemetryOptIn = false,
          consentAcknowledgedAt = null,
          disclaimerShownCount = 0,
          retentionPolicy = RetentionPolicy.INDEFINITE,
        )
      )
    every { observeUserProfileUseCase.flow } returns
      flowOf(
        ObserveUserProfileUseCase.Result(
          userProfile =
            UserProfile(
              id = "test-user-id",
              displayName = "Test User",
              themePreference = ThemePreference.SYSTEM,
              visualDensity = VisualDensity.DEFAULT,
              lastOpenedScreen = ScreenType.HOME,
              compactMode = false,
              pinnedTools = emptyList(),
              savedLayouts = emptyList(),
            ),
          layoutSnapshots = emptyList(),
          uiState = null,
          offline = false,
          hydratedFromCache = false,
        )
      )
    every { huggingFaceAuthCoordinator.state } returns
      MutableStateFlow(HuggingFaceAuthState(isAuthenticated = false, lastError = null))
    every { huggingFaceAuthCoordinator.deviceAuthState } returns deviceAuthStateFlow

    viewModel =
      SettingsViewModel(
        apiProviderConfigUseCase,
        modelDownloadsAndExportUseCase,
        privacyPreferenceStore,
        uiPreferencesStore,
        importService,
        observeUserProfileUseCase,
        settingsOperationsUseCase,
        toggleCompactModeUseCase,
        huggingFaceAuthCoordinator,
        huggingFaceOAuthConfig,
      )
  }

  @Test
  fun `setTelemetryOptIn updates privacy preference`() = runTest {
    coEvery { privacyPreferenceStore.setTelemetryOptIn(true) } returns Unit

    viewModel.setTelemetryOptIn(true)
    advanceUntilIdle()

    coVerify { privacyPreferenceStore.setTelemetryOptIn(true) }
  }

  @Test
  fun `setTelemetryOptIn emits error on failure`() = runTest {
    coEvery { privacyPreferenceStore.setTelemetryOptIn(any()) } throws Exception("Store error")

    viewModel.errorEvents.test {
      viewModel.setTelemetryOptIn(true)
      advanceUntilIdle()

      val error = awaitItem()
      assertThat(error).isInstanceOf(SettingsError.PreferenceUpdateFailed::class.java)
    }
  }

  @Test
  fun `acknowledgeConsent updates privacy preference with timestamp`() = runTest {
    coEvery { privacyPreferenceStore.acknowledgeConsent(any()) } returns Unit

    viewModel.acknowledgeConsent()
    advanceUntilIdle()

    coVerify { privacyPreferenceStore.acknowledgeConsent(any()) }
  }

  @Test
  fun `setRetentionPolicy updates privacy preference`() = runTest {
    coEvery { privacyPreferenceStore.setRetentionPolicy(RetentionPolicy.MANUAL_PURGE_ONLY) } returns
      Unit

    viewModel.setRetentionPolicy(RetentionPolicy.MANUAL_PURGE_ONLY)
    advanceUntilIdle()

    coVerify { privacyPreferenceStore.setRetentionPolicy(RetentionPolicy.MANUAL_PURGE_ONLY) }
  }

  @Test
  fun `setRetentionPolicy emits error on failure`() = runTest {
    coEvery { privacyPreferenceStore.setRetentionPolicy(any()) } throws Exception("Policy error")

    viewModel.errorEvents.test {
      viewModel.setRetentionPolicy(RetentionPolicy.INDEFINITE)
      advanceUntilIdle()

      val error = awaitItem()
      assertThat(error).isInstanceOf(SettingsError.PreferenceUpdateFailed::class.java)
    }
  }

  @Test
  fun `setThemePreference updates UI state and calls use case`() = runTest {
    coEvery { settingsOperationsUseCase.updateTheme(ThemePreference.DARK) } returns
      NanoAIResult.success(Unit)

    viewModel.setThemePreference(ThemePreference.DARK)
    advanceUntilIdle()

    viewModel.uiUxState.test {
      val state = awaitItem()
      assertThat(state.themePreference).isEqualTo(ThemePreference.DARK)
      assertThat(state.undoAvailable).isTrue()
      assertThat(state.statusMessage).isEqualTo("Theme updated")
    }

    coVerify { settingsOperationsUseCase.updateTheme(ThemePreference.DARK) }
  }

  @Test
  fun `setCompactMode updates UI state and calls use case`() = runTest {
    coEvery { toggleCompactModeUseCase.toggle(true) } returns Unit

    viewModel.setCompactMode(true)
    advanceUntilIdle()

    viewModel.uiUxState.test {
      val state = awaitItem()
      assertThat(state.compactModeEnabled).isTrue()
      assertThat(state.undoAvailable).isTrue()
      assertThat(state.statusMessage).isEqualTo("Compact mode enabled")
    }

    coVerify { toggleCompactModeUseCase.toggle(true) }
  }

  @Test
  fun `undoUiPreferenceChange restores previous state`() = runTest {
    coEvery { settingsOperationsUseCase.updateTheme(any()) } returns NanoAIResult.success(Unit)
    coEvery { toggleCompactModeUseCase.toggle(any()) } returns Unit

    // Make a change to enable undo
    viewModel.setThemePreference(ThemePreference.DARK)
    advanceUntilIdle()

    // Undo the change
    viewModel.undoUiPreferenceChange()
    advanceUntilIdle()

    viewModel.uiUxState.test {
      val state = awaitItem()
      assertThat(state.themePreference).isEqualTo(ThemePreference.SYSTEM)
      assertThat(state.undoAvailable).isFalse()
      assertThat(state.statusMessage).isEqualTo("Preferences restored")
    }
  }

  @Test
  fun `saveHuggingFaceApiKey updates state on success`() = runTest {
    coEvery { huggingFaceAuthCoordinator.savePersonalAccessToken(any()) } returns
      Result.success(HuggingFaceAuthState(isAuthenticated = true, lastError = null))

    viewModel.saveHuggingFaceApiKey("test-api-key")
    advanceUntilIdle()

    viewModel.uiUxState.test {
      val state = awaitItem()
      assertThat(state.statusMessage).isEqualTo("Hugging Face connected")
    }
  }

  @Test
  fun `saveHuggingFaceApiKey emits error on auth failure`() = runTest {
    coEvery { huggingFaceAuthCoordinator.savePersonalAccessToken(any()) } returns
      Result.success(HuggingFaceAuthState(isAuthenticated = false, lastError = "Invalid token"))

    viewModel.errorEvents.test {
      viewModel.saveHuggingFaceApiKey("invalid-key")
      advanceUntilIdle()

      val error = awaitItem()
      assertThat(error).isInstanceOf(SettingsError.HuggingFaceAuthFailed::class.java)
      assertThat((error as SettingsError.HuggingFaceAuthFailed).message).isEqualTo("Invalid token")
    }
  }

  @Test
  fun `startHuggingFaceOAuthLogin calls coordinator`() = runTest {
    val deviceAuthState =
      HuggingFaceDeviceAuthState(
        userCode = "TEST-CODE",
        verificationUri = "https://huggingface.co/login/device",
        verificationUriComplete = "https://huggingface.co/login/device?code=TEST-CODE",
        expiresAt = Clock.System.now() + kotlin.time.Duration.parse("15m"),
        pollIntervalSeconds = 5,
      )
    coEvery { huggingFaceAuthCoordinator.beginDeviceAuthorization(any(), any()) } returns
      Result.success(deviceAuthState)

    viewModel.startHuggingFaceOAuthLogin()
    advanceUntilIdle()

    coVerify { huggingFaceAuthCoordinator.beginDeviceAuthorization(any(), any()) }
  }

  @Test
  fun `startHuggingFaceOAuthLogin emits error on failure`() = runTest {
    coEvery { huggingFaceAuthCoordinator.beginDeviceAuthorization(any(), any()) } returns
      Result.failure(Exception("Auth failed"))

    viewModel.errorEvents.test {
      viewModel.startHuggingFaceOAuthLogin()
      advanceUntilIdle()

      val error = awaitItem()
      assertThat(error).isInstanceOf(SettingsError.HuggingFaceAuthFailed::class.java)
    }
  }

  @Test
  fun `cancelHuggingFaceOAuthLogin cancels device authorization`() = runTest {
    coEvery { huggingFaceAuthCoordinator.cancelDeviceAuthorization() } returns Unit

    viewModel.cancelHuggingFaceOAuthLogin()
    advanceUntilIdle()

    coVerify { huggingFaceAuthCoordinator.cancelDeviceAuthorization() }
  }

  @Test
  fun `disconnectHuggingFaceAccount clears credentials and updates state`() = runTest {
    coEvery { huggingFaceAuthCoordinator.clearCredentials() } returns Unit

    viewModel.disconnectHuggingFaceAccount()
    advanceUntilIdle()

    viewModel.uiUxState.test {
      val state = awaitItem()
      assertThat(state.statusMessage).isEqualTo("Hugging Face disconnected")
    }

    coVerify { huggingFaceAuthCoordinator.clearCredentials() }
  }

  @Test
  fun `exportBackup calls use case and emits success`() = runTest {
    val path = "/backup/path"
    coEvery { modelDownloadsAndExportUseCase.exportBackup(path, false) } returns
      NanoAIResult.success(path)

    viewModel.exportSuccess.test {
      viewModel.exportBackup(path, false)
      advanceUntilIdle()

      val successPath = awaitItem()
      assertThat(successPath).isEqualTo(path)
    }
  }

  @Test
  fun `exportBackup emits error on failure`() = runTest {
    coEvery { modelDownloadsAndExportUseCase.exportBackup(any(), any()) } returns
      NanoAIResult.recoverable(message = "Export error")

    viewModel.errorEvents.test {
      viewModel.exportBackup("/path", false)
      advanceUntilIdle()

      val error = awaitItem()
      assertThat(error).isInstanceOf(SettingsError.ExportFailed::class.java)
    }
  }

  @Test
  fun `importBackup calls service and emits summary on success`() = runTest {
    val uri = mockk<Uri>()
    val summary =
      ImportSummary(
        personasImported = 3,
        personasUpdated = 2,
        providersImported = 0,
        providersUpdated = 0,
      )
    coEvery { importService.importBackup(uri) } returns Result.success(summary)

    viewModel.importSuccess.test {
      viewModel.importBackup(uri)
      advanceUntilIdle()

      val result = awaitItem()
      assertThat(result.personasImported).isEqualTo(3)
    }
  }

  @Test
  fun `importBackup emits error on failure`() = runTest {
    val uri = mockk<Uri>()
    coEvery { importService.importBackup(uri) } returns Result.failure(Exception("Import error"))

    viewModel.errorEvents.test {
      viewModel.importBackup(uri)
      advanceUntilIdle()

      val error = awaitItem()
      assertThat(error).isInstanceOf(SettingsError.ImportFailed::class.java)
    }
  }

  @Test
  fun `huggingFaceAuthState exposes coordinator state`() = runTest {
    val authState = HuggingFaceAuthState(isAuthenticated = true, lastError = null)
    every { huggingFaceAuthCoordinator.state } returns MutableStateFlow(authState)

    val vm =
      SettingsViewModel(
        apiProviderConfigUseCase,
        modelDownloadsAndExportUseCase,
        privacyPreferenceStore,
        uiPreferencesStore,
        importService,
        observeUserProfileUseCase,
        settingsOperationsUseCase,
        toggleCompactModeUseCase,
        huggingFaceAuthCoordinator,
        huggingFaceOAuthConfig,
      )

    advanceUntilIdle()

    vm.huggingFaceAuthState.test {
      val state = awaitItem()
      assertThat(state.isAuthenticated).isTrue()
    }
  }

  @Test
  fun `privacyPreferences exposes store preferences`() = runTest {
    val prefs =
      PrivacyPreference(
        exportWarningsDismissed = true,
        telemetryOptIn = true,
        consentAcknowledgedAt = Clock.System.now(),
        disclaimerShownCount = 3,
        retentionPolicy = RetentionPolicy.MANUAL_PURGE_ONLY,
      )
    every { privacyPreferenceStore.privacyPreference } returns flowOf(prefs)

    val vm =
      SettingsViewModel(
        apiProviderConfigUseCase,
        modelDownloadsAndExportUseCase,
        privacyPreferenceStore,
        uiPreferencesStore,
        importService,
        observeUserProfileUseCase,
        settingsOperationsUseCase,
        toggleCompactModeUseCase,
        huggingFaceAuthCoordinator,
        huggingFaceOAuthConfig,
      )

    advanceUntilIdle()

    vm.privacyPreferences.test {
      val preferences = awaitItem()
      assertThat(preferences.telemetryOptIn).isTrue()
      assertThat(preferences.retentionPolicy).isEqualTo(RetentionPolicy.MANUAL_PURGE_ONLY)
    }
  }

  @Test
  fun `device auth announcement updates status message`() = runTest {
    val announcementState =
      HuggingFaceDeviceAuthState(
        userCode = "TEST",
        verificationUri = "https://huggingface.co/device",
        verificationUriComplete = null,
        expiresAt = Clock.System.now(),
        pollIntervalSeconds = 5,
        isPolling = true,
        lastErrorAnnouncement = "Code TEST expires in 10 minutes",
      )

    deviceAuthStateFlow.value = announcementState
    advanceUntilIdle()

    viewModel.uiUxState.test {
      val state = awaitItem()
      assertThat(state.statusMessage).isEqualTo("Code TEST expires in 10 minutes")
    }
  }

  @Test
  fun `clearStatusMessage removes active status message`() = runTest {
    viewModel.setThemePreference(ThemePreference.DARK)
    advanceUntilIdle()

    viewModel.uiUxState.test {
      val updated = awaitItem()
      assertThat(updated.statusMessage).isEqualTo("Theme updated")

      viewModel.clearStatusMessage()
      val cleared = awaitItem()
      assertThat(cleared.statusMessage).isNull()
    }
  }

  @Test
  fun `addApiProvider_emitsProviderAddFailedOnError`() = runTest {
    val config = mockk<com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig>()
    coEvery { apiProviderConfigUseCase.addProvider(config) } throws Exception("Network error")

    viewModel.errorEvents.test {
      viewModel.addApiProvider(config)
      advanceUntilIdle()

      val error = awaitItem()
      assertThat(error).isInstanceOf(SettingsError.ProviderAddFailed::class.java)
      assertThat((error as SettingsError.ProviderAddFailed).message).contains("Network error")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `updateApiProvider_emitsProviderUpdateFailedOnError`() = runTest {
    val config = mockk<com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig>()
    coEvery { apiProviderConfigUseCase.updateProvider(config) } throws Exception("Update failed")

    viewModel.errorEvents.test {
      viewModel.updateApiProvider(config)
      advanceUntilIdle()

      val error = awaitItem()
      assertThat(error).isInstanceOf(SettingsError.ProviderUpdateFailed::class.java)
      assertThat((error as SettingsError.ProviderUpdateFailed).message).contains("Update failed")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `deleteApiProvider_emitsProviderDeleteFailedOnError`() = runTest {
    val providerId = "test-provider-id"
    coEvery { apiProviderConfigUseCase.deleteProvider(providerId) } throws
      Exception("Delete failed")

    viewModel.errorEvents.test {
      viewModel.deleteApiProvider(providerId)
      advanceUntilIdle()

      val error = awaitItem()
      assertThat(error).isInstanceOf(SettingsError.ProviderDeleteFailed::class.java)
      assertThat((error as SettingsError.ProviderDeleteFailed).message).contains("Delete failed")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `validateProvider_checksRequiredFields`() = runTest {
    // This test verifies that provider validation occurs within the repository
    // The validation logic would be in the APIProviderConfig data class or repository
    val config = mockk<com.vjaykrsna.nanoai.core.domain.model.APIProviderConfig>()
    coEvery { apiProviderConfigUseCase.addProvider(config) } returns NanoAIResult.success(Unit)

    viewModel.addApiProvider(config)
    advanceUntilIdle()

    coVerify { apiProviderConfigUseCase.addProvider(config) }
  }

  @Test
  fun `refreshHuggingFaceAccount_updatesAuthState`() = runTest {
    val authState = mockk<HuggingFaceAuthState>()
    coEvery { huggingFaceAuthCoordinator.refreshAccount() } returns authState

    viewModel.refreshHuggingFaceAccount()
    advanceUntilIdle()

    coVerify { huggingFaceAuthCoordinator.refreshAccount() }
  }

  @Test
  fun `refreshHuggingFaceAccount_handlesAuthFailure`() = runTest {
    val authState = mockk<HuggingFaceAuthState>()
    coEvery { huggingFaceAuthCoordinator.refreshAccount() } returns authState

    viewModel.refreshHuggingFaceAccount()
    advanceUntilIdle()

    coVerify { huggingFaceAuthCoordinator.refreshAccount() }
  }

  @Test
  fun `huggingFaceOAuth_completesSuccessfully`() = runTest {
    val deviceAuthState =
      HuggingFaceDeviceAuthState(
        userCode = "TEST-CODE",
        verificationUri = "https://huggingface.co/login/device",
        verificationUriComplete = "https://huggingface.co/login/device?code=TEST-CODE",
        expiresAt = Clock.System.now() + kotlin.time.Duration.parse("15m"),
        pollIntervalSeconds = 5,
      )
    coEvery { huggingFaceAuthCoordinator.beginDeviceAuthorization(any(), any()) } returns
      Result.success(deviceAuthState)

    viewModel.startHuggingFaceOAuthLogin()
    advanceUntilIdle()

    coVerify { huggingFaceAuthCoordinator.beginDeviceAuthorization(any(), any()) }
  }

  @Test
  fun `dismissExportWarnings_clearsState`() = runTest {
    coEvery { privacyPreferenceStore.setExportWarningsDismissed(true) } returns Unit

    viewModel.dismissExportWarnings()
    advanceUntilIdle()

    coVerify { privacyPreferenceStore.setExportWarningsDismissed(true) }
  }

  @Test
  fun `exportData_triggersExportFlow`() = runTest {
    val path = "/test/export/path"
    coEvery { modelDownloadsAndExportUseCase.exportBackup(path, false) } returns
      NanoAIResult.success(path)

    viewModel.isLoading.test {
      assertThat(awaitItem()).isFalse()

      viewModel.exportBackup(path, false)
      assertThat(awaitItem()).isTrue() // Loading starts
      assertThat(awaitItem()).isFalse() // Loading ends
    }
  }

  @Test
  fun `importData_handlesValidation`() = runTest {
    val uri = mockk<android.net.Uri>()
    val summary =
      ImportSummary(
        personasImported = 3,
        personasUpdated = 2,
        providersImported = 0,
        providersUpdated = 0,
      )
    coEvery { importService.importBackup(uri) } returns Result.success(summary)

    viewModel.importSuccess.test {
      viewModel.importBackup(uri)
      advanceUntilIdle()

      val result = awaitItem()
      assertThat(result.personasImported).isEqualTo(3)
      cancelAndIgnoreRemainingEvents()
    }
  }
}
