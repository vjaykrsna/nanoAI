package com.vjaykrsna.nanoai.feature.uiux.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.data.preferences.PrivacyPreferenceStore
import com.vjaykrsna.nanoai.core.data.preferences.UiPreferencesStore
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.feature.uiux.domain.ObserveUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Top-level application view model responsible for exposing global UI state such as theme
 * preferences and hydration status.
 */
@HiltViewModel
class AppViewModel
@Inject
constructor(
  observeUserProfileUseCase: ObserveUserProfileUseCase,
  private val privacyPreferenceStore: PrivacyPreferenceStore,
  private val uiPreferencesStore: UiPreferencesStore,
) : ViewModel() {
  private val _uiState = MutableStateFlow(AppUiState())
  val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      combine(
          observeUserProfileUseCase.flow,
          privacyPreferenceStore.disclaimerExposure,
          uiPreferencesStore.uiPreferences,
        ) { result, disclaimer, uiPrefs ->
          Triple(result, disclaimer, uiPrefs)
        }
        .collect { (result, disclaimer, uiPrefs) ->
          val profile = result.userProfile
          val themePreference = profile?.themePreference ?: ThemePreference.SYSTEM
          val hydrating = profile == null && result.hydratedFromCache

          _uiState.update {
            it.copy(
              themePreference = themePreference,
              highContrastEnabled = uiPrefs.highContrastEnabled,
              isHydrating = hydrating,
              offline = result.offline,
              disclaimer =
                DisclaimerUiState(
                  shouldShow = disclaimer.shouldShowDialog,
                  acknowledged = disclaimer.acknowledged,
                  acknowledgedAt = disclaimer.acknowledgedAt,
                  shownCount = disclaimer.shownCount,
                ),
            )
          }
        }
    }
  }

  /** Records that the disclaimer dialog was surfaced during this session. */
  fun onDisclaimerDisplayed() {
    viewModelScope.launch { privacyPreferenceStore.incrementDisclaimerShown() }
  }

  /** Marks the disclaimer as acknowledged so subsequent launches skip the dialog. */
  fun onDisclaimerAccepted() {
    viewModelScope.launch { privacyPreferenceStore.acknowledgeConsent(Clock.System.now()) }
  }
}

/** Global application UI state exposed from [AppViewModel]. */
data class AppUiState(
  val themePreference: ThemePreference = ThemePreference.SYSTEM,
  val highContrastEnabled: Boolean = false,
  val isHydrating: Boolean = true,
  val offline: Boolean = false,
  val disclaimer: DisclaimerUiState = DisclaimerUiState(),
)

/** Snapshot describing the state of the first-run disclaimer. */
data class DisclaimerUiState(
  val shouldShow: Boolean = false,
  val acknowledged: Boolean = false,
  val acknowledgedAt: Instant? = null,
  val shownCount: Int = 0,
)
