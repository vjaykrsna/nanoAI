@file:Suppress("CyclomaticComplexMethod")

package com.vjaykrsna.nanoai.feature.uiux

import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilDoesNotExist
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
import com.vjaykrsna.nanoai.feature.uiux.presentation.ModeId
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
  ExperimentalMaterial3WindowSizeClassApi::class,
  ExperimentalTestApi::class,
)
class OfflineProgressTest {
  @org.junit.jupiter.api.extension.RegisterExtension
  @JvmField
  val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun offlineBanner_showsQueuedCount() {
    val state = mutableStateOf(sampleState(ConnectivityStatus.OFFLINE, queuedJobs = 2))
    composeRule.setContent { NanoAITheme { NanoShellScaffold(state = state.value, onEvent = {}) } }

    composeRule.waitForIdle()

    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule
        .onAllNodesWithTag("connectivity_banner", useUnmergedTree = true)
        .fetchSemanticsNodes(false)
        .isNotEmpty()
    }

    composeRule.onNodeWithTag("connectivity_banner", useUnmergedTree = true).assertIsDisplayed()
    composeRule.onNodeWithTag("connectivity_banner_cta", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun progressList_displaysQueuedJobs() {
    val state = mutableStateOf(sampleState(ConnectivityStatus.OFFLINE, queuedJobs = 3))
    composeRule.setContent { NanoAITheme { NanoShellScaffold(state = state.value, onEvent = {}) } }

    composeRule.waitForIdle()

    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule
        .onAllNodesWithTagPrefix("progress_list_item_", useUnmergedTree = true)
        .fetchSemanticsNodes(false)
        .size == 3
    }

    val items = composeRule.onAllNodesWithTagPrefix("progress_list_item_", useUnmergedTree = true)
    items.assertCountEquals(3)
    items[0].assertContentDescriptionContains("Waiting")
  }

  @Test
  fun reconnect_updatesConnectivity_andHidesBanner() {
    val state = mutableStateOf(sampleState(ConnectivityStatus.OFFLINE, queuedJobs = 1))
    composeRule.setContent {
      NanoAITheme {
        NanoShellScaffold(state = state.value, onEvent = { intent -> handleIntent(state, intent) })
      }
    }

    composeRule.waitForIdle()

    handleIntent(state, ShellUiEvent.ConnectivityChanged(ConnectivityStatus.ONLINE))
    composeRule.waitUntilDoesNotExist(hasTestTag("connectivity_banner"))
    composeRule
      .onAllNodesWithTag("connectivity_banner", useUnmergedTree = true)
      .assertCountEquals(0)
    assertThat(state.value.layout.connectivity).isEqualTo(ConnectivityStatus.ONLINE)
    assertThat(state.value.connectivityBanner.queuedActionCount).isEqualTo(0)
  }

  @Test
  fun retryFailedJob_emitsQueueIntent() {
    val jobId = UUID.randomUUID()
    val failingJob =
      ProgressJob(
        jobId = jobId,
        type = JobType.MODEL_DOWNLOAD,
        status = JobStatus.FAILED,
        progress = 0.2f,
        eta = Duration.ofSeconds(30),
        canRetry = true,
        queuedAt = Instant.now(),
      )
    val state =
      mutableStateOf(
        sampleState(ConnectivityStatus.ONLINE, queuedJobs = 0, jobs = listOf(failingJob))
      )
    val events = mutableListOf<ShellUiEvent>()

    composeRule.setContent {
      NanoAITheme {
        NanoShellScaffold(
          state = state.value,
          onEvent = { intent ->
            events += intent
            handleIntent(state, intent)
          },
        )
      }
    }

    composeRule.waitForNodeWithTag("progress_retry_button_${jobId}")

    val retryButton = composeRule.onNodeWithTag("progress_retry_button_${jobId}")
    retryButton.assertIsEnabled()
    retryButton.assert(
      SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Retry available")
    )
    retryButton.performClick()
    assertThat(events.filterIsInstance<ShellUiEvent.RetryJob>()).isNotEmpty()
  }

  private fun handleIntent(state: MutableState<ShellUiState>, intent: ShellUiEvent) {
    val current = state.value
    state.value =
      when (intent) {
        is ShellUiEvent.ConnectivityChanged -> handleConnectivityChanged(current, intent)
        is ShellUiEvent.QueueJob -> handleQueueJob(current, intent)
        is ShellUiEvent.RetryJob -> handleRetryJob(current, intent)
        is ShellUiEvent.CompleteJob -> handleCompleteJob(current, intent)
        is ShellUiEvent.ModeSelected -> handleModeSelected(current, intent)
        is ShellUiEvent.ToggleLeftDrawer -> handleToggleLeftDrawer(current)
        is ShellUiEvent.ToggleRightDrawer -> handleToggleRightDrawer(current, intent)
        is ShellUiEvent.ShowCommandPalette -> handleShowCommandPalette(current)
        is ShellUiEvent.HideCommandPalette -> handleHideCommandPalette(current)
        is ShellUiEvent.Undo -> handleUndo(current)
        is ShellUiEvent.UpdateTheme -> handleUpdateTheme(current, intent)
        is ShellUiEvent.UpdateDensity -> handleUpdateDensity(current, intent)
        is ShellUiEvent.SetLeftDrawer -> handleSetLeftDrawer(current, intent)
        else -> current
      }
  }

  private fun handleConnectivityChanged(
    current: ShellUiState,
    intent: ShellUiEvent.ConnectivityChanged,
  ) =
    current.copy(
      layout = current.layout.copy(connectivity = intent.status),
      connectivityBanner =
        current.connectivityBanner.copy(status = intent.status, queuedActionCount = 0),
    )

  private fun handleQueueJob(current: ShellUiState, intent: ShellUiEvent.QueueJob) =
    current.copy(
      layout = current.layout.copy(progressJobs = current.layout.progressJobs + intent.job)
    )

  private fun handleRetryJob(current: ShellUiState, intent: ShellUiEvent.RetryJob) =
    current.copy(
      layout =
        current.layout.copy(
          progressJobs =
            current.layout.progressJobs.map { job ->
              if (job.jobId == intent.job.jobId) job.copy(status = JobStatus.PENDING) else job
            }
        )
    )

  private fun handleCompleteJob(current: ShellUiState, intent: ShellUiEvent.CompleteJob) =
    current.copy(
      layout =
        current.layout.copy(
          progressJobs = current.layout.progressJobs.filterNot { job -> job.jobId == intent.jobId }
        )
    )

  private fun handleModeSelected(current: ShellUiState, intent: ShellUiEvent.ModeSelected) =
    current.copy(layout = current.layout.copy(activeMode = intent.modeId))

  private fun handleToggleLeftDrawer(current: ShellUiState) =
    current.copy(layout = current.layout.copy(isLeftDrawerOpen = !current.layout.isLeftDrawerOpen))

  private fun handleToggleRightDrawer(
    current: ShellUiState,
    intent: ShellUiEvent.ToggleRightDrawer,
  ) =
    current.copy(
      layout =
        current.layout.copy(
          isRightDrawerOpen = !current.layout.isRightDrawerOpen,
          activeRightPanel = intent.panel,
        )
    )

  private fun handleShowCommandPalette(current: ShellUiState) =
    current.copy(layout = current.layout.copy(showCommandPalette = true))

  private fun handleHideCommandPalette(current: ShellUiState) =
    current.copy(layout = current.layout.copy(showCommandPalette = false))

  private fun handleUndo(current: ShellUiState) =
    current.copy(layout = current.layout.copy(pendingUndoAction = null))

  private fun handleUpdateTheme(current: ShellUiState, intent: ShellUiEvent.UpdateTheme) =
    current.copy(preferences = current.preferences.copy(theme = intent.theme))

  private fun handleUpdateDensity(current: ShellUiState, intent: ShellUiEvent.UpdateDensity) =
    current.copy(preferences = current.preferences.copy(density = intent.density))

  private fun handleSetLeftDrawer(current: ShellUiState, intent: ShellUiEvent.SetLeftDrawer) =
    current.copy(layout = current.layout.copy(isLeftDrawerOpen = intent.open))

  private fun sampleState(
    connectivity: ConnectivityStatus,
    queuedJobs: Int,
    jobs: List<ProgressJob> =
      List(queuedJobs) { index ->
        ProgressJob(
          jobId = UUID.randomUUID(),
          type = JobType.IMAGE_GENERATION,
          status = JobStatus.PENDING,
          progress = 0f,
          eta = Duration.ofSeconds(60),
          canRetry = true,
          queuedAt = Instant.now().minusSeconds(index.toLong()),
        )
      },
  ): ShellUiState {
    val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(600.dp, 800.dp))
    val layout =
      ShellLayoutState(
        windowSizeClass = windowSizeClass,
        isLeftDrawerOpen = false,
        isRightDrawerOpen = false,
        activeRightPanel = RightPanel.MODEL_SELECTOR,
        activeMode = ModeId.HOME,
        showCommandPalette = false,
        connectivity = connectivity,
        pendingUndoAction = UndoPayload(actionId = "pending"),
        progressJobs = jobs,
        recentActivity = sampleRecentActivity(),
      )
    val banner =
      ConnectivityBannerState(
        status = connectivity,
        queuedActionCount = jobs.size,
        cta =
          CommandAction(
            id = "view-queue",
            title = "View queue (${jobs.size})",
            category = CommandCategory.JOBS,
            destination = CommandDestination.OpenRightPanel(RightPanel.MODEL_SELECTOR),
          ),
      )
    val palette =
      CommandPaletteState(query = "", results = emptyList(), recentCommands = emptyList())
    return ShellUiState(
      layout = layout,
      commandPalette = palette,
      connectivityBanner = banner,
      preferences = UiPreferenceSnapshot(),
    )
  }

  private fun sampleRecentActivity(): List<RecentActivityItem> =
    listOf(
      RecentActivityItem(
        id = "recent1",
        modeId = ModeId.CHAT,
        title = "Chat thread",
        timestamp = Instant.now().minusSeconds(200),
        status = RecentStatus.IN_PROGRESS,
      )
    )
}

private fun AndroidComposeTestRule<*, *>.waitForNodeWithTag(
  tag: String,
  useUnmergedTree: Boolean = false,
  timeoutMillis: Long = 5_000,
) {
  val deadline = SystemClock.elapsedRealtime() + timeoutMillis
  while (SystemClock.elapsedRealtime() < deadline) {
    val nodes = onAllNodesWithTag(tag, useUnmergedTree).fetchSemanticsNodes(false)
    if (nodes.isNotEmpty()) return
    waitForIdle()
  }
  throw AssertionError("Timed out waiting for node with tag '$tag'")
}

private fun AndroidComposeTestRule<*, *>.onAllNodesWithTagPrefix(
  prefix: String,
  useUnmergedTree: Boolean = false,
) = onAllNodes(SemanticsMatcherExpectTagPrefix(prefix), useUnmergedTree = useUnmergedTree)

private fun SemanticsMatcherExpectTagPrefix(prefix: String): SemanticsMatcher =
  SemanticsMatcher("Has test tag with prefix $prefix") { node ->
    val tag = node.config.getOrNull(SemanticsProperties.TestTag)
    tag?.startsWith(prefix) == true
  }
