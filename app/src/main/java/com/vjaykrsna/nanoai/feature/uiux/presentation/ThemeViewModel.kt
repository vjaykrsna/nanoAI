package com.vjaykrsna.nanoai.feature.uiux.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.core.data.repository.ThemeRepository
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.feature.uiux.domain.SettingsOperationsUseCase
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel responsible for theme and UI preference management. */
class ThemeViewModel
@Inject
constructor(
  private val themeRepository: ThemeRepository,
  private val settingsOperationsUseCase: SettingsOperationsUseCase,
  @Suppress("UnusedPrivateProperty")
  @MainImmediateDispatcher
  private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {
  /** Current UI preferences including theme, density, and other display settings. */
  val uiPreferences: StateFlow<UiPreferenceSnapshot> =
    themeRepository.uiPreferenceSnapshot.stateIn(
      scope = viewModelScope,
      started = SharingStarted.Eagerly,
      initialValue = UiPreferenceSnapshot(),
    )

  /** Updates the theme preference for the active user. */
  fun updateThemePreference(theme: ThemePreference) {
    viewModelScope.launch { settingsOperationsUseCase.updateTheme(theme) }
  }

  /** Updates the visual density preference for the active user. */
  fun updateVisualDensity(density: VisualDensity) {
    viewModelScope.launch { settingsOperationsUseCase.updateVisualDensity(density) }
  }

  /** Updates the high contrast enabled preference for the active user. */
  fun updateHighContrastEnabled(enabled: Boolean) {
    viewModelScope.launch { themeRepository.updateHighContrastEnabled(enabled) }
  }
}
