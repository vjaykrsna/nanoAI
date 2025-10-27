package com.vjaykrsna.nanoai.feature.uiux.contracts

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
 * Contract test for Theme toggle (FR-011). Checks instant theme switch semantics and persistence
 * signal.
 */
@LargeTest
@Ignore("Theme toggle contract pending retained preferences storage; see specs/003-UI-UX/plan.md")
class ThemeToggleContractTest {

  @JvmField @Rule val environmentRule = TestEnvironmentRule()
  @JvmField @Rule val composeRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun themeToggle_switchPresent_andInteractive() {
    composeRule
      .onNodeWithTag("theme_toggle_switch")
      .assertIsDisplayed()
      .assertHasClickAction()
      .performClick()

    composeRule.waitForIdle()
    composeRule
      .onNodeWithTag("theme_toggle_persistence_status")
      .assertIsDisplayed()
      .assertTextContains("Current:", substring = true)
  }

  @Test
  fun themeToggle_persistsSelection_acrossRecomposition() {
    composeRule.onNodeWithTag("theme_toggle_switch").performClick()
    composeRule.waitForIdle()
    composeRule.mainClock.advanceTimeBy(500)
    composeRule.waitForIdle()

    composeRule
      .onNodeWithTag("theme_toggle_persistence_status")
      .assertIsDisplayed()
      .assertTextContains("Dark", substring = true)

    composeRule.activityRule.scenario.recreate()
    composeRule.waitForIdle()

    composeRule
      .onNodeWithTag("theme_toggle_persistence_status")
      .assertIsDisplayed()
      .assertTextContains("Dark", substring = true)
  }
}
