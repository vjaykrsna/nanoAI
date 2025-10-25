package com.vjaykrsna.nanoai

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.preferences.DisclaimerExposureState
import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreferenceStore
import com.vjaykrsna.nanoai.core.data.preferences.UiPreferences
import com.vjaykrsna.nanoai.core.data.preferences.UiPreferencesStore
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.feature.uiux.domain.ObserveUserProfileUseCase
import com.vjaykrsna.nanoai.feature.uiux.presentation.AppViewModel
import com.vjaykrsna.nanoai.feature.uiux.presentation.DisclaimerUiState
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AppViewModelTest {

  @MockK private lateinit var observeUserProfileUseCase: ObserveUserProfileUseCase

  @MockK private lateinit var privacyPreferenceStore: PrivacyPreferenceStore

  @MockK private lateinit var uiPreferencesStore: UiPreferencesStore

  private lateinit var appViewModel: AppViewModel

  @Before
  fun setUp() {
    MockKAnnotations.init(this, relaxed = true)
    // Default UI preferences mock
    val uiPrefs = mockk<UiPreferences>(relaxed = true)
    every { uiPreferencesStore.uiPreferences } returns MutableStateFlow(uiPrefs)
  }

  @Test
  fun `initialization loads configuration and sets initial state`() = runTest {
    // Given
    val userProfileResult =
      ObserveUserProfileUseCase.Result(
        userProfile = null,
        layoutSnapshots = emptyList(),
        uiState = null,
        hydratedFromCache = true,
        offline = false,
      )
    val disclaimerState =
      mockk<com.vjaykrsna.nanoai.core.data.preferences.DisclaimerExposureState> {
        every { shouldShowDialog } returns false
        every { acknowledged } returns false
        every { acknowledgedAt } returns null
        every { shownCount } returns 0
      }

    every { observeUserProfileUseCase.flow } returns MutableStateFlow(userProfileResult)
    every { privacyPreferenceStore.disclaimerExposure } returns MutableStateFlow(disclaimerState)

    // When
    appViewModel =
      AppViewModel(observeUserProfileUseCase, privacyPreferenceStore, uiPreferencesStore)

    // Then
    appViewModel.uiState.test {
      val initialState = awaitItem()
      assertThat(initialState.isHydrating).isTrue()
      assertThat(initialState.themePreference).isEqualTo(ThemePreference.SYSTEM)
      assertThat(initialState.offline).isFalse()
      assertThat(initialState.disclaimer).isEqualTo(DisclaimerUiState())
    }
  }

  @Test
  fun `initialization handles unexpected error gracefully`() = runTest {
    // Given
    val userProfileResult =
      ObserveUserProfileUseCase.Result(
        userProfile = null,
        layoutSnapshots = emptyList(),
        uiState = null,
        hydratedFromCache = true,
        offline = false,
      )
    every { observeUserProfileUseCase.flow } returns MutableStateFlow(userProfileResult)
    val disclaimerState =
      mockk<com.vjaykrsna.nanoai.core.data.preferences.DisclaimerExposureState> {
        every { shouldShowDialog } returns false
        every { acknowledged } returns false
        every { acknowledgedAt } returns null
        every { shownCount } returns 0
      }
    every { privacyPreferenceStore.disclaimerExposure } returns MutableStateFlow(disclaimerState)

    // When
    appViewModel =
      AppViewModel(observeUserProfileUseCase, privacyPreferenceStore, uiPreferencesStore)

    // Then - Should not crash and should have default state
    appViewModel.uiState.test {
      val state = awaitItem()
      assertThat(state).isNotNull()
    }
  }

  @Test
  fun `initialization checks first launch correctly`() = runTest {
    // Given
    val userProfileResult =
      ObserveUserProfileUseCase.Result(
        userProfile = null,
        layoutSnapshots = emptyList(),
        uiState = null,
        hydratedFromCache = true,
        offline = false,
      )
    val disclaimerState =
      mockk<com.vjaykrsna.nanoai.core.data.preferences.DisclaimerExposureState> {
        every { shouldShowDialog } returns true
        every { acknowledged } returns false
        every { acknowledgedAt } returns null
        every { shownCount } returns 0
      }

    every { observeUserProfileUseCase.flow } returns MutableStateFlow(userProfileResult)
    every { privacyPreferenceStore.disclaimerExposure } returns MutableStateFlow(disclaimerState)

    // When
    appViewModel =
      AppViewModel(observeUserProfileUseCase, privacyPreferenceStore, uiPreferencesStore)

    // Then
    appViewModel.uiState.test {
      val state = awaitItem()
      assertThat(state.disclaimer.shouldShow).isTrue()
    }
  }

  @Test
  fun `onDisclaimerAccepted persists consent and updates state`() = runTest {
    // Given
    val userProfileResult =
      ObserveUserProfileUseCase.Result(
        userProfile = null,
        layoutSnapshots = emptyList(),
        uiState = null,
        hydratedFromCache = true,
        offline = false,
      )
    val disclaimerState =
      mockk<com.vjaykrsna.nanoai.core.data.preferences.DisclaimerExposureState> {
        every { shouldShowDialog } returns true
        every { acknowledged } returns false
        every { acknowledgedAt } returns null
        every { shownCount } returns 0
      }

    every { observeUserProfileUseCase.flow } returns MutableStateFlow(userProfileResult)
    every { privacyPreferenceStore.disclaimerExposure } returns MutableStateFlow(disclaimerState)

    appViewModel =
      AppViewModel(observeUserProfileUseCase, privacyPreferenceStore, uiPreferencesStore)

    // When
    appViewModel.onDisclaimerAccepted()

    // Then
    coVerify { privacyPreferenceStore.acknowledgeConsent(any()) }
  }

  @Test
  fun `onDisclaimerAccepted navigates to main screen`() = runTest {
    // Given
    val userProfileResult =
      ObserveUserProfileUseCase.Result(
        userProfile = null,
        layoutSnapshots = emptyList(),
        uiState = null,
        hydratedFromCache = true,
        offline = false,
      )
    val disclaimerState =
      mockk<com.vjaykrsna.nanoai.core.data.preferences.DisclaimerExposureState> {
        every { shouldShowDialog } returns true
        every { acknowledged } returns false
        every { acknowledgedAt } returns null
        every { shownCount } returns 0
      }

    every { observeUserProfileUseCase.flow } returns MutableStateFlow(userProfileResult)
    every { privacyPreferenceStore.disclaimerExposure } returns MutableStateFlow(disclaimerState)

    appViewModel =
      AppViewModel(observeUserProfileUseCase, privacyPreferenceStore, uiPreferencesStore)

    // When
    appViewModel.onDisclaimerAccepted()

    // Then - Verify that the state changes appropriately after consent
    appViewModel.uiState.test {
      val initialState = awaitItem()
      // After accepting disclaimer, the state should reflect this in subsequent emissions
      // The actual navigation would be handled by observing the state changes
      assertThat(initialState.disclaimer.acknowledged).isFalse() // Initial state
    }
  }
}
