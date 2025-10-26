package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// About Settings Cards
@Composable
internal fun AboutNanoAICard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "About nanoAI",
    infoText =
      "Learn about nanoAI, including version information, licensing details, and acknowledgments for third-party components and contributors.\n\nVersion metadata will read from BuildConfig once wired.",
    modifier = modifier,
  )
}

@Composable
internal fun AboutSupportFeedbackCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Support & Feedback",
    infoText =
      "Connect with our support team, submit bug reports, request features, or provide general feedback to help improve nanoAI.\n\nLinks will target docs/ and community channels when published.",
    modifier = modifier,
  )
}

@Composable
internal fun AboutDocumentationCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Documentation",
    infoText =
      "Explore detailed user guides, API references, tutorials, and best practices to get the most out of nanoAI.\n\nComprehensive documentation links for all features and APIs.",
    modifier = modifier,
  )
}

@Composable
internal fun AboutSystemInformationCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "System Information",
    infoText =
      "View detailed information about your device, operating system, and nanoAI's runtime environment to help with troubleshooting and support requests.\n\nDebug information for troubleshooting and support tickets.",
    modifier = modifier,
  )
}

@Composable
internal fun AboutAdvancedDiagnosticsCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Advanced Diagnostics",
    infoText =
      "Generate comprehensive diagnostic reports including application logs, performance metrics, and system traces to help our engineers identify and resolve issues.\n\nDiagnostics flow will lean on the telemetry pipeline in specs/002.",
    modifier = modifier,
  )
}

@Composable
internal fun AboutCacheManagementCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Cache Management",
    infoText =
      "Manage cached model data, temporary files, and other storage that can affect performance when it becomes excessive.\n\nStorage orchestration hooks will bind to the OfflineStore module.",
    modifier = modifier,
  )
}

@Composable
internal fun AboutExperimentalFeaturesCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Experimental Features",
    infoText =
      "Access cutting-edge features and experimental integrations that are still being tested and refined.\n\nFeature flag descriptors live in specs/004-fixes-and-inconsistencies/plan.md.",
    modifier = modifier,
  )
}
