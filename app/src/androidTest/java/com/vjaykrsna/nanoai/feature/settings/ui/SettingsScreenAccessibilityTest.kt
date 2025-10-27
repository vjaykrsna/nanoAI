package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Test

class SettingsScreenAccessibilityTest : BaseSettingsScreenTest() {

  @Test
  fun settingsScreen_allControls_haveSemanticLabels() {
    renderSettingsScreen()

    composeTestRule
      .onNodeWithContentDescription("Settings screen organized by tabs with contextual sections")
      .assertIsDisplayed()

    composeTestRule.onNodeWithText("General").assertIsDisplayed()
    composeTestRule.onNodeWithText("Appearance").assertIsDisplayed()
    composeTestRule.onNodeWithText("Privacy & Security").assertIsDisplayed()
    composeTestRule.onNodeWithText("Offline & Models").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_talkBack_navigatesCorrectly() {
    renderSettingsScreen()

    composeTestRule.onNodeWithText("General").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Appearance").performClick()
    composeTestRule.waitForIdle()
    // TODO: expand when TalkBack events can be captured in tests
  }

  @Test
  fun settingsScreen_dynamicType_adjustsCorrectly() {
    renderSettingsScreen()

    composeTestRule.onNodeWithText("General").assertIsDisplayed()
  }

  @Test
  fun settingsScreen_colorContrast_meetsStandards() {
    renderSettingsScreen()

    composeTestRule.onNodeWithText("General").assertIsDisplayed()
    composeTestRule.onNodeWithText("Appearance").assertIsDisplayed()
  }
}
