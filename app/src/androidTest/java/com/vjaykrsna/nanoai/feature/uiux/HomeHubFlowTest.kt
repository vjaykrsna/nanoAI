@file:Suppress("LongMethod")

package com.vjaykrsna.nanoai.feature.uiux

import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.feature.uiux.presentation.CommandAction
import com.vjaykrsna.nanoai.feature.uiux.presentation.CommandCategory
import com.vjaykrsna.nanoai.feature.uiux.presentation.CommandDestination
import com.vjaykrsna.nanoai.feature.uiux.presentation.CommandPaletteState
import com.vjaykrsna.nanoai.feature.uiux.presentation.ConnectivityBannerState
import com.vjaykrsna.nanoai.feature.uiux.presentation.ConnectivityStatus
import com.vjaykrsna.nanoai.feature.uiux.presentation.ModeCard
import com.vjaykrsna.nanoai.feature.uiux.presentation.ModeId
import com.vjaykrsna.nanoai.feature.uiux.presentation.RecentActivityItem
import com.vjaykrsna.nanoai.feature.uiux.presentation.RecentStatus
import com.vjaykrsna.nanoai.feature.uiux.presentation.RightPanel
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellLayoutState
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellUiState
import com.vjaykrsna.nanoai.feature.uiux.presentation.UiPreferenceSnapshot
import com.vjaykrsna.nanoai.feature.uiux.presentation.UndoPayload
import com.vjaykrsna.nanoai.feature.uiux.ui.shell.NanoShellScaffold
import com.vjaykrsna.nanoai.feature.uiux.ui.shell.ShellUiEvent
import com.vjaykrsna.nanoai.shared.ui.theme.NanoAITheme
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
class HomeHubFlowTest {
  @org.junit.jupiter.api.extension.RegisterExtension
  @JvmField
  val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun modeCards_renderAndTriggerModeSelection() {
    val events = mutableListOf<ShellUiEvent>()
    val state = mutableStateOf(sampleState())

    composeRule.setContent {
      NanoAITheme { NanoShellScaffold(state = state.value, onEvent = { event -> events += event }) }
    }

    composeRule.waitForIdle()

    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule
        .onAllNodesWithTag("mode_card", useUnmergedTree = true)
        .fetchSemanticsNodes(false)
        .size >= 3
    }
    val modeCards = composeRule.onAllNodesWithTag("mode_card", useUnmergedTree = true)
    modeCards.assertCountEquals(3)
    modeCards[0].performClick()

    composeRule.waitUntil {
      events.any { it is ShellUiEvent.ModeSelected && it.modeId == ModeId.CHAT }
    }

    assertThat(events.filterIsInstance<ShellUiEvent.ModeSelected>().first().modeId)
      .isEqualTo(ModeId.CHAT)
  }

  @Test
  fun recentActivity_showsLatestEntries() {
    val state = mutableStateOf(sampleState())
    composeRule.setContent { NanoAITheme { NanoShellScaffold(state = state.value, onEvent = {}) } }

    composeRule.waitForIdle()

    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule
        .onAllNodesWithTagPrefix("recent_activity_item", useUnmergedTree = true)
        .fetchSemanticsNodes(false)
        .size == 2
    }

    composeRule.onNodeWithTag("recent_activity_list", useUnmergedTree = true).assertIsDisplayed()
    val items = composeRule.onAllNodesWithTagPrefix("recent_activity_item", useUnmergedTree = true)
    items.assertCountEquals(2)
    items[0].assertIsDisplayed()
  }

  @Test
  fun quickActions_sectionVisible() {
    val events = mutableListOf<ShellUiEvent>()
    val state = mutableStateOf(sampleState())
    composeRule.setContent {
      NanoAITheme { NanoShellScaffold(state = state.value, onEvent = { events += it }) }
    }

    composeRule.waitForIdle()

    composeRule
      .onNodeWithTag("home_tools_toggle", useUnmergedTree = true)
      .assertIsDisplayed()
      .performClick()

    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule
        .onAllNodesWithTag("quick_actions_row", useUnmergedTree = true)
        .fetchSemanticsNodes(false)
        .isNotEmpty()
    }

    composeRule.onNodeWithTag("quick_actions_row", useUnmergedTree = true).assertIsDisplayed()
    composeRule.onNodeWithContentDescription("New Chat").assertIsDisplayed().performClick()

    composeRule.waitUntil {
      events.any { it is ShellUiEvent.CommandInvoked } &&
        events.any { it is ShellUiEvent.ModeSelected && it.modeId == ModeId.CHAT }
    }

    assertThat(events.filterIsInstance<ShellUiEvent.CommandInvoked>().first().action.title)
      .isEqualTo("New Chat")
  }

  private fun sampleState(): ShellUiState {
    val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(720.dp, 1024.dp))
    val modeCards =
      listOf(
        modeCard(ModeId.CHAT, "Chat", Icons.AutoMirrored.Filled.Chat),
        modeCard(ModeId.IMAGE, "Image", Icons.Filled.Image),
        modeCard(ModeId.AUDIO, "Audio", Icons.Filled.Mic),
      )
    val recent =
      listOf(
        RecentActivityItem(
          id = "r1",
          modeId = ModeId.CHAT,
          title = "Prompt brainstorm",
          timestamp = Instant.now().minusSeconds(60),
          status = RecentStatus.COMPLETED,
        ),
        RecentActivityItem(
          id = "r2",
          modeId = ModeId.IMAGE,
          title = "Moodboard",
          timestamp = Instant.now().minusSeconds(120),
          status = RecentStatus.IN_PROGRESS,
        ),
      )
    val layout =
      ShellLayoutState(
        windowSizeClass = windowSizeClass,
        isLeftDrawerOpen = false,
        isRightDrawerOpen = false,
        activeRightPanel = RightPanel.MODEL_SELECTOR,
        activeMode = ModeId.HOME,
        showCommandPalette = false,
        connectivity = ConnectivityStatus.ONLINE,
        pendingUndoAction = UndoPayload(actionId = "none"),
        progressJobs = emptyList(),
        recentActivity = recent,
      )
    val palette =
      CommandPaletteState(
        query = "",
        results = emptyList(),
        recentCommands = emptyList(),
        selectedIndex = 0,
        surfaceTarget = CommandCategory.MODES,
      )
    val banner =
      ConnectivityBannerState(
        status = ConnectivityStatus.ONLINE,
        queuedActionCount = 0,
        cta = CommandAction("view-queue", "View queue", category = CommandCategory.JOBS),
      )

    return ShellUiState(
      layout = layout,
      commandPalette = palette,
      connectivityBanner = banner,
      preferences = UiPreferenceSnapshot(),
      modeCards = modeCards,
      quickActions = modeCards.map(ModeCard::primaryAction),
    )
  }

  private fun modeCard(id: ModeId, title: String, icon: ImageVector): ModeCard =
    ModeCard(
      id = id,
      title = title,
      icon = icon,
      primaryAction =
        CommandAction(
          id = "quick-$title",
          title = "New $title",
          category = CommandCategory.MODES,
          destination = CommandDestination.Navigate(id.name.lowercase()),
        ),
      enabled = true,
      badge = null,
    )

  private fun ComposeContentTestRule.onAllNodesWithTagPrefix(
    prefix: String,
    useUnmergedTree: Boolean = false,
  ): SemanticsNodeInteractionCollection =
    onAllNodes(
      SemanticsMatcher("Has test tag with prefix $prefix") { node ->
        val tag = node.config.getOrNull(SemanticsProperties.TestTag)
        tag?.startsWith(prefix) == true
      },
      useUnmergedTree = useUnmergedTree,
    )
}
