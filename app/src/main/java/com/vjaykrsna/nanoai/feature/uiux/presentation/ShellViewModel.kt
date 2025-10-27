@file:OptIn(
  androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi::class
)

package com.vjaykrsna.nanoai.feature.uiux.presentation

// import com.vjaykrsna.telemetry.ShellTelemetry
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.core.data.repository.NavigationRepository
import com.vjaykrsna.nanoai.feature.uiux.ui.shell.ShellUiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** ViewModel coordinating shell layout state and user intents. */

// Constants for UI state combine lambda array indices
private const val WINDOW_SIZE_CLASS_INDEX = 0
private const val ACTIVE_MODE_INDEX = 1
private const val LEFT_DRAWER_OPEN_INDEX = 2
private const val RIGHT_DRAWER_OPEN_INDEX = 3
private const val ACTIVE_RIGHT_PANEL_INDEX = 4
private const val RECENT_ACTIVITY_INDEX = 5
private const val UNDO_PAYLOAD_INDEX = 6
private const val COMMAND_PALETTE_STATE_INDEX = 7
private const val PROGRESS_JOBS_INDEX = 8
private const val CHAT_STATE_INDEX = 9

// Constants for flow sharing
private const val UI_STATE_SUBSCRIPTION_TIMEOUT_MS = 5000L

private data class ModeCardDefinition(
  val id: ModeId,
  val title: String,
  val subtitle: String,
  val icon: ImageVector,
  val actionId: String,
  val actionTitle: String,
  val actionCategory: CommandCategory,
  val requiresOnline: Boolean = false,
)

private val MODE_CARD_DEFINITIONS =
  listOf(
    ModeCardDefinition(
      id = ModeId.CHAT,
      title = "Chat",
      subtitle = "Conversational AI assistant",
      icon = Icons.AutoMirrored.Filled.Chat,
      actionId = "new_chat",
      actionTitle = "New Chat",
      actionCategory = CommandCategory.MODES,
    ),
    ModeCardDefinition(
      id = ModeId.IMAGE,
      title = "Image",
      subtitle = "Generate images from text",
      icon = Icons.Default.Image,
      actionId = "new_image",
      actionTitle = "Generate",
      actionCategory = CommandCategory.MODES,
      requiresOnline = true,
    ),
    ModeCardDefinition(
      id = ModeId.AUDIO,
      title = "Audio",
      subtitle = "Voice and audio processing",
      icon = Icons.Default.Mic,
      actionId = "new_audio",
      actionTitle = "Record",
      actionCategory = CommandCategory.MODES,
    ),
    ModeCardDefinition(
      id = ModeId.CODE,
      title = "Code",
      subtitle = "Code generation and analysis",
      icon = Icons.Default.Code,
      actionId = "new_code",
      actionTitle = "Code",
      actionCategory = CommandCategory.MODES,
    ),
    ModeCardDefinition(
      id = ModeId.HISTORY,
      title = "History",
      subtitle = "View past conversations",
      icon = Icons.Default.History,
      actionId = "view_history",
      actionTitle = "History",
      actionCategory = CommandCategory.MODES,
    ),
    ModeCardDefinition(
      id = ModeId.SETTINGS,
      title = "Settings",
      subtitle = "Configure your experience",
      icon = Icons.Default.Settings,
      actionId = "open_settings",
      actionTitle = "Settings",
      actionCategory = CommandCategory.SETTINGS,
    ),
  )

@Suppress("LargeClass")
@HiltViewModel
class ShellViewModel
@Inject
constructor(
  private val navigationRepository: NavigationRepository,
  // Sub-ViewModels for focused responsibilities
  private val navigationViewModel: NavigationViewModel,
  private val connectivityViewModel: ConnectivityViewModel,
  private val progressViewModel: ProgressViewModel,
  private val themeViewModel: ThemeViewModel,
  // private val telemetry: ShellTelemetry,
  @Suppress("UnusedPrivateProperty")
  @MainImmediateDispatcher
  private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {

  private val _activeMode = MutableStateFlow(ModeId.HOME)
  private val _isLeftDrawerOpen = MutableStateFlow(false)
  private val _isRightDrawerOpen = MutableStateFlow(false)
  private val _activeRightPanel = MutableStateFlow<RightPanel?>(null)

  private val _chatState = MutableStateFlow<ChatState?>(null)

  @Suppress("CyclomaticComplexMethod")
  fun onEvent(event: ShellUiEvent) {
    when (event) {
      is ShellUiEvent.ModeSelected -> {
        navigationViewModel.openMode(event.modeId)
        _activeMode.value = event.modeId
      }
      is ShellUiEvent.ToggleLeftDrawer -> {
        navigationViewModel.toggleLeftDrawer()
        _isLeftDrawerOpen.value = !_isLeftDrawerOpen.value
      }
      is ShellUiEvent.SetLeftDrawer -> {
        navigationViewModel.setLeftDrawer(event.open)
        _isLeftDrawerOpen.value = event.open
      }
      is ShellUiEvent.ToggleRightDrawer -> {
        navigationViewModel.toggleRightDrawer(event.panel)
        _isRightDrawerOpen.value = !_isRightDrawerOpen.value
        _activeRightPanel.value = if (_isRightDrawerOpen.value) event.panel else null
      }
      is ShellUiEvent.ShowCommandPalette -> navigationViewModel.showCommandPalette(event.source)
      is ShellUiEvent.HideCommandPalette -> navigationViewModel.hideCommandPalette()
      is ShellUiEvent.CommandInvoked -> onCommandInvoked(event.action, event.source)
      is ShellUiEvent.QueueJob -> progressViewModel.queueGeneration(event.job)
      is ShellUiEvent.RetryJob -> progressViewModel.retryJob(event.job)
      is ShellUiEvent.CompleteJob -> progressViewModel.completeJob(event.jobId)
      is ShellUiEvent.Undo -> progressViewModel.undoAction(event.payload)
      is ShellUiEvent.ConnectivityChanged -> connectivityViewModel.updateConnectivity(event.status)
      is ShellUiEvent.UpdateTheme -> themeViewModel.updateThemePreference(event.theme)
      is ShellUiEvent.UpdateDensity -> themeViewModel.updateVisualDensity(event.density)
      else -> Unit
    }
  }

  val uiState: StateFlow<ShellUiState> =
    combine(
        navigationRepository.windowSizeClass,
        _activeMode,
        _isLeftDrawerOpen,
        _isRightDrawerOpen,
        _activeRightPanel,
        navigationRepository.recentActivity,
        navigationRepository.undoPayload,
        navigationRepository.commandPaletteState,
        progressViewModel.progressJobs,
        _chatState,
      ) { values ->
        val windowSizeClass = values[WINDOW_SIZE_CLASS_INDEX] as WindowSizeClass
        val activeMode = values[ACTIVE_MODE_INDEX] as ModeId
        val isLeftDrawerOpen = values[LEFT_DRAWER_OPEN_INDEX] as Boolean
        val isRightDrawerOpen = values[RIGHT_DRAWER_OPEN_INDEX] as Boolean
        val activeRightPanel = values[ACTIVE_RIGHT_PANEL_INDEX] as RightPanel?
        @Suppress("UNCHECKED_CAST")
        val recentActivity =
          values[RECENT_ACTIVITY_INDEX] as? List<RecentActivityItem>
            ?: error("Expected List<RecentActivityItem> at index $RECENT_ACTIVITY_INDEX")
        val undoPayload = values[UNDO_PAYLOAD_INDEX] as UndoPayload?
        val commandPaletteState = values[COMMAND_PALETTE_STATE_INDEX] as CommandPaletteState
        @Suppress("UNCHECKED_CAST")
        val jobs =
          values[PROGRESS_JOBS_INDEX] as? List<ProgressJob>
            ?: error("Expected List<ProgressJob> at index $PROGRESS_JOBS_INDEX")
        val chatState = values[CHAT_STATE_INDEX] as ChatState?

        val normalizedLayout =
          ShellLayoutState(
            windowSizeClass = windowSizeClass,
            isLeftDrawerOpen = isLeftDrawerOpen,
            isRightDrawerOpen = isRightDrawerOpen,
            activeRightPanel = activeRightPanel,
            activeMode = activeMode,
            showCommandPalette = commandPaletteState.results.isNotEmpty(),
            connectivity = ConnectivityStatus.ONLINE, // TODO: from connectivity
            pendingUndoAction = undoPayload,
            progressJobs = jobs,
            recentActivity = recentActivity,
          )
        val normalizedBanner = ConnectivityBannerState(status = ConnectivityStatus.ONLINE) // TODO

        ShellUiState(
          layout = normalizedLayout,
          commandPalette = commandPaletteState,
          connectivityBanner = normalizedBanner,
          preferences = UiPreferenceSnapshot(), // TODO
          modeCards = buildModeCards(true), // TODO
          quickActions = buildQuickActions(activeMode),
          chatState = chatState,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(UI_STATE_SUBSCRIPTION_TIMEOUT_MS),
        initialValue = buildInitialState(),
      )

  private fun buildInitialState(): ShellUiState {
    val defaultWindowSize =
      androidx.compose.material3.windowsizeclass.WindowSizeClass.calculateFromSize(
        androidx.compose.ui.unit.DpSize(width = 640.dp, height = 360.dp)
      )
    return ShellUiState(
      layout =
        ShellLayoutState(
          windowSizeClass = defaultWindowSize,
          isLeftDrawerOpen = false,
          isRightDrawerOpen = false,
          activeRightPanel = null,
          activeMode = ModeId.HOME,
          showCommandPalette = false,
          connectivity = ConnectivityStatus.ONLINE,
          pendingUndoAction = null,
          progressJobs = emptyList(),
          recentActivity = emptyList(),
        ),
      commandPalette = CommandPaletteState.Empty,
      connectivityBanner = ConnectivityBannerState(status = ConnectivityStatus.ONLINE),
      preferences = UiPreferenceSnapshot(),
    )
  }

  /** Records telemetry for command invocations to understand palette usage. */
  @Suppress("UnusedParameter")
  fun onCommandInvoked(action: CommandAction, source: CommandInvocationSource) {
    val layout = uiState.value.layout
    /*
    // // telemetry.trackCommandInvocation(action, source, layout.activeMode)
    if (source == CommandInvocationSource.PALETTE && layout.isPaletteVisible) {
      // // telemetry.trackCommandPaletteDismissed(PaletteDismissReason.EXECUTED, layout.activeMode)
    }
    */
  }

  /** Updates the current window size class so adaptive layouts respond to device changes. */
  fun updateWindowSizeClass(sizeClass: WindowSizeClass) {
    navigationViewModel.updateWindowSizeClass(sizeClass)
  }

  /** Updates the chat-specific state for contextual UI. */
  fun updateChatState(chatState: ChatState?) {
    _chatState.value = chatState
  }

  private fun buildModeCards(isOnline: Boolean): List<ModeCard> =
    MODE_CARD_DEFINITIONS.filter { !it.requiresOnline || isOnline }
      .map { definition ->
        ModeCard(
          id = definition.id,
          title = definition.title,
          subtitle = definition.subtitle,
          icon = definition.icon,
          primaryAction =
            CommandAction(
              id = definition.actionId,
              title = definition.actionTitle,
              category = definition.actionCategory,
            ),
          enabled = true,
        )
      }

  private fun buildQuickActions(activeMode: ModeId): List<CommandAction> =
    when (activeMode) {
      ModeId.CHAT ->
        listOf(
          CommandAction(id = "new_chat", title = "New Chat", category = CommandCategory.MODES),
          CommandAction(id = "clear_chat", title = "Clear Chat", category = CommandCategory.MODES),
        )
      ModeId.IMAGE ->
        listOf(
          CommandAction(
            id = "generate_image",
            title = "Generate",
            category = CommandCategory.MODES,
          ),
          CommandAction(id = "edit_image", title = "Edit", category = CommandCategory.MODES),
        )
      ModeId.AUDIO ->
        listOf(
          CommandAction(id = "record_audio", title = "Record", category = CommandCategory.MODES),
          CommandAction(
            id = "transcribe_audio",
            title = "Transcribe",
            category = CommandCategory.MODES,
          ),
        )
      ModeId.CODE ->
        listOf(
          CommandAction(id = "generate_code", title = "Generate", category = CommandCategory.MODES),
          CommandAction(id = "analyze_code", title = "Analyze", category = CommandCategory.MODES),
        )
      ModeId.HISTORY ->
        listOf(
          CommandAction(id = "view_history", title = "View All", category = CommandCategory.MODES),
          CommandAction(id = "search_history", title = "Search", category = CommandCategory.MODES),
        )
      ModeId.SETTINGS ->
        listOf(
          CommandAction(id = "open_settings", title = "Open", category = CommandCategory.SETTINGS)
        )
      else ->
        listOf(CommandAction(id = "new_chat", title = "New Chat", category = CommandCategory.MODES))
    }
}

/** Aggregated UI state exposed by [ShellViewModel]. */
data class ShellUiState(
  val layout: ShellLayoutState,
  val commandPalette: CommandPaletteState,
  val connectivityBanner: ConnectivityBannerState,
  val preferences: UiPreferenceSnapshot,
  val modeCards: List<ModeCard> = emptyList(),
  val quickActions: List<CommandAction> = emptyList(),
  val chatState: ChatState? = null,
)
