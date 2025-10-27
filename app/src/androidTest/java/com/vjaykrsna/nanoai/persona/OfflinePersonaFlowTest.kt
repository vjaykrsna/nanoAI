package com.vjaykrsna.nanoai.persona

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.filters.LargeTest
import com.vjaykrsna.nanoai.MainActivity
import org.junit.Ignore
import org.junit.Test

/**
 * Quickstart Scenario 4 instrumentation: Offline persona queue + replay flow.
 *
 * Expectations captured here (currently unmet, so assertions fail):
 * - Persona picker exposes a debug toggle to force offline mode (`persona_debug_toggle_offline`).
 * - Offline banner articulates persona-specific queue message (`offline_persona_banner_message`).
 * - Retry CTA persists queued persona actions and replays them when connectivity returns
 *   (`offline_persona_retry`).
 */
@LargeTest
@Ignore("Persona offline queue flow pending feature work; see specs/003-UI-UX/plan.md")
class OfflinePersonaFlowTest {
  @org.junit.jupiter.api.extension.RegisterExtension
  @JvmField
  val composeRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun offlinePersonaQueue_replaysActions_afterNetworkRestored() {
    composeRule
      .onNodeWithTag("persona_debug_toggle_offline")
      .assertIsDisplayed()
      .assertHasClickAction()
      .performClick()

    composeRule
      .onNodeWithTag("offline_persona_banner_message")
      .assertIsDisplayed()
      .assertTextContains("persona", substring = true, ignoreCase = true)

    composeRule
      .onNodeWithTag("offline_persona_queue_count")
      .assertIsDisplayed()
      .assertTextContains("0", substring = false)

    composeRule
      .onNodeWithTag("offline_persona_retry")
      .assertIsDisplayed()
      .assertHasClickAction()
      .performClick()

    composeRule
      .onNodeWithTag("offline_persona_queue_status")
      .assertIsDisplayed()
      .assertTextContains("replayed", substring = true, ignoreCase = true)

    composeRule
      .onNodeWithTag("persona_debug_toggle_online")
      .assertIsDisplayed()
      .assertHasClickAction()
      .performClick()

    composeRule
      .onNodeWithTag("offline_persona_queue_count")
      .assertIsDisplayed()
      .assertTextContains("0", substring = false)
  }
}
