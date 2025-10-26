package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// About Settings Cards
@Composable
internal fun AboutNanoAICard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "About nanoAI",
    description = "View version details, licenses, and acknowledgements.",
    supportingText = "Version metadata will read from BuildConfig once wired.",
    modifier = modifier,
    infoDialogContent = {
      Text(
        text =
          "Learn about nanoAI, including version information, licensing details, and acknowledgments for third-party components and contributors.",
        style = MaterialTheme.typography.bodyLarge,
      )
    },
  )
}

@Composable
internal fun AboutSupportFeedbackCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Support & Feedback",
    description = "Send feedback, report issues, and browse documentation.",
    supportingText = "Links will target docs/ and community channels when published.",
    modifier = modifier,
    infoDialogContent = {
      Text(
        text =
          "Connect with our support team, submit bug reports, request features, or provide general feedback to help improve nanoAI.",
        style = MaterialTheme.typography.bodyLarge,
      )
    },
  )
}

@Composable
internal fun AboutDocumentationCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Documentation",
    description = "Access user guides, API documentation, and tutorials.",
    supportingText = "Comprehensive documentation links for all features and APIs.",
    modifier = modifier,
    infoDialogContent = {
      Text(
        text =
          "Explore detailed user guides, API references, tutorials, and best practices to get the most out of nanoAI.",
        style = MaterialTheme.typography.bodyLarge,
      )
    },
  )
}

@Composable
internal fun AboutSystemInformationCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "System Information",
    description = "View device specifications and runtime diagnostics.",
    supportingText = "Debug information for troubleshooting and support tickets.",
    modifier = modifier,
    infoDialogContent = {
      Text(
        text =
          "View detailed information about your device, operating system, and nanoAI's runtime environment to help with troubleshooting and support requests.",
        style = MaterialTheme.typography.bodyLarge,
      )
    },
  )
}

@Composable
internal fun AboutAdvancedDiagnosticsCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Advanced Diagnostics",
    description = "Capture detailed logs, attach traces, and share with support.",
    supportingText = "Diagnostics flow will lean on the telemetry pipeline in specs/002.",
    modifier = modifier,
    infoDialogContent = {
      Text(
        text =
          "Generate comprehensive diagnostic reports including application logs, performance metrics, and system traces to help our engineers identify and resolve issues.",
        style = MaterialTheme.typography.bodyLarge,
      )
    },
  )
}

@Composable
internal fun AboutCacheManagementCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Cache Management",
    description = "Clear inference caches, downloaded assets, and temporary data.",
    supportingText = "Storage orchestration hooks will bind to the OfflineStore module.",
    modifier = modifier,
    infoDialogContent = {
      Text(
        text =
          "Manage cached model data, temporary files, and other storage that can affect performance when it becomes excessive.",
        style = MaterialTheme.typography.bodyLarge,
      )
    },
  )
}

@Composable
internal fun AboutExperimentalFeaturesCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Experimental Features",
    description = "Opt in to beta capabilities and Labs integrations.",
    supportingText =
      "Feature flag descriptors live in specs/004-fixes-and-inconsistencies/plan.md.",
    modifier = modifier,
    infoDialogContent = {
      Text(
        text =
          "Access cutting-edge features and experimental integrations that are still being tested and refined.",
        style = MaterialTheme.typography.bodyLarge,
      )
    },
  )
}
