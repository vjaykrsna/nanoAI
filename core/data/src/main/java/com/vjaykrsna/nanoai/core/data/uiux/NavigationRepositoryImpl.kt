package com.vjaykrsna.nanoai.core.data.uiux

import com.vjaykrsna.nanoai.core.common.IoDispatcher
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandCategory
import com.vjaykrsna.nanoai.core.domain.model.uiux.CommandPaletteState
import com.vjaykrsna.nanoai.core.domain.model.uiux.ModeId
import com.vjaykrsna.nanoai.core.domain.model.uiux.PaletteSource
import com.vjaykrsna.nanoai.core.domain.model.uiux.RecentActivityItem
import com.vjaykrsna.nanoai.core.domain.model.uiux.RightPanel
import com.vjaykrsna.nanoai.core.domain.model.uiux.ShellWindowSizeClass
import com.vjaykrsna.nanoai.core.domain.model.uiux.UIStateSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.UndoPayload
import com.vjaykrsna.nanoai.core.domain.repository.NavigationRepository
import com.vjaykrsna.nanoai.core.domain.uiux.UIUX_DEFAULT_USER_ID
import com.vjaykrsna.nanoai.core.domain.uiux.navigation.Screen
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class NavigationRepositoryImpl
@Inject
constructor(@IoDispatcher override val ioDispatcher: CoroutineDispatcher) : NavigationRepository {

  private val userId: String = UIUX_DEFAULT_USER_ID

  private val _uiSnapshot = MutableStateFlow(defaultSnapshot(userId))
  private val uiSnapshot: StateFlow<UIStateSnapshot> = _uiSnapshot.asStateFlow()

  private val _windowSizeClass = MutableStateFlow(defaultWindowSizeClass())
  private val _undoPayload = MutableStateFlow<UndoPayload?>(null)
  private val _recentActivity = MutableStateFlow<List<RecentActivityItem>>(emptyList())
  private val commandPalette = MutableStateFlow(CommandPaletteState.Empty)

  override val commandPaletteState: Flow<CommandPaletteState> = commandPalette.asStateFlow()

  override val recentActivity: Flow<List<RecentActivityItem>> = _recentActivity.asStateFlow()

  override val windowSizeClass: Flow<ShellWindowSizeClass> = _windowSizeClass.asStateFlow()

  override val undoPayload: Flow<UndoPayload?> = _undoPayload.asStateFlow()

  override fun updateWindowSizeClass(sizeClass: ShellWindowSizeClass) {
    _windowSizeClass.value = sizeClass
  }

  override suspend fun openMode(modeId: ModeId) {
    val route = Screen.fromModeId(modeId).route
    _uiSnapshot.value =
      uiSnapshot.value
        .updateActiveMode(route)
        .toggleLeftDrawer(false)
        .updatePaletteVisibility(false)
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
    _uiSnapshot.value =
      current.toggleLeftDrawer(open).let { updatedSnapshot ->
        if (open && updatedSnapshot.isCommandPaletteVisible) {
          updatedSnapshot.updatePaletteVisibility(false)
        } else {
          updatedSnapshot
        }
      }
  }

  override suspend fun toggleRightDrawer(
    panel: com.vjaykrsna.nanoai.core.domain.model.uiux.RightPanel
  ) {
    val snapshot = uiSnapshot.value
    val activePanel = snapshot.activeRightPanel.toRightPanel()
    val currentlyOpen = snapshot.isRightDrawerOpen && activePanel == panel
    val newOpen = !currentlyOpen
    val panelValue = if (newOpen) panel.toStorageValue() else null
    _uiSnapshot.value =
      snapshot.toggleRightDrawer(newOpen, panelValue).let { updatedSnapshot ->
        if (newOpen && updatedSnapshot.isCommandPaletteVisible) {
          updatedSnapshot.updatePaletteVisibility(false)
        } else {
          updatedSnapshot
        }
      }
  }

  override suspend fun showCommandPalette(source: PaletteSource) {
    commandPalette.value = CommandPaletteState(surfaceTarget = source.toCategory()).clearSelection()
    _uiSnapshot.value = uiSnapshot.value.toggleLeftDrawer(false).updatePaletteVisibility(true)
  }

  override suspend fun hideCommandPalette() {
    commandPalette.value = CommandPaletteState.Empty
    _uiSnapshot.value = uiSnapshot.value.updatePaletteVisibility(false)
  }

  override suspend fun recordUndoPayload(payload: UndoPayload?) {
    _undoPayload.value = payload
  }

  private fun defaultWindowSizeClass(): ShellWindowSizeClass = ShellWindowSizeClass.Default

  private fun String?.toRightPanel(): com.vjaykrsna.nanoai.core.domain.model.uiux.RightPanel? {
    val value = this ?: return null
    return RightPanel.entries.firstOrNull { panel -> panel.name.equals(value, ignoreCase = true) }
  }

  private fun com.vjaykrsna.nanoai.core.domain.model.uiux.RightPanel.toStorageValue(): String =
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
