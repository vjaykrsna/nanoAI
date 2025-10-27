package com.vjaykrsna.nanoai.feature.uiux.scenario

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
 * Quickstart Scenario 5 instrumentation: Offline banner flow.
 *
 * Expectations (not yet met, so assertions fail):
 * - A developer hook or debug affordance exists to toggle offline mode (tag `debug_toggle_offline`)
 * - Offline banner surfaces with messaging (`offline_banner_message`)
 * - Disabled CTA summary (`offline_disabled_actions_summary`) is rendered
 * - Retry button (`offline_banner_retry`) queues actions for later
 */
@LargeTest
@Ignore("Offline scenario blocked on debug toggles; see specs/003-UI-UX/plan.md")
class OfflineModeScenarioTest {
  @JvmField @Rule val environmentRule = TestEnvironmentRule()

  @JvmField @Rule val composeRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun offlineMode_displaysBanner_disablesCtas_andQueuesRetry() {
    // Use debug hook to simulate offline state until real connectivity API is wired
    composeRule
      .onNodeWithTag("debug_toggle_offline")
      .assertIsDisplayed()
      .assertHasClickAction()
      .performClick()

    composeRule
      .onNodeWithTag("offline_banner_message")
      .assertIsDisplayed()
      .assertTextContains("offline", substring = true, ignoreCase = true)

    composeRule
      .onNodeWithTag("offline_banner_disabled_actions")
      .assertIsDisplayed()
      .assertContentDescriptionEquals("Offline actions disabled until reconnect")

    composeRule
      .onNodeWithTag("offline_banner_retry")
      .assertIsDisplayed()
      .assertHasClickAction()
      .performClick()

    composeRule
      .onNodeWithTag("offline_banner_queue_status")
      .assertIsDisplayed()
      .assertTextContains("queued", substring = true, ignoreCase = true)
  }
}
