@file:OptIn(
  androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi::class
)

package com.vjaykrsna.nanoai.feature.uiux.presentation

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.data.repository.NavigationRepository
import com.vjaykrsna.nanoai.feature.uiux.domain.NavigationOperationsUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** ViewModel handling navigation state and operations. */
private const val WINDOW_STATE_INDEX = 0
private const val ACTIVE_MODE_INDEX = 1
private const val LEFT_DRAWER_INDEX = 2
private const val RIGHT_DRAWER_INDEX = 3
private const val ACTIVE_PANEL_INDEX = 4
private const val UNDO_STATE_INDEX = 5
private const val COMMAND_PALETTE_INDEX = 6

class NavigationViewModel
@Inject
constructor(
  private val navigationRepository: NavigationRepository,
  private val navigationOperationsUseCase: NavigationOperationsUseCase,
) : ViewModel() {

  private val _activeMode = MutableStateFlow<ModeId>(ModeId.HOME)
  private val _leftDrawerState = MutableStateFlow<DrawerState>(DrawerState(false))
  private val _rightDrawerState = MutableStateFlow<DrawerState>(DrawerState(false))
  private val _activeRightPanel = MutableStateFlow<RightPanel?>(null)
  private val _undoState = MutableStateFlow<UndoState>(UndoState(null))

  val navigationState: StateFlow<NavigationState> =
    combine(
        navigationRepository.windowSizeClass,
        _activeMode,
        _leftDrawerState,
        _rightDrawerState,
        _activeRightPanel,
        _undoState,
        navigationRepository.commandPaletteState,
      ) { values ->
        val windowState = values[WINDOW_STATE_INDEX] as WindowSizeClass
        val activeMode = values[ACTIVE_MODE_INDEX] as ModeId
        val leftDrawerState = values[LEFT_DRAWER_INDEX] as DrawerState
        val rightDrawerState = values[RIGHT_DRAWER_INDEX] as DrawerState
        val activeRightPanel = values[ACTIVE_PANEL_INDEX] as RightPanel?
        val undoState = values[UNDO_STATE_INDEX] as UndoState
        val commandPalette = values[COMMAND_PALETTE_INDEX] as CommandPaletteState

        NavigationState(
          windowState = windowState,
          activeMode = activeMode,
          leftDrawerState = leftDrawerState,
          rightDrawerState = rightDrawerState,
          activeRightPanel = activeRightPanel,
          undoState = undoState,
          commandPalette = commandPalette,
        )
      }
      .stateIn(viewModelScope, SharingStarted.Eagerly, NavigationState.default())

  fun openMode(modeId: ModeId) {
    _activeMode.value = modeId
  }

  fun toggleLeftDrawer() {
    _leftDrawerState.value = _leftDrawerState.value.copy(isOpen = !_leftDrawerState.value.isOpen)
  }

  fun setLeftDrawer(open: Boolean) {
    _leftDrawerState.value = _leftDrawerState.value.copy(isOpen = open)
  }

  fun toggleRightDrawer(panel: RightPanel) {
    val isOpen = !_rightDrawerState.value.isOpen
    _rightDrawerState.value = DrawerState(isOpen)
    _activeRightPanel.value = if (isOpen) panel else null
  }

  fun showCommandPalette(source: PaletteSource) {
    navigationOperationsUseCase.showCommandPalette(source)
  }

  fun hideCommandPalette() {
    navigationOperationsUseCase.hideCommandPalette()
  }

  fun updateWindowSizeClass(sizeClass: WindowSizeClass) {
    navigationOperationsUseCase.updateWindowSizeClass(sizeClass)
  }
}

/** Navigation-specific state extracted from ShellLayoutState. */
data class NavigationState(
  val windowState: WindowSizeClass,
  val activeMode: ModeId,
  val leftDrawerState: DrawerState,
  val rightDrawerState: DrawerState,
  val activeRightPanel: RightPanel?,
  val undoState: UndoState,
  val commandPalette: CommandPaletteState,
) {
  companion object {
    fun default(): NavigationState =
      NavigationState(
        windowState =
          WindowSizeClass.calculateFromSize(
            androidx.compose.ui.unit.DpSize(width = 640.dp, height = 360.dp)
          ),
        activeMode = ModeId.HOME,
        leftDrawerState = DrawerState(false),
        rightDrawerState = DrawerState(false),
        activeRightPanel = null,
        undoState = UndoState(null),
        commandPalette = CommandPaletteState.Empty,
      )
  }
}

/** Simple data class for drawer state. */
data class DrawerState(val isOpen: Boolean)

/** Simple data class for undo state. */
data class UndoState(val payload: UndoPayload?)
