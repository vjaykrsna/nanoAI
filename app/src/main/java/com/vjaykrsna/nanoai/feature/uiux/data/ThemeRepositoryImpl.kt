package com.vjaykrsna.nanoai.feature.uiux.data

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.data.repository.ThemeRepository
import com.vjaykrsna.nanoai.core.data.repository.UserProfileRepository
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.UiPreferencesSnapshot as DomainUiPreferencesSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.feature.uiux.domain.UIUX_DEFAULT_USER_ID
import com.vjaykrsna.nanoai.feature.uiux.state.UiPreferenceSnapshot
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

@Singleton
class ThemeRepositoryImpl
@Inject
constructor(
  private val userProfileRepository: UserProfileRepository,
  @IoDispatcher override val ioDispatcher: CoroutineDispatcher,
) : ThemeRepository {

  private val scope = CoroutineScope(ioDispatcher + SupervisorJob())
  private val userId = UIUX_DEFAULT_USER_ID

  private val preferences =
    userProfileRepository
      .observePreferences()
      .stateIn(scope, SharingStarted.Eagerly, DomainUiPreferencesSnapshot())

  override val uiPreferenceSnapshot: Flow<UiPreferenceSnapshot> =
    preferences
      .map { snapshot -> snapshot.toUiPreferenceSnapshot() }
      .stateIn(scope, SharingStarted.Eagerly, UiPreferenceSnapshot())

  override suspend fun updateThemePreference(theme: ThemePreference) {
    withContext(ioDispatcher) { userProfileRepository.updateThemePreference(userId, theme.name) }
  }

  override suspend fun updateVisualDensity(density: VisualDensity) {
    withContext(ioDispatcher) { userProfileRepository.updateVisualDensity(userId, density.name) }
  }

  private fun DomainUiPreferencesSnapshot.toUiPreferenceSnapshot(): UiPreferenceSnapshot =
    UiPreferenceSnapshot(
      theme = themePreference,
      density = visualDensity,
      fontScale = 1f,
      dismissedTooltips = emptySet(),
    )
}
