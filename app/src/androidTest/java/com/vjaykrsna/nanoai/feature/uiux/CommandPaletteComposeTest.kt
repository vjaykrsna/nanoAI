@file:Suppress("LongMethod", "CyclomaticComplexMethod")

package com.vjaykrsna.nanoai.feature.uiux

import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelectable
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.feature.uiux.presentation.CommandAction
import com.vjaykrsna.nanoai.feature.uiux.presentation.CommandCategory
import com.vjaykrsna.nanoai.feature.uiux.presentation.CommandDestination
import com.vjaykrsna.nanoai.feature.uiux.presentation.CommandPaletteState
import com.vjaykrsna.nanoai.feature.uiux.presentation.ConnectivityBannerState
import com.vjaykrsna.nanoai.feature.uiux.presentation.ConnectivityStatus
import com.vjaykrsna.nanoai.feature.uiux.presentation.JobStatus
import com.vjaykrsna.nanoai.feature.uiux.presentation.JobType
import com.vjaykrsna.nanoai.feature.uiux.presentation.ModeCard
import com.vjaykrsna.nanoai.feature.uiux.presentation.ModeId
import com.vjaykrsna.nanoai.feature.uiux.presentation.PaletteSource
import com.vjaykrsna.nanoai.feature.uiux.presentation.ProgressJob
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
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test

@OptIn(
  ExperimentalCoroutinesApi::class,
  ExperimentalTestApi::class,
  ExperimentalMaterial3WindowSizeClassApi::class,
)
class CommandPaletteComposeTest {
  @org.junit.jupiter.api.extension.RegisterExtension
  @JvmField
  val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun palette_opensWithShortcut_focusesSearchField() {
    val recorder = EventRecorder()
    val state = mutableStateOf(sampleState(showPalette = false))

    composeTestRule.setContent {
      NanoAITheme {
        NanoShellScaffold(
          state = state.value,
          onEvent = { intent ->
            recorder.record(intent)
            handleIntent(state, intent)
          },
        )
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.runOnIdle {
      val shortcutEvent = ShellUiEvent.ShowCommandPalette(PaletteSource.KEYBOARD_SHORTCUT)
      recorder.record(shortcutEvent)
      handleIntent(state, shortcutEvent)
    }

    composeTestRule.onNodeWithTag("command_palette").assertIsDisplayed()
    composeTestRule.onNodeWithTag("command_palette_search").assertIsDisplayed().assertIsFocused()
    assertThat(recorder.events)
      .contains(ShellUiEvent.ShowCommandPalette(PaletteSource.KEYBOARD_SHORTCUT))
  }

  @Test
  fun palette_filtersActions_prioritizesMatches() {
    val state =
      mutableStateOf(
        sampleState(
          showPalette = true,
          commandPalette = samplePaletteState().copy(results = sampleCommands()),
        )
      )

    composeTestRule.setContent {
      NanoAITheme {
        NanoShellScaffold(state = state.value, onEvent = { intent -> handleIntent(state, intent) })
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("command_palette_search").performTextInput("ima")
    composeTestRule.onAllNodesWithTag("command_palette_item")[0].assertIsDisplayed()
  }

  @Test
  fun palette_keyboardNavigation_wrapsSelection() {
    val state =
      mutableStateOf(
        sampleState(
          showPalette = true,
          commandPalette = samplePaletteState().copy(results = sampleCommands()),
        )
      )

    composeTestRule.setContent {
      NanoAITheme {
        NanoShellScaffold(state = state.value, onEvent = { intent -> handleIntent(state, intent) })
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("command_palette_list").performKeyInput {
      pressKey(Key.DirectionDown)
      pressKey(Key.DirectionDown)
      pressKey(Key.DirectionDown)
    }

    composeTestRule.onAllNodesWithTag("command_palette_item")[0].assertIsSelectable()
  }

  @Test
  fun palette_executesAction_andCloses() {
    val recorder = EventRecorder()
    val state =
      mutableStateOf(
        sampleState(
          showPalette = true,
          commandPalette = samplePaletteState().copy(results = sampleCommands()),
        )
      )

    composeTestRule.setContent {
      NanoAITheme {
        NanoShellScaffold(
          state = state.value,
          onEvent = { intent ->
            recorder.record(intent)
            handleIntent(state, intent)
          },
        )
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.onAllNodesWithTag("command_palette_item")[0].performClick()
    composeTestRule.onAllNodesWithTag("command_palette").assertCountEquals(0)
    assertThat(recorder.events.filterIsInstance<ShellUiEvent.ModeSelected>()).isNotEmpty()
  }

  @Test
  fun palette_disabledMode_showsErrorState() {
    val disabledCommand = sampleCommands().last().copy(enabled = false)
    val state =
      mutableStateOf(
        sampleState(
          showPalette = true,
          commandPalette = samplePaletteState().copy(results = listOf(disabledCommand)),
        )
      )

    composeTestRule.setContent {
      NanoAITheme {
        NanoShellScaffold(state = state.value, onEvent = { intent -> handleIntent(state, intent) })
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("command_palette_item").assertIsNotEnabled()
    composeTestRule.onNodeWithContentDescription("Unavailable offline").assertIsDisplayed()
  }

  @Test
  fun progressRetryButton_reflectsRetryAvailability() {
    val retryable = sampleProgressJob(canRetry = true, type = JobType.MODEL_DOWNLOAD)
    val nonRetryable = sampleProgressJob(canRetry = false, type = JobType.IMAGE_GENERATION)
    val recorder = EventRecorder()
    val state =
      mutableStateOf(
        sampleState(showPalette = false, progressJobs = listOf(retryable, nonRetryable))
      )

    composeTestRule.setContent {
      NanoAITheme {
        NanoShellScaffold(
          state = state.value,
          onEvent = { intent ->
            recorder.record(intent)
            handleIntent(state, intent)
          },
        )
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.runOnIdle { assertThat(state.value.layout.progressJobs).hasSize(2) }

    composeTestRule.waitForProgressItems(prefix = "progress_list_item_", count = 2)

    val retryAvailableButton =
      composeTestRule.onNodeWithTag(
        "progress_retry_button_${retryable.jobId}",
        useUnmergedTree = true,
      )
    retryAvailableButton.assertIsEnabled()
    retryAvailableButton.assert(
      SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Retry available")
    )
    retryAvailableButton.performClick()

    val retryUnavailableButton =
      composeTestRule.onNodeWithTag(
        "progress_retry_button_${nonRetryable.jobId}",
        useUnmergedTree = true,
      )
    retryUnavailableButton.assertIsNotEnabled()
    retryUnavailableButton.assert(
      SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Retry unavailable")
    )

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      recorder.events.any { it is ShellUiEvent.RetryJob && it.job.jobId == retryable.jobId }
    }
  }

  @Test
  fun snackbar_displaysUndoAction_andDispatchesEvent() {
    val payload =
      UndoPayload(
        actionId = "queue-${UUID.randomUUID()}",
        metadata = mapOf("message" to "Image generation queued for reconnect"),
      )
    val recorder = EventRecorder()
    val state = mutableStateOf(sampleState(showPalette = false, pendingUndo = payload))

    composeTestRule.setContent {
      NanoAITheme {
        NanoShellScaffold(
          state = state.value,
          onEvent = { intent ->
            recorder.record(intent)
            handleIntent(state, intent)
          },
        )
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
        .onAllNodesWithText("Image generation queued for reconnect", substring = false)
        .fetchSemanticsNodes(false)
        .isNotEmpty()
    }

    composeTestRule.onNode(hasText("Image generation queued for reconnect")).assertExists()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
        .onAllNodes(hasClickAction() and hasAnyChild(hasText("Undo", ignoreCase = true)))
        .fetchSemanticsNodes(false)
        .isNotEmpty()
    }

    composeTestRule
      .onNode(hasClickAction() and hasAnyChild(hasText("Undo", ignoreCase = true)))
      .performClick()

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      recorder.events.any { it is ShellUiEvent.Undo && it.payload == payload }
    }
  }
}

private fun handleIntent(state: MutableState<ShellUiState>, intent: ShellUiEvent) {
  val current = state.value
  when (intent) {
    is ShellUiEvent.ShowCommandPalette ->
      state.value =
        current.copy(
          layout = current.layout.copy(showCommandPalette = true, isLeftDrawerOpen = false)
        )
    is ShellUiEvent.HideCommandPalette ->
      state.value = current.copy(layout = current.layout.copy(showCommandPalette = false))
    is ShellUiEvent.ModeSelected ->
      state.value =
        current.copy(
          layout =
            current.layout.copy(
              activeMode = intent.modeId,
              showCommandPalette = false,
              isLeftDrawerOpen = false,
            )
        )
    is ShellUiEvent.ToggleRightDrawer ->
      state.value =
        current.copy(
          layout =
            current.layout.copy(
              isRightDrawerOpen = !current.layout.isRightDrawerOpen,
              activeRightPanel = intent.panel,
            )
        )
    is ShellUiEvent.ToggleLeftDrawer ->
      state.value =
        current.copy(
          layout =
            current.layout.copy(
              isLeftDrawerOpen = !current.layout.isLeftDrawerOpen,
              showCommandPalette = false,
            )
        )
    is ShellUiEvent.QueueJob ->
      state.value =
        current.copy(
          layout = current.layout.copy(progressJobs = current.layout.progressJobs + intent.job)
        )
    is ShellUiEvent.CompleteJob ->
      state.value =
        current.copy(
          layout =
            current.layout.copy(
              progressJobs =
                current.layout.progressJobs.filterNot { job -> job.jobId == intent.jobId }
            )
        )
    is ShellUiEvent.Undo ->
      state.value = current.copy(layout = current.layout.copy(pendingUndoAction = null))
    is ShellUiEvent.ConnectivityChanged ->
      state.value =
        current.copy(
          layout = current.layout.copy(connectivity = intent.status),
          connectivityBanner = current.connectivityBanner.copy(status = intent.status),
        )
    is ShellUiEvent.UpdateTheme ->
      state.value = current.copy(preferences = current.preferences.copy(theme = intent.theme))
    is ShellUiEvent.UpdateDensity ->
      state.value = current.copy(preferences = current.preferences.copy(density = intent.density))
    is ShellUiEvent.SetLeftDrawer ->
      state.value = current.copy(layout = current.layout.copy(isLeftDrawerOpen = intent.open))
    else -> Unit
  }
}

private class EventRecorder {
  val events = mutableListOf<ShellUiEvent>()

  fun record(event: ShellUiEvent) {
    events += event
  }
}

private fun AndroidComposeTestRule<*, *>.waitForProgressItems(
  prefix: String,
  count: Int,
  timeoutMillis: Long = 10_000,
) {
  waitUntil(timeoutMillis) {
    onAllNodesWithTagPrefix(prefix, useUnmergedTree = true).fetchSemanticsNodes().size == count
  }
}

private fun AndroidComposeTestRule<*, *>.onAllNodesWithTagPrefix(
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

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
private fun sampleState(
  showPalette: Boolean,
  commandPalette: CommandPaletteState = samplePaletteState(),
  pendingUndo: UndoPayload? = null,
  progressJobs: List<ProgressJob> = emptyList(),
  connectivity: ConnectivityStatus = ConnectivityStatus.ONLINE,
  rightPanel: RightPanel? = RightPanel.MODEL_SELECTOR,
): ShellUiState {
  val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(600.dp, 800.dp))
  val layout =
    ShellLayoutState(
      windowSizeClass = windowSizeClass,
      isLeftDrawerOpen = false,
      isRightDrawerOpen = true,
      activeRightPanel = rightPanel,
      activeMode = ModeId.HOME,
      showCommandPalette = showPalette,
      connectivity = connectivity,
      pendingUndoAction = pendingUndo,
      progressJobs = progressJobs,
      recentActivity = sampleRecentActivity(),
    )
  val banner =
    ConnectivityBannerState(
      status = connectivity,
      queuedActionCount = progressJobs.count { !it.isTerminal },
      cta = sampleCommands().first(),
    )

  return ShellUiState(
    layout = layout,
    commandPalette = commandPalette,
    connectivityBanner = banner,
    preferences = UiPreferenceSnapshot(),
    modeCards = sampleModeCards(),
    quickActions = sampleQuickActions(),
  )
}

private fun sampleModeCards(): List<ModeCard> =
  listOf(
    ModeCard(
      id = ModeId.CHAT,
      title = "Chat",
      icon = Icons.Outlined.Home,
      primaryAction = CommandAction(id = "chat", title = "Chat"),
    ),
    ModeCard(
      id = ModeId.IMAGE,
      title = "Image",
      icon = Icons.Outlined.Home,
      primaryAction = CommandAction(id = "image", title = "Generate"),
    ),
  )

private fun sampleQuickActions(): List<CommandAction> =
  sampleModeCards().map { card -> card.primaryAction }

private fun sampleCommands(): List<CommandAction> =
  listOf(
    CommandAction(
      id = "mode-chat",
      title = "Chat",
      subtitle = "Converse with AI",
      category = CommandCategory.MODES,
      destination = CommandDestination.Navigate("chat"),
    ),
    CommandAction(
      id = "mode-image",
      title = "Image",
      subtitle = "Generate visuals",
      category = CommandCategory.MODES,
      destination = CommandDestination.Navigate("image"),
    ),
    CommandAction(
      id = "mode-audio",
      title = "Audio",
      subtitle = "Process sound",
      category = CommandCategory.MODES,
      destination = CommandDestination.Navigate("audio"),
    ),
  )

private fun samplePaletteState(): CommandPaletteState =
  CommandPaletteState(
    query = "",
    results = sampleCommands(),
    recentCommands = sampleCommands().take(1),
    selectedIndex = 0,
    surfaceTarget = CommandCategory.MODES,
  )

private fun sampleRecentActivity(): List<RecentActivityItem> =
  listOf(
    RecentActivityItem(
      id = "recent-chat",
      modeId = ModeId.CHAT,
      title = "Chat with Nova",
      timestamp = Instant.now().minusSeconds(120),
      status = RecentStatus.COMPLETED,
    )
  )

private fun sampleProgressJob(
  status: JobStatus = JobStatus.FAILED,
  canRetry: Boolean = true,
  type: JobType = JobType.MODEL_DOWNLOAD,
): ProgressJob =
  ProgressJob(
    jobId = UUID.randomUUID(),
    type = type,
    status = status,
    progress = if (status == JobStatus.FAILED) 0f else 0.35f,
    eta = Duration.ofSeconds(45),
    canRetry = canRetry,
    queuedAt = Instant.now(),
  )
