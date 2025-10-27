package com.vjaykrsna.nanoai.feature.uiux

import androidx.activity.ComponentActivity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.feature.uiux.presentation.CommandAction
import com.vjaykrsna.nanoai.feature.uiux.presentation.CommandCategory
import com.vjaykrsna.nanoai.feature.uiux.presentation.CommandDestination
import com.vjaykrsna.nanoai.feature.uiux.presentation.CommandPaletteState
import com.vjaykrsna.nanoai.feature.uiux.presentation.ConnectivityBannerState
import com.vjaykrsna.nanoai.feature.uiux.presentation.ConnectivityStatus
import com.vjaykrsna.nanoai.feature.uiux.presentation.ModeId
import com.vjaykrsna.nanoai.feature.uiux.presentation.RightPanel
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellLayoutState
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellUiState
import com.vjaykrsna.nanoai.feature.uiux.presentation.UiPreferenceSnapshot
import com.vjaykrsna.nanoai.feature.uiux.presentation.UndoPayload
import com.vjaykrsna.nanoai.feature.uiux.ui.shell.NanoShellScaffold
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
class AdaptiveShellTest {
  @org.junit.jupiter.api.extension.RegisterExtension
  @JvmField
  val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun compactWidth_usesModalDrawer() {
    val state =
      mutableStateOf(sampleState(WindowWidthSizeClass.Compact, WindowHeightSizeClass.Expanded))
    composeRule.setContent { NanoShellScaffold(state = state.value, onEvent = {}) }

    composeRule.onNodeWithTag("left_drawer_modal").assertIsDisplayed()
  }

  @Test
  fun expandedWidth_showsPermanentDrawer() {
    val state =
      mutableStateOf(sampleState(WindowWidthSizeClass.Expanded, WindowHeightSizeClass.Medium))
    composeRule.setContent { NanoShellScaffold(state = state.value, onEvent = {}) }

    composeRule.onNodeWithTag("left_drawer_permanent").assertIsDisplayed()
    composeRule.onAllNodesWithTag("left_drawer_modal").assertCountEquals(0)
  }

  @Test
  fun accessibility_focusMovesIntoContent() {
    val state =
      mutableStateOf(sampleState(WindowWidthSizeClass.Medium, WindowHeightSizeClass.Medium))
    composeRule.setContent { NanoShellScaffold(state = state.value, onEvent = {}) }

    composeRule.waitForIdle()

    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodesWithTag("shell_content").fetchSemanticsNodes().isNotEmpty()
    }

    val content = composeRule.onNodeWithTag("shell_content")
    content.assertExists()
    content.performSemanticsAction(SemanticsActions.RequestFocus)
    content.assertIsFocused()
  }

  private fun sampleState(
    width: WindowWidthSizeClass,
    height: WindowHeightSizeClass,
  ): ShellUiState {
    val windowSizeClass = fakeWindowSizeClass(width, height)
    val layout =
      ShellLayoutState(
        windowSizeClass = windowSizeClass,
        isLeftDrawerOpen = width == WindowWidthSizeClass.Compact,
        isRightDrawerOpen = false,
        activeRightPanel =
          if (width >= WindowWidthSizeClass.Medium) RightPanel.MODEL_SELECTOR else null,
        activeMode = ModeId.HOME,
        showCommandPalette = false,
        connectivity = ConnectivityStatus.ONLINE,
        pendingUndoAction = UndoPayload(actionId = "none"),
        progressJobs = emptyList(),
        recentActivity = emptyList(),
      )
    val palette = CommandPaletteState()
    val banner =
      ConnectivityBannerState(
        status = ConnectivityStatus.ONLINE,
        queuedActionCount = 0,
        cta =
          CommandAction(
            "view-queue",
            "View queue",
            category = CommandCategory.JOBS,
            destination = CommandDestination.OpenRightPanel(RightPanel.MODEL_SELECTOR),
          ),
      )

    return ShellUiState(
      layout = layout,
      commandPalette = palette,
      connectivityBanner = banner,
      preferences = UiPreferenceSnapshot(),
    )
  }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
private fun fakeWindowSizeClass(
  width: WindowWidthSizeClass,
  height: WindowHeightSizeClass,
): WindowSizeClass {
  val widthDp =
    when (width) {
      WindowWidthSizeClass.Compact -> 480.dp
      WindowWidthSizeClass.Medium -> 720.dp
      WindowWidthSizeClass.Expanded -> 960.dp
      else -> 720.dp
    }
  val heightDp =
    when (height) {
      WindowHeightSizeClass.Compact -> 400.dp
      WindowHeightSizeClass.Medium -> 720.dp
      WindowHeightSizeClass.Expanded -> 960.dp
      else -> 720.dp
    }
  return WindowSizeClass.calculateFromSize(DpSize(widthDp, heightDp))
}
