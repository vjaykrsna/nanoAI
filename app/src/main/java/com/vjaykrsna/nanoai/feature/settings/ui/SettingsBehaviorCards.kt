package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Behavior Settings Cards
@Composable
internal fun BehaviorStartupCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Startup & Home",
    infoText =
      "Configure which screen nanoAI opens to when launched. Choose between starting fresh or resuming your last session with all conversations, modes, and documents restored to their previous state.\n\nUpcoming implementation will hook into Shell launch policies.",
    modifier = modifier,
  )
}

@Composable
internal fun BehaviorInputPreferencesCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Input Preferences",
    infoText =
      "Customize how you interact with nanoAI. Set keyboard shortcuts for common actions, configure when messages are sent (Enter vs Cmd/Ctrl+Enter), and set up voice activation phrases for hands-free operation.\n\nTracked for Phase 2 once mode-specific composers land.",
    modifier = modifier,
  )
}

@Composable
internal fun BehaviorAccessibilityCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Accessibility",
    infoText =
      "Enhance nanoAI's usability for users with disabilities. Configure screen reader support for better interaction with ChatGPT and other assistive technologies. Options include high contrast themes, larger text, and customized interaction patterns to meet WCAG 2.1 AA compliance standards.\n\nTalkBack optimizations and WCAG 2.1 AA compliance features.",
    modifier = modifier,
  )
}

@Composable
internal fun BehaviorNotificationsCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Notifications",
    infoText =
      "Configure when and how nanoAI notifies you. Get alerts for completed inferences, downloads, or background tasks. Customize notification channels and importance levels to stay informed without being overwhelmed.\n\nWorkManager notification channels for progress updates.",
    modifier = modifier,
  )
}
