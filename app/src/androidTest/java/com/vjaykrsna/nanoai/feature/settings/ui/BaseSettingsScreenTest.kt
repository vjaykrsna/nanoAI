package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreference
import com.vjaykrsna.nanoai.core.data.preferences.RetentionPolicy
import com.vjaykrsna.nanoai.core.domain.model.ApiProviderConfig
import com.vjaykrsna.nanoai.feature.settings.domain.ImportSummary
import com.vjaykrsna.nanoai.feature.settings.domain.huggingface.HuggingFaceAuthState
import com.vjaykrsna.nanoai.feature.settings.domain.huggingface.HuggingFaceDeviceAuthState
import com.vjaykrsna.nanoai.feature.settings.presentation.SettingsError
import com.vjaykrsna.nanoai.feature.settings.presentation.SettingsUiUxState
import com.vjaykrsna.nanoai.feature.settings.presentation.SettingsViewModel
import com.vjaykrsna.nanoai.testing.TestEnvironmentRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule

@OptIn(ExperimentalTestApi::class)
abstract class BaseSettingsScreenTest {

  @JvmField @Rule val composeTestRule: ComposeContentTestRule = createComposeRule()
  @JvmField @Rule val testEnvironmentRule = TestEnvironmentRule()

  protected lateinit var viewModel: SettingsViewModel

  protected val mockApiProviders = MutableStateFlow<List<ApiProviderConfig>>(emptyList())
  protected val mockPrivacyPreferences =
    MutableStateFlow(
      PrivacyPreference(
        exportWarningsDismissed = false,
        telemetryOptIn = false,
        consentAcknowledgedAt = null,
        disclaimerShownCount = 0,
        retentionPolicy = RetentionPolicy.INDEFINITE,
      )
    )
  protected val mockUiUxState = MutableStateFlow(SettingsUiUxState())
  protected val mockHuggingFaceAuthState = MutableStateFlow(HuggingFaceAuthState.unauthenticated())
  protected val mockHuggingFaceDeviceAuthState = MutableStateFlow<HuggingFaceDeviceAuthState?>(null)
  protected val mockErrorEvents = MutableSharedFlow<SettingsError>(extraBufferCapacity = 1)
  protected val mockExportSuccess = MutableSharedFlow<String>(extraBufferCapacity = 1)
  protected val mockImportSuccess = MutableSharedFlow<ImportSummary>(extraBufferCapacity = 1)

  @Before
  fun setUpBase() {
    viewModel = mockk(relaxed = true)
    every { viewModel.apiProviders } returns mockApiProviders
    every { viewModel.privacyPreferences } returns mockPrivacyPreferences
    every { viewModel.uiUxState } returns mockUiUxState
    every { viewModel.huggingFaceAuthState } returns mockHuggingFaceAuthState
    every { viewModel.huggingFaceDeviceAuthState } returns mockHuggingFaceDeviceAuthState
    every { viewModel.errorEvents } returns mockErrorEvents
    every { viewModel.exportSuccess } returns mockExportSuccess
    every { viewModel.importSuccess } returns mockImportSuccess
  }

  protected fun renderSettingsScreen() {
    composeTestRule.setContent { SettingsScreen(viewModel = viewModel) }
    composeTestRule.waitForIdle()
  }
}
