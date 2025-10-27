package com.vjaykrsna.nanoai.disclaimer

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.filters.LargeTest
import com.vjaykrsna.nanoai.MainActivity
import com.vjaykrsna.nanoai.testing.TestEnvironmentRule
import org.junit.Rule
import org.junit.Test

/**
 * Quickstart Scenario 5 instrumentation: First-launch disclaimer accessibility + blocking
 * behaviour.
 *
 * Assertions (expected to fail until dialog wired end-to-end):
 * - Dialog rendered on first launch with TalkBack semantics (`disclaimer_dialog_container`).
 * - Primary CTA blocked until user scrolls and acknowledges (`disclaimer_accept_button`).
 * - Secondary CTA dismisses with clear accessibility label (`disclaimer_decline_button`).
 */
@LargeTest
class DisclaimerDialogTest {

  @JvmField @Rule val environmentRule = TestEnvironmentRule()
  @JvmField @Rule val composeRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun disclaimerDialog_requiresAcknowledgement_and_readsWithTalkBack() {
    composeRule
      .onNodeWithTag("disclaimer_dialog_container")
      .assertIsDisplayed()
      .assertContentDescriptionEquals("nanoAI privacy disclaimer dialog")

    composeRule
      .onNodeWithTag("disclaimer_accept_button")
      .assertIsDisplayed()
      .assertIsNotEnabled()
      .assertContentDescriptionEquals("Accept privacy terms")

    composeRule
      .onNodeWithTag("disclaimer_decline_button")
      .assertIsDisplayed()
      .assertHasClickAction()
      .assertContentDescriptionEquals("Decline and review later")
      .assertTextContains("Decline", substring = true)

    composeRule.onNodeWithTag("disclaimer_last_text").performScrollTo()

    composeRule
      .onNodeWithTag("disclaimer_accept_button")
      .assertIsDisplayed()
      .assertIsEnabled()
      .assertHasClickAction()
      .assertTextContains("Agree", substring = true)
      .performClick()
  }
}
