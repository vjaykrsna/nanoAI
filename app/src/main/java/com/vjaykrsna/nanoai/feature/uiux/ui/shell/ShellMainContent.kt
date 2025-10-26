package com.vjaykrsna.nanoai.feature.uiux.ui.shell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import com.vjaykrsna.nanoai.feature.uiux.presentation.CommandInvocationSource
import com.vjaykrsna.nanoai.feature.uiux.presentation.ModeId
import com.vjaykrsna.nanoai.feature.uiux.presentation.RightPanel
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellUiState
import com.vjaykrsna.nanoai.feature.uiux.ui.HomeScreen
import com.vjaykrsna.nanoai.feature.uiux.ui.components.ConnectivityBanner
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShellMainSurface(
  state: ShellUiState,
  snackbarHostState: SnackbarHostState,
  onEvent: (ShellUiEvent) -> Unit,
  modeContent: @Composable (ModeId) -> Unit,
  modifier: Modifier = Modifier,
  originalOnEvent: (ShellUiEvent) -> Unit = onEvent,
) {
  val layout = state.layout
  Scaffold(
    modifier = modifier,
    topBar = {
      ShellTopAppBar(
        state = state,
        onToggleLeftDrawer = { originalOnEvent(ShellUiEvent.ToggleLeftDrawer) },
        onToggleRightDrawer = { panel -> originalOnEvent(ShellUiEvent.ToggleRightDrawer(panel)) },
        onEvent = onEvent,
      )
    },
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .testTag("shell_content")
          .semantics {
            contentDescription = "Main content area"
            stateDescription = layout.connectivityStatusDescription
          }
          .focusable(),
      verticalArrangement = Arrangement.Top,
    ) {
      val bannerState = state.connectivityBanner
      if (!layout.isPaletteVisible && bannerState.isVisible) {
        ConnectivityBanner(
          state = bannerState,
          onCtaClick = {
            bannerState.cta?.let { action ->
              handleCommandAction(action, CommandInvocationSource.BANNER, onEvent)
            }
          },
          onDismiss = {
            // Persist dismissal once the repository exposes the corresponding event.
          },
          modifier = Modifier.fillMaxWidth().testTag("connectivity_banner"),
        )
      }

      when (layout.activeMode) {
        ModeId.HOME ->
          HomeScreen(
            layout = layout,
            modeCards = state.modeCards,
            quickActions = state.quickActions,
            recentActivity = layout.recentActivity,
            progressJobs = layout.progressJobs,
            onModeSelect = { modeId -> onEvent(ShellUiEvent.ModeSelected(modeId)) },
            onQuickActionSelect = { action ->
              handleCommandAction(
                action = action,
                source = CommandInvocationSource.QUICK_ACTION,
                onEvent = onEvent,
              )
            },
            onRecentActivitySelect = { item -> onEvent(ShellUiEvent.ModeSelected(item.modeId)) },
            onProgressRetry = { job -> onEvent(ShellUiEvent.RetryJob(job)) },
            onProgressDismiss = { job -> onEvent(ShellUiEvent.CompleteJob(job.jobId)) },
            modifier = Modifier.fillMaxSize(),
          )
        else -> modeContent(layout.activeMode)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShellTopAppBar(
  state: ShellUiState,
  onToggleLeftDrawer: () -> Unit,
  onToggleRightDrawer: (RightPanel) -> Unit,
  onEvent: (ShellUiEvent) -> Unit,
) {
  val layout = state.layout
  // var expanded by remember { mutableStateOf(false) }

  TopAppBar(
    title = {
      val titleText =
        if (layout.activeMode == ModeId.CHAT && state.chatState != null) {
          state.chatState.availablePersonas
            .find { it.personaId == state.chatState.currentPersonaId }
            ?.name ?: "Chat"
        } else {
          layout.activeMode.name.lowercase(Locale.ROOT).replaceFirstChar {
            it.titlecase(Locale.ROOT)
          }
        }
      Text(
        text = titleText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier =
          Modifier.clickable {
            if (layout.activeMode == ModeId.CHAT) {
              onEvent(ShellUiEvent.ChatTitleClicked)
            }
          },
      )
    },
    navigationIcon = {
      IconButton(onClick = onToggleLeftDrawer, modifier = Modifier.testTag("topbar_nav_icon")) {
        Icon(Icons.Outlined.Menu, contentDescription = "Toggle navigation drawer")
      }
    },
    actions = {
      IconButton(
        onClick = { onToggleRightDrawer(RightPanel.MODEL_SELECTOR) },
        modifier = Modifier.testTag("topbar_model_selector"),
      ) {
        Icon(Icons.Outlined.Tune, contentDescription = "Open model selector")
      }
      /*
      if (layout.activeMode == ModeId.CHAT) {
        IconButton(
          onClick = { expanded = true },
          modifier = Modifier.testTag("topbar_chat_menu"),
        ) {
          Icon(Icons.Outlined.MoreVert, contentDescription = "Chat menu")
        }
        DropdownMenu(
          expanded = expanded,
          onDismissRequest = { expanded = false },
        ) {
          DropdownMenuItem(
            text = { Text(stringResource(R.string.nano_shell_select_model)) },
            onClick = {
              onToggleRightDrawer(RightPanel.MODEL_SELECTOR)
              expanded = false
            },
          )
        }
      }
      */
    },
    colors = TopAppBarDefaults.topAppBarColors(),
  )
}
