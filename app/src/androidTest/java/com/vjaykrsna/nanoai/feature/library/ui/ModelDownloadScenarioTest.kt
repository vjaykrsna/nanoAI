package com.vjaykrsna.nanoai.feature.library.ui

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.filters.LargeTest
import com.vjaykrsna.nanoai.MainActivity
import com.vjaykrsna.nanoai.testing.TestEnvironmentRule
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

/**
 * Quickstart Scenario 3 instrumentation: Model download integrity happy + corrupt flows.
 *
 * Assertions (red until integrity + telemetry are implemented):
 * - Integrity status indicator appears when manifest verification succeeds
 *   (`model_integrity_success_banner`).
 * - Corrupt package triggers actionable error with retry CTA (`model_integrity_error_message`).
 * - Retry CTA enqueues WorkManager job again (`model_integrity_retry_button`).
 */
@LargeTest
@Ignore(
  "Model integrity workflow not implemented yet; see specs/005-improve-test-coverage/tasks.md"
)
class ModelDownloadScenarioTest {

  @JvmField @Rule val environmentRule = TestEnvironmentRule()
  @JvmField @Rule val composeRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun corruptDownload_surfacesActionableError_and_allowsRetry() {
    composeRule
      .onNodeWithTag("model_list_open_downloads")
      .assertIsDisplayed()
      .assertHasClickAction()
      .performClick()

    composeRule
      .onNodeWithTag("model_integrity_success_banner")
      .assertIsDisplayed()
      .assertTextContains("Signature verified", substring = true)

    composeRule
      .onNodeWithTag("model_integrity_force_corrupt")
      .assertIsDisplayed()
      .assertHasClickAction()
      .performClick()

    composeRule
      .onNodeWithTag("model_integrity_error_message")
      .assertIsDisplayed()
      .assertTextContains("integrity", substring = true, ignoreCase = true)
      .assertContentDescriptionEquals("Model download integrity failure")

    composeRule
      .onNodeWithTag("model_integrity_retry_button")
      .assertIsDisplayed()
      .assertHasClickAction()
      .performClick()
    composeRule
      .onNodeWithTag("model_integrity_retry_confirmation")
      .assertIsDisplayed()
      .assertTextContains("retry queued", substring = true, ignoreCase = true)
  }
}
