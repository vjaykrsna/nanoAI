package com.vjaykrsna.nanoai.feature.uiux.data

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.data.repository.NavigationRepository
import com.vjaykrsna.nanoai.core.data.repository.UserProfileRepository
import com.vjaykrsna.nanoai.core.domain.model.uiux.UIStateSnapshot
import com.vjaykrsna.nanoai.feature.uiux.domain.UIUX_DEFAULT_USER_ID
import com.vjaykrsna.nanoai.feature.uiux.state.CommandCategory
import com.vjaykrsna.nanoai.feature.uiux.state.CommandPaletteState
import com.vjaykrsna.nanoai.feature.uiux.state.ModeId
import com.vjaykrsna.nanoai.feature.uiux.state.PaletteSource
import com.vjaykrsna.nanoai.feature.uiux.state.RecentActivityItem
import com.vjaykrsna.nanoai.feature.uiux.state.RightPanel
import com.vjaykrsna.nanoai.feature.uiux.state.UndoPayload
import com.vjaykrsna.nanoai.ui.navigation.Screen
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class NavigationRepositoryImpl
@Inject
constructor(
  private val userProfileRepository: UserProfileRepository,
  @IoDispatcher override val ioDispatcher: CoroutineDispatcher,
) : NavigationRepository {

  private val scope = CoroutineScope(ioDispatcher + SupervisorJob())
  private val userId: String = UIUX_DEFAULT_USER_ID
  private val hasAppliedHomeStartup = AtomicBoolean(false)

  private val uiSnapshot: StateFlow<UIStateSnapshot> =
    userProfileRepository
      .observeUIStateSnapshot(userId)
      .map { snapshot -> snapshot ?: defaultSnapshot(userId) }
      .map { snapshot -> coerceInitialActiveMode(snapshot) }
      .stateIn(scope, SharingStarted.Eagerly, defaultSnapshot(userId))

  private val _windowSizeClass = MutableStateFlow(defaultWindowSizeClass())
  private val _undoPayload = MutableStateFlow<UndoPayload?>(null)
  private val _recentActivity = MutableStateFlow<List<RecentActivityItem>>(emptyList())
  private val commandPalette = MutableStateFlow(CommandPaletteState.Empty)

  override val commandPaletteState: Flow<CommandPaletteState> = commandPalette.asStateFlow()

  override val recentActivity: Flow<List<RecentActivityItem>> = _recentActivity.asStateFlow()

  override val windowSizeClass: Flow<WindowSizeClass> = _windowSizeClass.asStateFlow()

  override val undoPayload: Flow<UndoPayload?> = _undoPayload.asStateFlow()

  override fun updateWindowSizeClass(sizeClass: WindowSizeClass) {
    _windowSizeClass.value = sizeClass
  }

  override suspend fun openMode(modeId: ModeId) {
    val route = Screen.fromModeId(modeId).route
    withContext(ioDispatcher) {
      userProfileRepository.updateActiveModeRoute(userId, route)
      userProfileRepository.updateLeftDrawerOpen(userId, false)
      userProfileRepository.updateCommandPaletteVisibility(userId, false)
    }
    commandPalette.value = CommandPaletteState.Empty
  }

  override suspend fun toggleLeftDrawer() {
    val current = uiSnapshot.value
    setLeftDrawer(!current.isLeftDrawerOpen)
  }

  override suspend fun setLeftDrawer(open: Boolean) {
    val current = uiSnapshot.value
    if (current.isLeftDrawerOpen == open && !(open && current.isCommandPaletteVisible)) {
      return
    }
    withContext(ioDispatcher) {
      userProfileRepository.updateLeftDrawerOpen(userId, open)
      if (open && current.isCommandPaletteVisible) {
        userProfileRepository.updateCommandPaletteVisibility(userId, false)
      }
    }
  }

  override suspend fun toggleRightDrawer(
    panel: com.vjaykrsna.nanoai.feature.uiux.state.RightPanel
  ) {
    val snapshot = uiSnapshot.value
    val activePanel = snapshot.activeRightPanel.toRightPanel()
    val currentlyOpen = snapshot.isRightDrawerOpen && activePanel == panel
    val newOpen = !currentlyOpen
    val panelValue = if (newOpen) panel.toStorageValue() else null
    withContext(ioDispatcher) {
      userProfileRepository.updateRightDrawerState(userId, newOpen, panelValue)
      if (newOpen && snapshot.isCommandPaletteVisible) {
        userProfileRepository.updateCommandPaletteVisibility(userId, false)
      }
    }
  }

  override suspend fun showCommandPalette(source: PaletteSource) {
    commandPalette.value = CommandPaletteState(surfaceTarget = source.toCategory()).clearSelection()
    withContext(ioDispatcher) {
      userProfileRepository.updateLeftDrawerOpen(userId, false)
      userProfileRepository.updateCommandPaletteVisibility(userId, true)
    }
  }

  override suspend fun hideCommandPalette() {
    commandPalette.value = CommandPaletteState.Empty
    withContext(ioDispatcher) {
      userProfileRepository.updateCommandPaletteVisibility(userId, false)
    }
  }

  override suspend fun recordUndoPayload(payload: UndoPayload?) {
    _undoPayload.value = payload
  }

  @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
  private fun defaultWindowSizeClass(): WindowSizeClass =
    WindowSizeClass.calculateFromSize(DpSize(width = 640.dp, height = 360.dp))

  private fun coerceInitialActiveMode(snapshot: UIStateSnapshot): UIStateSnapshot {
    if (hasAppliedHomeStartup.compareAndSet(false, true)) {
      val resetSnapshot =
        snapshot
          .updateActiveMode(UIStateSnapshot.DEFAULT_MODE_ROUTE)
          .toggleLeftDrawer(open = false)
          .toggleRightDrawer(open = false, panelId = null)
          .updatePaletteVisibility(visible = false)

      scope.launch {
        if (
          !snapshot.activeModeRoute.equals(UIStateSnapshot.DEFAULT_MODE_ROUTE, ignoreCase = true)
        ) {
          userProfileRepository.updateActiveModeRoute(userId, Screen.fromModeId(ModeId.HOME).route)
        }
        if (snapshot.isLeftDrawerOpen) {
          userProfileRepository.updateLeftDrawerOpen(userId, false)
        }
        if (snapshot.isRightDrawerOpen || snapshot.activeRightPanel != null) {
          userProfileRepository.updateRightDrawerState(userId, false, null)
        }
        if (snapshot.isCommandPaletteVisible) {
          userProfileRepository.updateCommandPaletteVisibility(userId, false)
        }
      }

      return resetSnapshot
    }

    return snapshot
  }

  private fun String?.toRightPanel(): com.vjaykrsna.nanoai.feature.uiux.state.RightPanel? {
    val value = this ?: return null
    return RightPanel.entries.firstOrNull { panel -> panel.name.equals(value, ignoreCase = true) }
  }

  private fun com.vjaykrsna.nanoai.feature.uiux.state.RightPanel.toStorageValue(): String =
    name.lowercase()

  private fun PaletteSource.toCategory(): CommandCategory =
    when (this) {
      PaletteSource.KEYBOARD_SHORTCUT -> CommandCategory.MODES
      PaletteSource.TOP_APP_BAR -> CommandCategory.SETTINGS
      PaletteSource.QUICK_ACTION -> CommandCategory.JOBS
      PaletteSource.UNKNOWN -> CommandCategory.MODES
    }

  private fun defaultSnapshot(userId: String): UIStateSnapshot =
    UIStateSnapshot(
      userId = userId,
      expandedPanels = emptyList(),
      recentActions = emptyList(),
      isSidebarCollapsed = false,
    )
}
