package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Behavior Settings Cards
@Composable
internal fun BehaviorStartupCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Startup & Home",
    description = "Define the screen nanoAI opens to and whether to restore previous sessions.",
    supportingText = "Upcoming implementation will hook into Shell launch policies.",
    modifier = modifier,
    infoDialogContent = {
      Text(
        text =
          "Configure which screen nanoAI opens to when launched. Choose between starting fresh or resuming your last session with all conversations, modes, and documents restored to their previous state.",
        style = MaterialTheme.typography.bodyLarge,
      )
    },
  )
}

@Composable
internal fun BehaviorInputPreferencesCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Input Preferences",
    description = "Configure keyboard shortcuts, compose send behavior, and voice activation cues.",
    supportingText = "Tracked for Phase 2 once mode-specific composers land.",
    modifier = modifier,
    infoDialogContent = {
      Text(
        text =
          "Customize how you interact with nanoAI. Set keyboard shortcuts for common actions, configure when messages are sent (Enter vs Cmd/Ctrl+Enter), and set up voice activation phrases for hands-free operation.",
        style = MaterialTheme.typography.bodyLarge,
      )
    },
  )
}

@Composable
internal fun BehaviorAccessibilityCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Accessibility",
    description = "Configure screen reader, high contrast, and assistive technologies.",
    supportingText = "TalkBack optimizations and WCAG 2.1 AA compliance features.",
    modifier = modifier,
    infoDialogContent = {
      Text(
        text =
          "Enhance nanoAI's usability for users with disabilities. Configure screen reader support for better interaction with ChatGPT and other assistive technologies. Options include high contrast themes, larger text, and customized interaction patterns to meet WCAG 2.1 AA compliance standards.",
        style = MaterialTheme.typography.bodyLarge,
      )
    },
  )
}

@Composable
internal fun BehaviorNotificationsCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Notifications",
    description = "Control alerts for downloads, job completion, and background tasks.",
    supportingText = "WorkManager notification channels for progress updates.",
    modifier = modifier,
    infoDialogContent = {
      Text(
        text =
          "Configure when and how nanoAI notifies you. Get alerts for completed inferences, downloads, or background tasks. Customize notification channels and importance levels to stay informed without being overwhelmed.",
        style = MaterialTheme.typography.bodyLarge,
      )
    },
  )
}
