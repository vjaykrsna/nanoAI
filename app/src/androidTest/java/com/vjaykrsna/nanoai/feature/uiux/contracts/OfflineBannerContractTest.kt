package com.vjaykrsna.nanoai.feature.uiux.contracts

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.filters.LargeTest
import com.vjaykrsna.nanoai.MainActivity
import com.vjaykrsna.nanoai.testing.TestEnvironmentRule
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

/**
 * Contract test for the Offline banner (FR-006). Ensures offline messaging, disabled-action
 * affordance, and retry semantics are exposed.
 *
 * This test intentionally fails until the UI layer adds the required semantics.
 */
@LargeTest
@Ignore("Offline banner interactions pending telemetry integration; see specs/003-UI-UX/plan.md")
class OfflineBannerContractTest {

  @JvmField @Rule val environmentRule = TestEnvironmentRule()
  @JvmField @Rule val composeRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun offlineBanner_displaysMessaging_andDisabledAffordance() {
    composeRule.onNodeWithTag("offline_banner_container").assertIsDisplayed()

    composeRule.onNodeWithContentDescription("Offline status banner").assertIsDisplayed()

    composeRule
      .onNodeWithTag("offline_banner_message")
      .assertIsDisplayed()
      .assertTextContains("You're offline")

    composeRule
      .onNodeWithTag("offline_banner_disabled_actions")
      .assertIsDisplayed()
      .assertContentDescriptionEquals("Offline actions disabled until reconnect")
  }

  @Test
  fun offlineBanner_retryAction_isAccessible() {
    composeRule
      .onNodeWithTag("offline_banner_retry")
      .assertIsDisplayed()
      .assertHasClickAction()
      .assertContentDescriptionEquals("Retry queued actions now")

    composeRule
      .onNodeWithTag("offline_banner_queue_status")
      .assertIsDisplayed()
      .assertTextContains("Queued:")
  }
}
