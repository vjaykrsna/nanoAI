package com.vjaykrsna.nanoai.feature.uiux.presentation

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.core.data.repository.UserProfileRepository
import com.vjaykrsna.nanoai.core.domain.model.DownloadTask
import com.vjaykrsna.nanoai.core.domain.model.uiux.LayoutSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.UIStateSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.UiPreferencesSnapshot
import com.vjaykrsna.nanoai.core.domain.model.uiux.UserProfile
import com.vjaykrsna.nanoai.feature.library.data.DownloadManager
import com.vjaykrsna.nanoai.feature.library.domain.DownloadStatus
import com.vjaykrsna.nanoai.feature.uiux.domain.CommandPaletteActionProvider
import com.vjaykrsna.nanoai.feature.uiux.domain.ProgressCenterCoordinator
import com.vjaykrsna.nanoai.feature.uiux.presentation.navigation.Screen
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Instant as KtxInstant

internal class NoopUserProfileRepository : UserProfileRepository {
  private val profileFlow = MutableStateFlow<UserProfile?>(null)
  private val preferencesFlow = MutableStateFlow(UiPreferencesSnapshot())
  private val uiStateFlow = MutableStateFlow<UIStateSnapshot?>(null)
  private val offlineFlow = MutableStateFlow(false)

  override fun observeUserProfile(userId: String): Flow<UserProfile?> = profileFlow

  override fun observeOfflineStatus(): Flow<Boolean> = offlineFlow

  override suspend fun getUserProfile(userId: String): UserProfile? = null

  override fun observePreferences(): Flow<UiPreferencesSnapshot> = preferencesFlow

  override suspend fun updateThemePreference(userId: String, themePreferenceName: String) = Unit

  override suspend fun updateVisualDensity(userId: String, visualDensityName: String) = Unit

  override suspend fun updateCompactMode(userId: String, enabled: Boolean) = Unit

  override suspend fun updatePinnedTools(userId: String, pinnedTools: List<String>) = Unit

  override suspend fun saveLayoutSnapshot(userId: String, layout: LayoutSnapshot, position: Int) =
    Unit

  override suspend fun deleteLayoutSnapshot(layoutId: String) = Unit

  override fun observeUIStateSnapshot(userId: String): Flow<UIStateSnapshot?> = uiStateFlow

  override suspend fun updateLeftDrawerOpen(userId: String, open: Boolean) {
    val current =
      uiStateFlow.value
        ?: UIStateSnapshot(
          userId = userId,
          expandedPanels = emptyList(),
          recentActions = emptyList(),
          isSidebarCollapsed = false,
        )
    uiStateFlow.value = current.copy(isLeftDrawerOpen = open)
  }

  override suspend fun updateRightDrawerState(userId: String, open: Boolean, panel: String?) {
    val current =
      uiStateFlow.value
        ?: UIStateSnapshot(
          userId = userId,
          expandedPanels = emptyList(),
          recentActions = emptyList(),
          isSidebarCollapsed = false,
        )
    uiStateFlow.value = current.copy(isRightDrawerOpen = open, activeRightPanel = panel)
  }

  override suspend fun updateActiveModeRoute(userId: String, route: String) {
    val current =
      uiStateFlow.value
        ?: UIStateSnapshot(
          userId = userId,
          expandedPanels = emptyList(),
          recentActions = emptyList(),
          isSidebarCollapsed = false,
        )
    uiStateFlow.value = current.copy(activeModeRoute = route)
  }

  override suspend fun updateCommandPaletteVisibility(userId: String, visible: Boolean) {
    val current =
      uiStateFlow.value
        ?: UIStateSnapshot(
          userId = userId,
          expandedPanels = emptyList(),
          recentActions = emptyList(),
          isSidebarCollapsed = false,
        )
    uiStateFlow.value = current.copy(isCommandPaletteVisible = visible)
  }

  override suspend fun recordCommandPaletteRecent(commandId: String) = Unit

  override suspend fun setCommandPaletteRecents(commandIds: List<String>) = Unit

  override suspend fun setConnectivityBannerDismissed(dismissedAt: KtxInstant?) = Unit

  override suspend fun setOfflineOverride(isOffline: Boolean) {
    offlineFlow.value = isOffline
  }
}

internal class FakeNavigationRepository(
  private val userProfileRepository: com.vjaykrsna.nanoai.core.data.repository.UserProfileRepository
) : com.vjaykrsna.nanoai.core.data.repository.NavigationRepository {
  override val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher =
    kotlinx.coroutines.Dispatchers.Unconfined

  private val commandPaletteStateFlow =
    kotlinx.coroutines.flow.MutableStateFlow(CommandPaletteState())
  override val commandPaletteState: kotlinx.coroutines.flow.Flow<CommandPaletteState> =
    commandPaletteStateFlow

  private val recentActivityFlow =
    kotlinx.coroutines.flow.MutableStateFlow<List<RecentActivityItem>>(emptyList())
  override val recentActivity: kotlinx.coroutines.flow.Flow<List<RecentActivityItem>> =
    recentActivityFlow

  @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
  private val windowSizeClassFlow =
    kotlinx.coroutines.flow.MutableStateFlow(
      WindowSizeClass.calculateFromSize(DpSize(800.dp, 600.dp))
    )
  override val windowSizeClass: kotlinx.coroutines.flow.Flow<WindowSizeClass> = windowSizeClassFlow

  internal val undoPayloadFlow =
    kotlinx.coroutines.flow.MutableStateFlow<
      com.vjaykrsna.nanoai.feature.uiux.presentation.UndoPayload?
    >(
      null
    )
  override val undoPayload:
    kotlinx.coroutines.flow.Flow<com.vjaykrsna.nanoai.feature.uiux.presentation.UndoPayload?> =
    undoPayloadFlow

  override fun updateWindowSizeClass(sizeClass: WindowSizeClass) {
    windowSizeClassFlow.value = sizeClass
  }

  override suspend fun openMode(modeId: ModeId) {
    val route = Screen.fromModeId(modeId).route
    userProfileRepository.updateActiveModeRoute("default", route)
    userProfileRepository.updateLeftDrawerOpen("default", false)
    userProfileRepository.updateCommandPaletteVisibility("default", false)
  }

  override suspend fun toggleLeftDrawer() = Unit

  override suspend fun setLeftDrawer(open: Boolean) = Unit

  override suspend fun toggleRightDrawer(panel: RightPanel) {
    userProfileRepository.updateRightDrawerState("default", true, panel.name)
  }

  override suspend fun showCommandPalette(
    source: com.vjaykrsna.nanoai.feature.uiux.presentation.PaletteSource
  ) {
    commandPaletteStateFlow.value = CommandPaletteState(surfaceTarget = CommandCategory.MODES)
  }

  override suspend fun hideCommandPalette() {
    commandPaletteStateFlow.value = CommandPaletteState()
  }

  override suspend fun recordUndoPayload(
    payload: com.vjaykrsna.nanoai.feature.uiux.presentation.UndoPayload?
  ) {
    undoPayloadFlow.value = payload
  }
}

internal class FakeConnectivityRepository :
  com.vjaykrsna.nanoai.core.data.repository.ConnectivityRepository {
  override val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher =
    kotlinx.coroutines.Dispatchers.Unconfined

  private val connectivityBannerStateFlow =
    kotlinx.coroutines.flow.MutableStateFlow(
      ConnectivityBannerState(status = ConnectivityStatus.ONLINE)
    )
  override val connectivityBannerState: kotlinx.coroutines.flow.Flow<ConnectivityBannerState> =
    connectivityBannerStateFlow

  override suspend fun updateConnectivity(status: ConnectivityStatus) {
    connectivityBannerStateFlow.value = ConnectivityBannerState(status = status)
  }
}

internal class FakeThemeRepository : com.vjaykrsna.nanoai.core.data.repository.ThemeRepository {
  override val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher =
    kotlinx.coroutines.Dispatchers.Unconfined

  private val uiPreferenceSnapshotFlow =
    kotlinx.coroutines.flow.MutableStateFlow(UiPreferenceSnapshot())
  override val uiPreferenceSnapshot: kotlinx.coroutines.flow.Flow<UiPreferenceSnapshot> =
    uiPreferenceSnapshotFlow

  override suspend fun updateThemePreference(
    theme: com.vjaykrsna.nanoai.core.domain.model.uiux.ThemePreference
  ) {
    uiPreferenceSnapshotFlow.value = uiPreferenceSnapshotFlow.value.copy(theme = theme)
  }

  override suspend fun updateVisualDensity(
    density: com.vjaykrsna.nanoai.core.domain.model.uiux.VisualDensity
  ) {
    uiPreferenceSnapshotFlow.value = uiPreferenceSnapshotFlow.value.copy(density = density)
  }

  override suspend fun updateHighContrastEnabled(enabled: Boolean) {
    uiPreferenceSnapshotFlow.value =
      uiPreferenceSnapshotFlow.value.copy(highContrastEnabled = enabled)
  }
}

internal class FakeProgressRepository :
  com.vjaykrsna.nanoai.core.data.repository.ProgressRepository {
  override val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher =
    kotlinx.coroutines.Dispatchers.Unconfined

  private val progressJobsFlow =
    kotlinx.coroutines.flow.MutableStateFlow<List<ProgressJob>>(emptyList())
  override val progressJobs: kotlinx.coroutines.flow.Flow<List<ProgressJob>> = progressJobsFlow

  override suspend fun queueJob(job: ProgressJob) {
    progressJobsFlow.value = progressJobsFlow.value + job
  }

  override suspend fun completeJob(jobId: java.util.UUID) {
    progressJobsFlow.value = progressJobsFlow.value.filterNot { it.jobId == jobId }
  }
}

internal fun createFakeCommandPaletteActionProvider(): CommandPaletteActionProvider =
  CommandPaletteActionProvider()

internal fun createFakeProgressCenterCoordinator(): ProgressCenterCoordinator {
  val downloadManager = FakeDownloadManager()
  return ProgressCenterCoordinator(downloadManager = downloadManager)
}

internal class FakeDownloadManager : DownloadManager {
  override suspend fun startDownload(modelId: String): UUID = UUID.randomUUID()

  override suspend fun queueDownload(modelId: String): UUID = UUID.randomUUID()

  override suspend fun pauseDownload(taskId: UUID) = Unit

  override suspend fun resumeDownload(taskId: UUID) = Unit

  override suspend fun cancelDownload(taskId: UUID) = Unit

  override suspend fun retryDownload(taskId: UUID) = Unit

  override suspend fun resetTask(taskId: UUID) = Unit

  override suspend fun getDownloadStatus(taskId: UUID): DownloadTask? = null

  override suspend fun getTaskById(taskId: UUID): Flow<DownloadTask?> = flowOf(null)

  override suspend fun getActiveDownloads(): Flow<List<DownloadTask>> = flowOf(emptyList())

  override fun getQueuedDownloads(): Flow<List<DownloadTask>> = flowOf(emptyList())

  override fun observeManagedDownloads(): Flow<List<DownloadTask>> = flowOf(emptyList())

  override fun observeProgress(taskId: UUID): Flow<Float> = flowOf(0f)

  override suspend fun getMaxConcurrentDownloads(): Int = 2

  override suspend fun updateTaskStatus(taskId: UUID, status: DownloadStatus) = Unit

  override suspend fun getModelIdForTask(taskId: UUID): String? = null

  override suspend fun getDownloadedChecksum(modelId: String): String? = null

  override suspend fun deletePartialFiles(modelId: String) = Unit
}

internal fun createFakeRepositories(): FakeRepositories {
  val userProfileRepository = NoopUserProfileRepository()
  return FakeRepositories(
    navigationRepository = FakeNavigationRepository(userProfileRepository),
    connectivityRepository = FakeConnectivityRepository(),
    themeRepository = FakeThemeRepository(),
    progressRepository = FakeProgressRepository(),
    userProfileRepository = userProfileRepository,
  )
}

internal data class FakeRepositories(
  val navigationRepository: com.vjaykrsna.nanoai.core.data.repository.NavigationRepository,
  val connectivityRepository: com.vjaykrsna.nanoai.core.data.repository.ConnectivityRepository,
  val themeRepository: com.vjaykrsna.nanoai.core.data.repository.ThemeRepository,
  val progressRepository: com.vjaykrsna.nanoai.core.data.repository.ProgressRepository,
  val userProfileRepository: com.vjaykrsna.nanoai.core.data.repository.UserProfileRepository,
)
