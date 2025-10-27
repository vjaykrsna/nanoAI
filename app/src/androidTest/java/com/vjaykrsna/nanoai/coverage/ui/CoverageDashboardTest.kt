package com.vjaykrsna.nanoai.coverage.ui

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.vjaykrsna.nanoai.coverage.model.CoverageMetric
import com.vjaykrsna.nanoai.coverage.model.TestLayer
import com.vjaykrsna.nanoai.coverage.ui.CoverageDashboardBanner.OFFLINE_ANNOUNCEMENT
import com.vjaykrsna.nanoai.coverage.ui.CoverageDashboardBanner.offline
import com.vjaykrsna.nanoai.shared.ui.theme.NanoAITheme
import java.io.IOException
import org.junit.Test

class CoverageDashboardTest {

  val composeRule = createComposeRule()

  @Test
  fun coverageLayersDisplayPercentagesAndTargets() {
    val state =
      CoverageDashboardUiState(
        buildId = "build-2025-10-10",
        generatedAtIso = "2025-10-10T12:30:00Z",
        isRefreshing = false,
        layers =
          listOf(
            LayerCoverageState(
              layer = TestLayer.VIEW_MODEL,
              metric = CoverageMetric(coverage = 81.0, threshold = 75.0),
            ),
            LayerCoverageState(
              layer = TestLayer.UI,
              metric = CoverageMetric(coverage = 66.0, threshold = 65.0),
            ),
          ),
        risks = emptyList(),
        trendDelta = mapOf(TestLayer.VIEW_MODEL to 2.4, TestLayer.UI to -0.5),
        errorBanner = null,
      )

    composeRule.setContent {
      NanoAITheme(dynamicColor = false) {
        CoverageDashboardScreen(
          state = state,
          onRefresh = {},
          onRiskSelect = {},
          onShareRequest = {},
        )
      }
    }

    composeRule.waitForIdle()

    composeRule.onNodeWithText("View Model").assertExists()
    composeRule
      .onNodeWithTag("coverage-layer-ViewModel", useUnmergedTree = true)
      .assertExists()
      .assertTextContains("81", substring = true)
      .assertTextContains("Target 75", substring = true)
    composeRule
      .onNodeWithTag("coverage-layer-UI", useUnmergedTree = true)
      .assertExists()
      .assertTextContains("66", substring = true)
      .assertTextContains("Target 65", substring = true)
  }

  @Test
  fun riskChipAnnouncesSeverityAndStatus() {
    val state =
      CoverageDashboardUiState(
        buildId = "build-2025-10-10",
        generatedAtIso = "2025-10-10T12:30:00Z",
        isRefreshing = false,
        layers =
          listOf(
            LayerCoverageState(
              layer = TestLayer.DATA,
              metric = CoverageMetric(coverage = 71.0, threshold = 70.0),
            )
          ),
        risks =
          listOf(
            RiskChipState(
              riskId = "risk-critical-data",
              title = "Offline writes failing",
              severity = "CRITICAL",
              status = "OPEN",
            )
          ),
        trendDelta = emptyMap(),
        errorBanner = null,
      )

    composeRule.setContent {
      NanoAITheme(dynamicColor = false) {
        CoverageDashboardScreen(
          state = state,
          onRefresh = {},
          onRiskSelect = {},
          onShareRequest = {},
        )
      }
    }

    composeRule.waitForIdle()

    composeRule
      .onNodeWithContentDescription("Risk risk-critical-data, severity CRITICAL, status OPEN")
      .assertExists()
      .assertContentDescriptionEquals("Risk risk-critical-data, severity CRITICAL, status OPEN")
  }

  @Test
  fun offlineFallback_showsErrorBannerWithAccessibleAnnouncement() {
    val failure = IOException("Mock coverage service HTTP 503")

    val offlineBanner = offline(failure)

    val state =
      CoverageDashboardUiState(
        buildId = "build-2025-10-10",
        generatedAtIso = "2025-10-10T12:30:00Z",
        isRefreshing = false,
        layers =
          listOf(
            LayerCoverageState(
              layer = TestLayer.VIEW_MODEL,
              metric = CoverageMetric(coverage = 81.0, threshold = 75.0),
            ),
            LayerCoverageState(
              layer = TestLayer.UI,
              metric = CoverageMetric(coverage = 66.0, threshold = 65.0),
            ),
          ),
        risks = emptyList(),
        trendDelta = emptyMap(),
        errorBanner = offlineBanner,
      )

    composeRule.setContent {
      NanoAITheme(dynamicColor = false) {
        CoverageDashboardScreen(
          state = state,
          onRefresh = {},
          onRiskSelect = {},
          onShareRequest = {},
        )
      }
    }

    composeRule.waitForIdle()

    composeRule
      .onNodeWithTag("coverage-dashboard-error-banner")
      .assertExists()
      .assertContentDescriptionEquals(OFFLINE_ANNOUNCEMENT)

    composeRule.onNodeWithText("Device farm offline", substring = true).assertExists()

    composeRule
      .onNodeWithContentDescription(OFFLINE_ANNOUNCEMENT)
      .assertExists()
      .assertContentDescriptionEquals(OFFLINE_ANNOUNCEMENT)
  }
}
