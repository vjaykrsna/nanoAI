package com.vjaykrsna.nanoai.feature.uiux.ui.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
import com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
import com.vjaykrsna.nanoai.feature.uiux.presentation.CommandAction
import com.vjaykrsna.nanoai.feature.uiux.presentation.CommandDestination
import com.vjaykrsna.nanoai.feature.uiux.presentation.CommandInvocationSource
import com.vjaykrsna.nanoai.feature.uiux.presentation.ConnectivityStatus
import com.vjaykrsna.nanoai.feature.uiux.presentation.ModeId
import com.vjaykrsna.nanoai.feature.uiux.presentation.PaletteDismissReason
import com.vjaykrsna.nanoai.feature.uiux.presentation.PaletteSource
import com.vjaykrsna.nanoai.feature.uiux.presentation.ProgressJob
import com.vjaykrsna.nanoai.feature.uiux.presentation.RightPanel
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellUiState
import com.vjaykrsna.nanoai.feature.uiux.presentation.UndoPayload
import com.vjaykrsna.nanoai.feature.uiux.presentation.toModeIdOrNull
import com.vjaykrsna.nanoai.feature.uiux.ui.commandpalette.CommandPaletteSheet
import java.util.UUID

/** Root scaffold Compose entry point for the unified shell experience. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun NanoShellScaffold(
  state: ShellUiState,
  onEvent: (ShellUiEvent) -> Unit,
  modifier: Modifier = Modifier,
  modeContent: @Composable (ModeId) -> Unit = {},
) {
  /**
   * Architecture Overview: NanoShellScaffold is the root container for the unified shell
   * experience. It orchestrates:
   * 1. Left Navigation Drawer (modal or permanent based on layout)
   * 2. Right Panels (model selector, progress, etc.)
   * 3. Command Palette overlay
   * 4. Responsive layout adapting to window size class
   *
   * The main composable is focused on:
   * - State management and synchronization with Material3 drawer
   * - Keyboard shortcuts handling
   * - Event dispatch orchestration
   * - Conditional UI rendering based on layout
   *
   * Child composables handle specific UI sections:
   * - ShellDrawerContent: Navigation drawer UI
   * - ShellRightRailHost: Right panels and main content area
   * - CommandPaletteSheet: Command palette overlay
   */
  val layout = state.layout
  val snackbarHostState = remember { SnackbarHostState() }
  val focusRequester = remember { FocusRequester() }
  val drawerState =
    rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
  val currentOnEvent by rememberUpdatedState(newValue = onEvent)
  val latestLeftDrawerOpen by rememberUpdatedState(layout.isLeftDrawerOpen)

  fun closeLeftDrawerIfOpen() {
    if (!layout.canToggleLeftDrawer) return
    if (layout.isLeftDrawerOpen) {
      currentOnEvent(ShellUiEvent.ToggleLeftDrawer)
    }
  }

  fun closeRightDrawerIfOpen() {
    if (layout.isRightDrawerOpen) {
      val panel = layout.activeRightPanel ?: RightPanel.MODEL_SELECTOR
      currentOnEvent(ShellUiEvent.ToggleRightDrawer(panel))
    }
  }

  fun toggleLeftDrawerWithRules() {
    if (!layout.canToggleLeftDrawer) return
    if (layout.isRightDrawerOpen) {
      closeRightDrawerIfOpen()
    }
    currentOnEvent(ShellUiEvent.ToggleLeftDrawer)
  }

  fun toggleRightDrawerWithRules(panel: RightPanel) {
    if (layout.isLeftDrawerOpen) {
      closeLeftDrawerIfOpen()
    }
    currentOnEvent(ShellUiEvent.ToggleRightDrawer(panel))
  }

  val dispatchEvent: (ShellUiEvent) -> Unit = { event ->
    when (event) {
      ShellUiEvent.ToggleLeftDrawer -> toggleLeftDrawerWithRules()
      is ShellUiEvent.ToggleRightDrawer -> toggleRightDrawerWithRules(event.panel)
      is ShellUiEvent.ShowCommandPalette -> {
        closeLeftDrawerIfOpen()
        closeRightDrawerIfOpen()
        currentOnEvent(event)
      }
      is ShellUiEvent.ModeSelected -> {
        closeLeftDrawerIfOpen()
        closeRightDrawerIfOpen()
        currentOnEvent(event)
      }
      else -> currentOnEvent(event)
    }
  }

  LaunchedEffect(layout.useModalNavigation, layout.isLeftDrawerOpen) {
    if (!layout.useModalNavigation) {
      drawerState.close()
      return@LaunchedEffect
    }
    if (layout.isLeftDrawerOpen) {
      drawerState.open()
    } else {
      drawerState.close()
    }
  }

  LaunchedEffect(drawerState, layout.useModalNavigation) {
    if (!layout.useModalNavigation) return@LaunchedEffect
    snapshotFlow { drawerState.currentValue }
      .collect { value ->
        val isOpen = value == androidx.compose.material3.DrawerValue.Open
        if (isOpen != latestLeftDrawerOpen) {
          currentOnEvent(ShellUiEvent.SetLeftDrawer(isOpen))
        }
      }
  }

  LaunchedEffect(layout.pendingUndoAction) {
    val payload = layout.pendingUndoAction ?: return@LaunchedEffect
    val message = payload.metadata["message"] as? String ?: "Action completed"
    val result = snackbarHostState.showSnackbar(message = message, actionLabel = "Undo")
    if (result == SnackbarResult.ActionPerformed) {
      currentOnEvent(ShellUiEvent.Undo(payload))
    }
  }

  LaunchedEffect(Unit) { focusRequester.requestFocus() }
  LaunchedEffect(layout.isPaletteVisible) {
    if (!layout.isPaletteVisible) {
      focusRequester.requestFocus()
    }
  }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .let {
          val thresholds = rememberShellDrawerThresholds()
          it.shellDrawerGestures(layout, thresholds, dispatchEvent)
        }
        .focusRequester(focusRequester)
        .onPreviewKeyEvent { event ->
          handleShellShortcuts(event, layout.isPaletteVisible, dispatchEvent)
        }
        .testTag("shell_root")
  ) {
    if (layout.usesPermanentLeftDrawer) {
      PermanentNavigationDrawer(
        drawerContent = {
          ShellDrawerContent(
            variant = DrawerVariant.Permanent,
            activeMode = layout.activeMode,
            onEvent = { drawerEvent ->
              when (drawerEvent) {
                is ShellDrawerEvent.ModeSelected ->
                  dispatchEvent(ShellUiEvent.ModeSelected(drawerEvent.modeId))
                is ShellDrawerEvent.ShowCommandPalette ->
                  dispatchEvent(ShellUiEvent.ShowCommandPalette(drawerEvent.source))
                ShellDrawerEvent.CloseDrawer -> closeLeftDrawerIfOpen()
              }
            },
          )
        },
        modifier = Modifier.testTag("left_drawer_permanent"),
      ) {
        ShellRightRailHost(
          state = state,
          snackbarHostState = snackbarHostState,
          onEvent = dispatchEvent,
          modeContent = modeContent,
          originalOnEvent = onEvent,
        )
      }
    } else {
      ModalNavigationDrawer(
        drawerContent = {
          ShellDrawerContent(
            variant = DrawerVariant.Modal,
            activeMode = layout.activeMode,
            onEvent = { drawerEvent ->
              when (drawerEvent) {
                is ShellDrawerEvent.ModeSelected ->
                  dispatchEvent(ShellUiEvent.ModeSelected(drawerEvent.modeId))
                is ShellDrawerEvent.ShowCommandPalette ->
                  dispatchEvent(ShellUiEvent.ShowCommandPalette(drawerEvent.source))
                ShellDrawerEvent.CloseDrawer -> closeLeftDrawerIfOpen()
              }
            },
          )
        },
        drawerState = drawerState,
        gesturesEnabled = layout.useModalNavigation && !layout.isRightDrawerOpen,
        modifier = Modifier.testTag("left_drawer_modal"),
      ) {
        ShellRightRailHost(
          state = state,
          snackbarHostState = snackbarHostState,
          onEvent = dispatchEvent,
          modeContent = modeContent,
          originalOnEvent = onEvent,
        )
      }
    }

    AnimatedVisibility(
      visible = layout.isPaletteVisible,
      enter = fadeIn(animationSpec = tween(durationMillis = 120, easing = FastOutLinearInEasing)),
      exit = fadeOut(animationSpec = tween(durationMillis = 100, easing = LinearOutSlowInEasing)),
    ) {
      CommandPaletteSheet(
        state = state.commandPalette,
        onDismissRequest = { reason -> dispatchEvent(ShellUiEvent.HideCommandPalette(reason)) },
        onCommandSelect = { action ->
          handleCommandAction(action, CommandInvocationSource.PALETTE, dispatchEvent)
        },
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}

/** Events emitted by [NanoShellScaffold] to interact with view models. */
sealed interface ShellUiEvent {
  data class ModeSelected(val modeId: ModeId) : ShellUiEvent

  data object ToggleLeftDrawer : ShellUiEvent

  data class SetLeftDrawer(val open: Boolean) : ShellUiEvent

  data class ToggleRightDrawer(val panel: RightPanel) : ShellUiEvent

  data class ShowCommandPalette(val source: PaletteSource) : ShellUiEvent

  data class HideCommandPalette(val reason: PaletteDismissReason) : ShellUiEvent

  data class CommandInvoked(val action: CommandAction, val source: CommandInvocationSource) :
    ShellUiEvent

  data class QueueJob(val job: ProgressJob) : ShellUiEvent

  data class RetryJob(val job: ProgressJob) : ShellUiEvent

  data class CompleteJob(val jobId: UUID) : ShellUiEvent

  data class Undo(val payload: UndoPayload) : ShellUiEvent

  data class ConnectivityChanged(val status: ConnectivityStatus) : ShellUiEvent

  data class UpdateTheme(val theme: ThemePreference) : ShellUiEvent

  data class UpdateDensity(val density: VisualDensity) : ShellUiEvent

  data class ChatPersonaSelected(
    val personaId: java.util.UUID,
    val action: com.vjaykrsna.nanoai.core.model.PersonaSwitchAction,
  ) : ShellUiEvent

  data object ChatTitleClicked : ShellUiEvent
}

internal enum class DrawerVariant {
  Modal,
  Permanent,
}

internal fun handleCommandAction(
  action: CommandAction,
  source: CommandInvocationSource,
  onEvent: (ShellUiEvent) -> Unit,
) {
  onEvent(ShellUiEvent.CommandInvoked(action, source))
  when (val destination = action.destination) {
    is CommandDestination.Navigate -> {
      val modeId = routeToMode(destination.route)
      if (modeId != null) {
        onEvent(ShellUiEvent.ModeSelected(modeId))
      }
    }
    is CommandDestination.OpenRightPanel ->
      onEvent(ShellUiEvent.ToggleRightDrawer(destination.panel))
    CommandDestination.None -> Unit
  }
}

private fun routeToMode(route: String): ModeId? = route.substringBefore('/').toModeIdOrNull()

private fun handleShellShortcuts(
  event: KeyEvent,
  paletteVisible: Boolean,
  onEvent: (ShellUiEvent) -> Unit,
): Boolean {
  if (event.type != KeyEventType.KeyDown) return false
  return when {
    event.key == Key.K && event.isCtrlPressed -> {
      onEvent(ShellUiEvent.ShowCommandPalette(PaletteSource.KEYBOARD_SHORTCUT))
      true
    }
    event.key == Key.Escape && paletteVisible -> {
      onEvent(ShellUiEvent.HideCommandPalette(PaletteDismissReason.BACK_PRESSED))
      true
    }
    else -> false
  }
}
