@file:Suppress("MatchingDeclarationName")

package com.vjaykrsna.nanoai.feature.uiux.ui.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.feature.uiux.presentation.ModeId
import com.vjaykrsna.nanoai.feature.uiux.presentation.ShellUiState
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoElevation
import com.vjaykrsna.nanoai.feature.uiux.ui.sidebar.RightSidebarPanels

/** Events emitted by shell right rail components. */
sealed interface ShellRightRailEvent {
  // Add specific events if needed
}

@Composable
internal fun ShellRightRailHost(
  state: ShellUiState,
  onEvent: (ShellUiEvent) -> Unit,
  modeContent: @Composable (ModeId) -> Unit,
  snackbarHostState: androidx.compose.material3.SnackbarHostState,
  originalOnEvent: (ShellUiEvent) -> Unit = onEvent,
) {
  val layout = state.layout
  if (layout.supportsRightRail) {
    Row(modifier = Modifier.fillMaxSize()) {
      ShellMainSurface(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = onEvent,
        modeContent = modeContent,
        modifier = Modifier.weight(1f),
        originalOnEvent = originalOnEvent,
      )

      Surface(
        tonalElevation = NanoElevation.level2,
        modifier = Modifier.fillMaxHeight().width(320.dp).testTag("right_sidebar_permanent"),
      ) {
        RightSidebarPanels(state = state, onEvent = onEvent, modifier = Modifier.fillMaxSize())
      }
    }
  } else {
    val parentLayoutDirection = LocalLayoutDirection.current
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
        CompositionLocalProvider(LocalLayoutDirection provides parentLayoutDirection) {
          ShellMainSurface(
            state = state,
            snackbarHostState = snackbarHostState,
            onEvent = onEvent,
            modeContent = modeContent,
            modifier = Modifier.fillMaxSize().align(Alignment.TopStart),
            originalOnEvent = originalOnEvent,
          )
        }

        AnimatedVisibility(
          visible = layout.isRightDrawerOpen,
          enter =
            slideInHorizontally(
              animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
            ) { drawerWidth ->
              drawerWidth
            },
          exit =
            slideOutHorizontally(
              animationSpec = tween(durationMillis = 160, easing = LinearOutSlowInEasing)
            ) { drawerWidth ->
              drawerWidth
            },
        ) {
          Surface(
            tonalElevation = NanoElevation.level3,
            modifier =
              Modifier.align(Alignment.TopEnd)
                .fillMaxHeight()
                .width(320.dp)
                .testTag("right_sidebar_modal"),
          ) {
            RightSidebarPanels(state = state, onEvent = onEvent, modifier = Modifier.fillMaxSize())
          }
        }
      }
    }
  }
}
