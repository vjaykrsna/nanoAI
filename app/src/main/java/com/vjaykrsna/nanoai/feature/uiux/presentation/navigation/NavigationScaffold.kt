package com.vjaykrsna.nanoai.feature.uiux.presentation.navigation

// Welcome / onboarding UI removed — onboarding is no longer part of the shell flow.
// Welcome UI imports removed
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.metrics.performance.PerformanceMetricsState
import com.vjaykrsna.nanoai.feature.audio.ui.AudioScreen
import com.vjaykrsna.nanoai.feature.chat.presentation.ChatViewModel
import com.vjaykrsna.nanoai.feature.chat.ui.ChatScreen
import com.vjaykrsna.nanoai.feature.image.ui.ImageFeatureContainer
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryScreen
import com.vjaykrsna.nanoai.feature.settings.ui.SettingsScreen
import com.vjaykrsna.nanoai.feature.uiux.presentation.AppUiState
import com.vjaykrsna.nanoai.feature.uiux.presentation.ConnectivityStatus
import com.vjaykrsna.nanoai.feature.uiux.presentation.ModeId
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellViewModel
import com.vjaykrsna.nanoai.feature.uiux.ui.shell.NanoShellScaffold
import com.vjaykrsna.nanoai.feature.uiux.ui.shell.ShellUiEvent
import com.vjaykrsna.nanoai.shared.ui.components.DisclaimerDialog

/** Entry point that connects app-wide state to the unified Compose shell. */
@Composable
fun NavigationScaffold(
  appState: AppUiState,
  windowSizeClass: WindowSizeClass,
  modifier: Modifier = Modifier,
  shellViewModel: ShellViewModel = hiltViewModel(),
  chatViewModel: ChatViewModel = hiltViewModel(),
  onDisclaimerShow: () -> Unit = {},
  onDisclaimerAccept: () -> Unit = {},
  onDisclaimerDecline: () -> Unit = {},
) {
  val shellUiState by shellViewModel.uiState.collectAsStateWithLifecycle()

  val view = LocalView.current
  val metricsStateHolder = remember(view) { PerformanceMetricsState.getHolderForHierarchy(view) }

  LaunchedEffect(windowSizeClass) { shellViewModel.updateWindowSizeClass(windowSizeClass) }
  LaunchedEffect(appState.offline) {
    val status = if (appState.offline) ConnectivityStatus.OFFLINE else ConnectivityStatus.ONLINE
    shellViewModel.onEvent(ShellUiEvent.ConnectivityChanged(status))
  }

  LaunchedEffect(shellUiState.layout.activeMode) {
    metricsStateHolder.state?.putState("shell_mode", shellUiState.layout.activeMode.name)
  }
  LaunchedEffect(shellUiState.layout.progressJobs) {
    val activeJobs = shellUiState.layout.progressJobs.count { !it.isTerminal }
    metricsStateHolder.state?.putState("active_jobs", activeJobs.toString())
  }

  DisposableEffect(metricsStateHolder) {
    onDispose {
      metricsStateHolder.state?.removeState("shell_mode")
      metricsStateHolder.state?.removeState("active_jobs")
    }
  }

  var disclaimerDismissedForSession by rememberSaveable { mutableStateOf(false) }
  val shouldShowDisclaimer = appState.disclaimer.shouldShow && !disclaimerDismissedForSession

  LaunchedEffect(appState.disclaimer.shouldShow) {
    if (!appState.disclaimer.shouldShow) {
      disclaimerDismissedForSession = false
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    NanoShellScaffold(
      state = shellUiState,
      onEvent = { event ->
        when (event) {
          is ShellUiEvent.ChatPersonaSelected ->
            chatViewModel.switchPersona(event.personaId, event.action)
          is ShellUiEvent.ChatTitleClicked -> chatViewModel.showModelPicker()
          else -> shellViewModel.onEvent(event)
        }
      },
      modifier = Modifier.fillMaxSize(),
      modeContent = { modeId ->
        ShellModeContent(
          modeId = modeId,
          modifier = Modifier.fillMaxSize(),
          onUpdateChatState = shellViewModel::updateChatState,
          onNavigate = { mode -> shellViewModel.onEvent(ShellUiEvent.ModeSelected(mode)) },
        )
      },
    )

    // Onboarding / welcome removed — main shell is shown directly.

    if (shouldShowDisclaimer) {
      DisclaimerDialog(
        onAccept = {
          onDisclaimerAccept()
          disclaimerDismissedForSession = true
        },
        onDecline = {
          disclaimerDismissedForSession = true
          onDisclaimerDecline()
        },
        onDismissRequest = {
          disclaimerDismissedForSession = true
          onDisclaimerDecline()
        },
        onDialogShow = onDisclaimerShow,
      )
    }

    // Skip links for keyboard navigation accessibility
    SkipLinksNavigation(
      onSkipToContent = { shellViewModel.onEvent(ShellUiEvent.ModeSelected(ModeId.HOME)) },
      onSkipToNavigation = { shellViewModel.onEvent(ShellUiEvent.ToggleLeftDrawer) },
      modifier = Modifier.align(Alignment.TopStart),
    )
  }
}

@Composable
private fun ShellModeContent(
  modeId: ModeId,
  modifier: Modifier = Modifier,
  onUpdateChatState: (com.vjaykrsna.nanoai.feature.uiux.presentation.ChatState?) -> Unit,
  onNavigate: (ModeId) -> Unit,
) {
  var hasError by rememberSaveable { mutableStateOf(false) }
  var errorMessage by rememberSaveable { mutableStateOf("") }

  if (hasError) {
    ErrorBoundary(
      modifier = modifier,
      title = "Navigation Error",
      description = errorMessage,
      onRetry = {
        hasError = false
        errorMessage = ""
      },
    )
    return
  }

  // TODO: Implement proper error boundary triggering for navigation failures
  // Current error boundary exists but is never triggered. Consider:
  // - Adding error states to ViewModels for navigation failures
  // - Using LaunchedEffect to catch async errors
  // - Adding validation for required screen dependencies

  when (modeId) {
    // HOME is handled directly by NanoShellScaffold - never called here
    ModeId.HOME -> error("HOME mode should be handled by NanoShellScaffold, not NavigationScaffold")
    ModeId.CHAT ->
      ChatScreen(
        modifier = modifier,
        onUpdateChatState = onUpdateChatState,
        onNavigate = onNavigate,
      )
    ModeId.LIBRARY -> ModelLibraryScreen(modifier = modifier)
    ModeId.SETTINGS -> SettingsScreen(modifier = modifier)
    ModeId.IMAGE -> ImageFeatureContainer(modifier = modifier)
    ModeId.AUDIO -> AudioScreen(modifier = modifier)
    ModeId.CODE -> ModePlaceholder("Code workspace", modifier)
    ModeId.TRANSLATE -> ModePlaceholder("Translation", modifier)
    ModeId.HISTORY -> ModePlaceholder("History", modifier)
    ModeId.TOOLS -> ModePlaceholder("Tools", modifier)
  }
}

@Composable
private fun ErrorBoundary(
  modifier: Modifier = Modifier,
  title: String = "Error",
  description: String = "An unexpected error occurred",
  onRetry: () -> Unit = {},
) {
  Box(
    modifier =
      modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).semantics {
        contentDescription = "Error boundary: $title"
      },
    contentAlignment = Alignment.Center,
  ) {
    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        imageVector = Icons.Default.Warning,
        contentDescription = null,
        modifier = Modifier.padding(bottom = 16.dp),
        tint = MaterialTheme.colorScheme.error,
      )

      Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 8.dp),
      )

      Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 24.dp),
      )

      TextButton(onClick = onRetry) { Text("Try Again") }
    }
  }
}

@Composable
private fun ModePlaceholder(title: String, modifier: Modifier = Modifier) {
  Box(
    modifier =
      modifier.fillMaxSize().semantics { contentDescription = "$title surface placeholder" },
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = "$title is coming soon",
      style = MaterialTheme.typography.bodyLarge,
      fontWeight = FontWeight.Medium,
      textAlign = TextAlign.Center,
    )
  }
}

/**
 * Skip links navigation component for keyboard accessibility. Provides keyboard shortcuts to skip
 * directly to main content or navigation. These links are visually hidden but accessible to screen
 * readers and keyboard users.
 */
@Composable
@Suppress("UnusedParameter")
private fun SkipLinksNavigation(
  onSkipToContent: () -> Unit,
  onSkipToNavigation: () -> Unit,
  modifier: Modifier = Modifier,
) {
  // NOTE: Skip links are typically visually hidden but keyboard-accessible.
  // They provide quick navigation for keyboard users and screen reader users.
  // Implementation uses standard accessibility patterns:
  // - Alt+1: Skip to main content
  // - Alt+2: Skip to navigation
  Box(
    modifier =
      modifier.fillMaxSize().semantics {
        contentDescription =
          "Skip links: Press Alt+1 to jump to main content, Alt+2 to jump to navigation"
      }
  ) {
    // Skip links are implemented as keyboard shortcuts in ShellViewModel
    // and handled via accessibility services. This composable serves as
    // documentation and accessibility marker.
  }
}

// WelcomeOverlay and WelcomeScreen removed from the app. See specs/003-UI-UX for rationale.
